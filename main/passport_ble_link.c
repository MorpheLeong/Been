#include "passport_ble_link.h"

#include "esp_log.h"
#include "nvs_flash.h"
#include "passport_trip_store.h"
#include "passport_display_policy.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "host/ble_gap.h"
#include "host/ble_gatt.h"
#include "host/ble_hs.h"
#include "host/ble_uuid.h"
#include "host/util/util.h"
#include "nimble/nimble_port.h"
#include "esp_heap_caps.h"
#include "os/os_mbuf.h"
#include "services/gap/ble_svc_gap.h"
#include "services/gatt/ble_svc_gatt.h"

#include <string.h>

#define DEVICE_NAME "AI Passport"
static const char *TAG = "passport_ble";
static const ble_uuid128_t SERVICE_UUID = BLE_UUID128_INIT(
    0x41, 0x49, 0x50, 0x01, 0x8c, 0x6e, 0x4a, 0x21,
    0xa9, 0x10, 0x41, 0x50, 0x00, 0x00, 0x00, 0x01);
static const ble_uuid128_t REQUEST_UUID = BLE_UUID128_INIT(
    0x41, 0x49, 0x50, 0x02, 0x8c, 0x6e, 0x4a, 0x21,
    0xa9, 0x10, 0x41, 0x50, 0x00, 0x00, 0x00, 0x01);
static const ble_uuid128_t RESULT_UUID = BLE_UUID128_INIT(
    0x41, 0x49, 0x50, 0x03, 0x8c, 0x6e, 0x4a, 0x21,
    0xa9, 0x10, 0x41, 0x50, 0x00, 0x00, 0x00, 0x01);
static const ble_uuid128_t SNAPSHOT_UUID = BLE_UUID128_INIT(
    0x41, 0x49, 0x50, 0x04, 0x8c, 0x6e, 0x4a, 0x21,
    0xa9, 0x10, 0x41, 0x50, 0x00, 0x00, 0x00, 0x01);

static uint16_t s_request_value_handle;
static passport_trip_model_t *s_trip_model;
static passport_trip_model_t s_trip_backup;
static passport_trip_rx_t s_trip_rx;
static volatile uint16_t s_connection = BLE_HS_CONN_HANDLE_NONE;
static volatile bool s_request_subscribed;
static uint8_t s_address_type;
static bool s_started;
static bool s_start_requested;
static passport_ble_event_cb_t s_callback;
static void *s_callback_context;

static void notify_app(passport_ble_event_t event,
                       const passport_ble_result_t *result)
{
    if (s_callback != NULL) s_callback(event, result, s_callback_context);
}

static int result_access(uint16_t connection, uint16_t attribute,
                         struct ble_gatt_access_ctxt *context, void *arg)
{
    (void)connection;
    (void)attribute;
    (void)arg;
    if (context->op != BLE_GATT_ACCESS_OP_WRITE_CHR) return BLE_ATT_ERR_UNLIKELY;

    uint8_t frame[PASSPORT_LOCATION_SUCCESS_SIZE];
    uint16_t length = 0;
    if (OS_MBUF_PKTLEN(context->om) > sizeof(frame) ||
        ble_hs_mbuf_to_flat(context->om, frame, sizeof(frame), &length) != 0) {
        return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN;
    }

    passport_ble_result_t result;
    if (!passport_location_protocol_decode_result(frame, length, &result))
        return BLE_ATT_ERR_VALUE_NOT_ALLOWED;
    notify_app(PASSPORT_BLE_RESULT, &result);
    return 0;
}

static int snapshot_access(uint16_t connection, uint16_t attribute,
                           struct ble_gatt_access_ctxt *context, void *arg)
{
    (void)connection; (void)attribute; (void)arg;
    if (context->op != BLE_GATT_ACCESS_OP_WRITE_CHR || s_trip_model == NULL)
        return BLE_ATT_ERR_UNLIKELY;
    uint8_t frame[20];
    uint16_t length = 0;
    if (OS_MBUF_PKTLEN(context->om) > sizeof(frame) ||
        ble_hs_mbuf_to_flat(context->om, frame, sizeof(frame), &length) != 0)
        return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN;
    if (length < 2) return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN;
    if (frame[1] == PASSPORT_SCREEN_SETTING_OPCODE) {
        uint16_t seconds;
        if (!passport_display_setting_decode(frame, length, &seconds))
            return BLE_ATT_ERR_VALUE_NOT_ALLOWED;
        if (passport_trip_store_set_screen_timeout(seconds) != ESP_OK)
            return BLE_ATT_ERR_UNLIKELY;
        notify_app(PASSPORT_BLE_SETTINGS, NULL);
        return 0;
    }
    if (frame[1] == PASSPORT_SNAPSHOT_COMMIT_OPCODE)
        s_trip_backup = *s_trip_model;
    if (!passport_trip_sync_frame(&s_trip_rx, s_trip_model, frame, length))
        return BLE_ATT_ERR_VALUE_NOT_ALLOWED;
    if (frame[1] == PASSPORT_SNAPSHOT_COMMIT_OPCODE) {
        if (passport_trip_store_save(s_trip_model) != ESP_OK) {
            *s_trip_model = s_trip_backup;
            return BLE_ATT_ERR_UNLIKELY;
        }
        notify_app(PASSPORT_BLE_SNAPSHOT, NULL);
    }
    return 0;
}

static int request_access(uint16_t connection, uint16_t attribute,
                          struct ble_gatt_access_ctxt *context, void *arg)
{
    (void)connection;
    (void)attribute;
    (void)context;
    (void)arg;
    return BLE_ATT_ERR_READ_NOT_PERMITTED;
}

static const struct ble_gatt_svc_def GATT_SERVICES[] = {
    {
        .type = BLE_GATT_SVC_TYPE_PRIMARY,
        .uuid = &SERVICE_UUID.u,
        .characteristics = (struct ble_gatt_chr_def[]) {
            {
                .uuid = &REQUEST_UUID.u,
                .access_cb = request_access,
                .flags = BLE_GATT_CHR_F_NOTIFY,
                .val_handle = &s_request_value_handle,
            },
            {
                .uuid = &RESULT_UUID.u,
                .access_cb = result_access,
                .flags = BLE_GATT_CHR_F_WRITE | BLE_GATT_CHR_F_WRITE_NO_RSP,
            },
            {
                .uuid = &SNAPSHOT_UUID.u,
                .access_cb = snapshot_access,
                .flags = BLE_GATT_CHR_F_WRITE | BLE_GATT_CHR_F_WRITE_NO_RSP,
            },
            { 0 },
        },
    },
    { 0 },
};

static int advertise(void);

static int gap_event(struct ble_gap_event *event, void *arg)
{
    (void)arg;
    switch (event->type) {
    case BLE_GAP_EVENT_CONNECT:
        if (event->connect.status == 0) {
            s_connection = event->connect.conn_handle;
        } else if (s_start_requested) {
            const int rc = advertise();
            if (rc != 0) {
                ESP_LOGE(TAG, "BLE restart advertising failed: %d", rc);
                notify_app(PASSPORT_BLE_FAILED, NULL);
            }
        }
        break;
    case BLE_GAP_EVENT_DISCONNECT:
        s_connection = BLE_HS_CONN_HANDLE_NONE;
        s_request_subscribed = false;
        notify_app(PASSPORT_BLE_DISCONNECTED, NULL);
        if (s_start_requested) {
            const int rc = advertise();
            if (rc != 0) {
                ESP_LOGE(TAG, "BLE advertising after disconnect failed: %d", rc);
                notify_app(PASSPORT_BLE_FAILED, NULL);
            }
        }
        break;
    case BLE_GAP_EVENT_ADV_COMPLETE:
        if (s_start_requested && s_connection == BLE_HS_CONN_HANDLE_NONE) {
            const int rc = advertise();
            if (rc != 0) {
                ESP_LOGE(TAG, "BLE advertising restart failed: %d", rc);
                notify_app(PASSPORT_BLE_FAILED, NULL);
            }
        }
        break;
    case BLE_GAP_EVENT_SUBSCRIBE:
        if (event->subscribe.attr_handle == s_request_value_handle) {
            s_request_subscribed = event->subscribe.cur_notify;
            notify_app(s_request_subscribed ? PASSPORT_BLE_CONNECTED
                                            : PASSPORT_BLE_DISCONNECTED, NULL);
        }
        break;
    default:
        break;
    }
    return 0;
}

static int advertise(void)
{
    struct ble_hs_adv_fields fields = { 0 };
    fields.flags = BLE_HS_ADV_F_DISC_GEN | BLE_HS_ADV_F_BREDR_UNSUP;
    fields.uuids128 = (ble_uuid128_t *)&SERVICE_UUID;
    fields.num_uuids128 = 1;
    fields.uuids128_is_complete = 1;
    int rc = ble_gap_adv_set_fields(&fields);
    if (rc != 0) return rc;

    struct ble_hs_adv_fields response_fields = { 0 };
    response_fields.name = (const uint8_t *)DEVICE_NAME;
    response_fields.name_len = strlen(DEVICE_NAME);
    response_fields.name_is_complete = 1;
    rc = ble_gap_adv_rsp_set_fields(&response_fields);
    if (rc != 0) return rc;

    struct ble_gap_adv_params params = { 0 };
    params.conn_mode = BLE_GAP_CONN_MODE_UND;
    params.disc_mode = BLE_GAP_DISC_MODE_GEN;
    params.itvl_min = BLE_GAP_ADV_ITVL_MS(250);
    params.itvl_max = BLE_GAP_ADV_ITVL_MS(250);
    return ble_gap_adv_start(s_address_type, NULL, BLE_HS_FOREVER,
                             &params, gap_event, NULL);
}

static void on_reset(int reason)
{
    s_connection = BLE_HS_CONN_HANDLE_NONE;
    s_request_subscribed = false;
    ESP_LOGE(TAG, "NimBLE host reset: %d", reason);
    notify_app(PASSPORT_BLE_FAILED, NULL);
}

static void on_sync(void)
{
    int rc = ble_hs_util_ensure_addr(0);
    if (rc == 0) rc = ble_hs_id_infer_auto(0, &s_address_type);
    if (rc == 0 && s_start_requested) rc = advertise();
    if (rc != 0) {
        ESP_LOGE(TAG, "NimBLE synchronization failed: %d", rc);
        notify_app(PASSPORT_BLE_FAILED, NULL);
    }
}

static void host_task(void *arg)
{
    (void)arg;
    nimble_port_run();
    vTaskDelete(NULL);
}

esp_err_t passport_ble_link_start(passport_ble_event_cb_t callback, void *context)
{
    if (s_started) return ESP_ERR_INVALID_STATE;
    s_callback = callback;
    s_callback_context = context;
    s_start_requested = false;
    s_connection = BLE_HS_CONN_HANDLE_NONE;

    esp_err_t err = nvs_flash_init();
    if (err != ESP_OK) return err; // Preserve user NVS; never auto-erase.
    err = nimble_port_init();
    if (err != ESP_OK) {
        ESP_LOGE("passport_ble", "NimBLE init: %s; free internal=%u, largest=%u",
                 esp_err_to_name(err),
                 (unsigned)heap_caps_get_free_size(MALLOC_CAP_INTERNAL | MALLOC_CAP_8BIT),
                 (unsigned)heap_caps_get_largest_free_block(MALLOC_CAP_INTERNAL | MALLOC_CAP_8BIT));
        return err;
    }
    s_started = true;
    ble_svc_gap_init();
    ble_svc_gatt_init();
    int rc = ble_svc_gap_device_name_set(DEVICE_NAME);
    if (rc == 0) rc = ble_gatts_count_cfg(GATT_SERVICES);
    if (rc == 0) rc = ble_gatts_add_svcs(GATT_SERVICES);
    if (rc != 0) { err = ESP_FAIL; goto failed; }

    ble_hs_cfg.reset_cb = on_reset;
    ble_hs_cfg.sync_cb = on_sync;
    s_start_requested = true;
    if (xTaskCreatePinnedToCore(host_task, "passport_ble", NIMBLE_HS_STACK_SIZE,
                                NULL, configMAX_PRIORITIES - 4, NULL,
                                NIMBLE_CORE) != pdPASS) {
        err = ESP_ERR_NO_MEM;
        goto failed;
    }
    return ESP_OK;

failed:
    s_start_requested = false;
    (void)nimble_port_deinit();
    s_started = false;
    return err;
}

void passport_ble_link_bind_trip_model(passport_trip_model_t *model)
{
    s_trip_model = model;
    memset(&s_trip_rx, 0, sizeof(s_trip_rx));
}

esp_err_t passport_ble_link_request_location(uint16_t request_id)
{
    if (s_connection == BLE_HS_CONN_HANDLE_NONE || !s_request_subscribed)
        return ESP_ERR_INVALID_STATE;
    uint8_t frame[PASSPORT_LOCATION_REQUEST_SIZE];
    const size_t length = passport_location_protocol_encode_request(
        request_id, frame, sizeof(frame));
    if (length != sizeof(frame)) return ESP_ERR_INVALID_SIZE;
    struct os_mbuf *om = ble_hs_mbuf_from_flat(frame, length);
    if (om == NULL) return ESP_ERR_NO_MEM;
    const int rc = ble_gatts_notify_custom(s_connection, s_request_value_handle, om);
    return rc == 0 ? ESP_OK : ESP_FAIL;
}

static esp_err_t request_destination_state(uint16_t request_id,
                                          uint8_t opcode,
                                          const char *destination_id,
                                          const char *destination_name)
{
    if (destination_id == NULL || destination_name == NULL ||
        s_connection == BLE_HS_CONN_HANDLE_NONE || !s_request_subscribed)
        return ESP_ERR_INVALID_STATE;
    const size_t id_length = strnlen(destination_id, PASSPORT_TRIP_ID_BYTES);
    const size_t name_length = strnlen(destination_name, PASSPORT_TRIP_NAME_BYTES);
    if (id_length == 0 || id_length >= PASSPORT_TRIP_ID_BYTES ||
        name_length == 0 || name_length >= PASSPORT_TRIP_NAME_BYTES)
        return ESP_ERR_INVALID_ARG;

    uint8_t payload[2 + PASSPORT_TRIP_ID_BYTES + PASSPORT_TRIP_NAME_BYTES];
    size_t used = 0;
    payload[used++] = (uint8_t)id_length;
    memcpy(payload + used, destination_id, id_length); used += id_length;
    payload[used++] = (uint8_t)name_length;
    memcpy(payload + used, destination_name, name_length); used += name_length;
    const size_t chunk_size = 11;
    for (size_t offset = 0; offset < used; offset += chunk_size) {
        uint8_t frame[20] = {
            PASSPORT_LOCATION_PROTOCOL_VERSION,
            PASSPORT_CHECKIN_FRAGMENT_OPCODE,
            opcode,
            (uint8_t)request_id, (uint8_t)(request_id >> 8),
            (uint8_t)used, (uint8_t)(used >> 8),
            (uint8_t)offset, (uint8_t)(offset >> 8),
        };
        const size_t part = (used - offset) < chunk_size ? used - offset : chunk_size;
        memcpy(frame + 9, payload + offset, part);
        struct os_mbuf *om = ble_hs_mbuf_from_flat(frame, 9 + part);
        if (om == NULL) return ESP_ERR_NO_MEM;
        const int rc = ble_gatts_notify_custom(s_connection, s_request_value_handle, om);
        if (rc != 0) return ESP_FAIL;
        if (offset + part < used) vTaskDelay(pdMS_TO_TICKS(20));
    }
    return ESP_OK;
}

esp_err_t passport_ble_link_request_checkin(uint16_t request_id,
                                            const char *destination_id,
                                            const char *destination_name)
{
    return request_destination_state(request_id, PASSPORT_CHECKIN_REQUEST_OPCODE,
                                     destination_id, destination_name);
}

esp_err_t passport_ble_link_request_uncheckin(uint16_t request_id,
                                              const char *destination_id,
                                              const char *destination_name)
{
    return request_destination_state(request_id, PASSPORT_UNCHECKIN_REQUEST_OPCODE,
                                     destination_id, destination_name);
}

bool passport_ble_link_is_connected(void)
{
    return s_connection != BLE_HS_CONN_HANDLE_NONE;
}
