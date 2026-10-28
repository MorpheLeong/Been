#include "passport_ble_link.h"

#include "esp_log.h"
#include "esp_random.h"
#include "nvs_flash.h"
#include "passport_ble_ids.h"
#include "passport_avatar.h"
#include "passport_avatar_protocol.h"
#include "passport_trip_store.h"
#include "passport_track_name_protocol.h"
#include "passport_display_policy.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "host/ble_gap.h"
#include "host/ble_gatt.h"
#include "host/ble_hs.h"
#include "host/ble_store.h"
#include "host/ble_uuid.h"
#include "host/util/util.h"
#include "nimble/nimble_npl.h"
#include "nimble/nimble_port.h"
#include "esp_heap_caps.h"
#include "os/os_mbuf.h"
#include "services/gap/ble_svc_gap.h"
#include "services/gatt/ble_svc_gatt.h"

#include <stdlib.h>
#include <string.h>

void ble_store_config_init(void);

#define DEVICE_NAME "BugChecking"
#define PASSPORT_BLE_MAX_ATT_MTU 247U
#define PASSPORT_BLE_MAX_WRITE_BYTES (PASSPORT_BLE_MAX_ATT_MTU - 3U)
static const char *TAG = "passport_ble";
static const ble_uuid128_t SERVICE_UUID = BLE_UUID128_INIT(PASSPORT_BLE_SERVICE_UUID_BYTES);
static const ble_uuid128_t REQUEST_UUID = BLE_UUID128_INIT(PASSPORT_BLE_COMMAND_UUID_BYTES);
static const ble_uuid128_t RESULT_UUID = BLE_UUID128_INIT(PASSPORT_BLE_RESULT_UUID_BYTES);
static const ble_uuid128_t SNAPSHOT_UUID = BLE_UUID128_INIT(PASSPORT_BLE_SNAPSHOT_UUID_BYTES);

static uint16_t s_request_value_handle;
static passport_trip_model_t *s_trip_model;
static passport_location_model_t *s_location_model;
static passport_trip_rx_t s_trip_rx;
static passport_avatar_rx_t s_avatar_rx;
static volatile uint16_t s_connection = BLE_HS_CONN_HANDLE_NONE;
static volatile bool s_request_subscribed;
static volatile bool s_request_indications;
static volatile bool s_link_secured;
static volatile uint32_t s_pairing_code;
static uint8_t s_address_type;
static bool s_started;
static bool s_start_requested;
static passport_ble_event_cb_t s_callback;
static void *s_callback_context;

typedef struct {
    bool active;
    bool waiting_tx;
    uint8_t opcode;
    uint16_t request_id;
    uint8_t payload[2 + PASSPORT_TRIP_ID_BYTES + PASSPORT_TRIP_NAME_BYTES];
    size_t payload_length;
    size_t offset;
    size_t in_flight_length;
} passport_request_tx_t;

static passport_request_tx_t s_request_tx;
static struct ble_npl_callout s_request_tx_callout;
static bool s_request_tx_callout_ready;

static esp_err_t send_next_request_frame(void);

static bool notify_app(passport_ble_event_t event,
                       const passport_ble_result_t *result)
{
    return s_callback != NULL && s_callback(event, result, s_callback_context);
}

static void clear_request_tx(void)
{
    if (s_request_tx_callout_ready) ble_npl_callout_stop(&s_request_tx_callout);
    memset(&s_request_tx, 0, sizeof(s_request_tx));
}

static void request_tx_callout_cb(struct ble_npl_event *event)
{
    (void)event;
    if (!s_request_tx.active || s_request_tx.waiting_tx) return;
    if (send_next_request_frame() != ESP_OK) {
        clear_request_tx();
        notify_app(PASSPORT_BLE_REQUEST_FAILED, NULL);
    }
}

static bool schedule_request_tx(void)
{
    if (!s_request_tx_callout_ready) return false;
    ble_npl_time_t delay = ble_npl_time_ms_to_ticks32(1);
    if (delay == 0) delay = 1;
    return ble_npl_callout_reset(&s_request_tx_callout, delay) == BLE_NPL_OK;
}

static void log_memory(const char *stage)
{
    const uint32_t caps = MALLOC_CAP_INTERNAL | MALLOC_CAP_8BIT;
    ESP_LOGI(TAG, "memory %s: free=%u minimum=%u largest=%u stack_free=%u",
             stage,
             (unsigned)heap_caps_get_free_size(caps),
             (unsigned)heap_caps_get_minimum_free_size(caps),
             (unsigned)heap_caps_get_largest_free_block(caps),
             (unsigned)uxTaskGetStackHighWaterMark(NULL));
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
    ESP_LOGI(TAG, "received result id=%u success=%d error=%u",
             (unsigned)result.request_id, result.success, (unsigned)result.error_code);
    if (!notify_app(PASSPORT_BLE_RESULT, &result))
        return BLE_ATT_ERR_INSUFFICIENT_RES;
    return 0;
}

static int snapshot_access(uint16_t connection, uint16_t attribute,
                           struct ble_gatt_access_ctxt *context, void *arg)
{
    (void)connection; (void)attribute; (void)arg;
    if (context->op != BLE_GATT_ACCESS_OP_WRITE_CHR || s_trip_model == NULL)
        return BLE_ATT_ERR_UNLIKELY;
    uint8_t frame[PASSPORT_BLE_MAX_WRITE_BYTES];
    uint16_t length = 0;
    if (OS_MBUF_PKTLEN(context->om) > sizeof(frame) ||
        ble_hs_mbuf_to_flat(context->om, frame, sizeof(frame), &length) != 0)
        return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN;
    if (length < 2) return BLE_ATT_ERR_INVALID_ATTR_VALUE_LEN;
    if (frame[1] >= PASSPORT_TRACK_NAMES_BEGIN_OPCODE &&
        frame[1] <= PASSPORT_TRACK_NAMES_COMMIT_OPCODE) {
        if (s_location_model == NULL) return BLE_ATT_ERR_UNLIKELY;
        bool changed = false;
        if (!passport_track_name_sync_frame(&s_trip_rx, s_location_model,
                                             frame, length, &changed))
            return BLE_ATT_ERR_VALUE_NOT_ALLOWED;
        if (frame[1] == PASSPORT_TRACK_NAMES_COMMIT_OPCODE && changed)
            (void)notify_app(PASSPORT_BLE_TRACK_NAMES, NULL);
        return 0;
    }
    if (frame[1] >= PASSPORT_AVATAR_BEGIN_OPCODE &&
        frame[1] <= PASSPORT_AVATAR_CLEAR_OPCODE) {
        passport_avatar_action_t action;
        if (!passport_avatar_sync_frame(&s_avatar_rx, frame, length, &action))
            return BLE_ATT_ERR_VALUE_NOT_ALLOWED;
        if (action == PASSPORT_AVATAR_ACTION_COMMIT) {
            if (passport_avatar_save(s_avatar_rx.payload) != ESP_OK)
                return BLE_ATT_ERR_UNLIKELY;
            notify_app(PASSPORT_BLE_AVATAR, NULL);
        } else if (action == PASSPORT_AVATAR_ACTION_CLEAR) {
            if (passport_avatar_clear() != ESP_OK)
                return BLE_ATT_ERR_UNLIKELY;
            notify_app(PASSPORT_BLE_AVATAR, NULL);
        }
        return 0;
    }
    if (frame[1] == PASSPORT_SCREEN_SETTING_OPCODE) {
        uint16_t seconds;
        if (!passport_display_setting_decode(frame, length, &seconds))
            return BLE_ATT_ERR_VALUE_NOT_ALLOWED;
        if (passport_trip_store_set_screen_timeout(seconds) != ESP_OK)
            return BLE_ATT_ERR_UNLIKELY;
        notify_app(PASSPORT_BLE_SETTINGS, NULL);
        return 0;
    }
    if (frame[1] == PASSPORT_TIME_SYNC_OPCODE) {
        passport_ble_result_t time_result = { .success = true };
        if (!passport_time_sync_decode(frame, length, &time_result.timestamp_utc,
                                       &time_result.utc_offset_minutes))
            return BLE_ATT_ERR_VALUE_NOT_ALLOWED;
        ESP_LOGI(TAG, "received time utc=%lld offset=%d",
                 (long long)time_result.timestamp_utc,
                 (int)time_result.utc_offset_minutes);
        if (!notify_app(PASSPORT_BLE_TIME, &time_result))
            return BLE_ATT_ERR_INSUFFICIENT_RES;
        return 0;
    }
    const bool committing = frame[1] == PASSPORT_SNAPSHOT_COMMIT_OPCODE;
    passport_trip_model_t *candidate = NULL;
    passport_trip_model_t *target = s_trip_model;
    if (committing) {
        candidate = heap_caps_malloc(sizeof(*candidate), MALLOC_CAP_INTERNAL | MALLOC_CAP_8BIT);
        if (candidate == NULL) {
            ESP_LOGE(TAG, "snapshot candidate allocation failed bytes=%u",
                     (unsigned)sizeof(*candidate));
            log_memory("snapshot-candidate-failed");
            return BLE_ATT_ERR_INSUFFICIENT_RES;
        }
        target = candidate;
    }
    if (!passport_trip_sync_frame(&s_trip_rx, target, frame, length)) {
        free(candidate);
        return BLE_ATT_ERR_VALUE_NOT_ALLOWED;
    }
    if (committing) {
        if (passport_trip_store_save(candidate) != ESP_OK) {
            free(candidate);
            return BLE_ATT_ERR_UNLIKELY;
        }
        *s_trip_model = *candidate;
        free(candidate);
        log_memory("snapshot-committed");
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
                .flags = BLE_GATT_CHR_F_NOTIFY | BLE_GATT_CHR_F_INDICATE |
                         BLE_GATT_CHR_F_READ_AUTHEN,
                .min_key_size = 16,
                .val_handle = &s_request_value_handle,
            },
            {
                .uuid = &RESULT_UUID.u,
                .access_cb = result_access,
                .flags = BLE_GATT_CHR_F_WRITE | BLE_GATT_CHR_F_WRITE_AUTHEN,
                .min_key_size = 16,
            },
            {
                .uuid = &SNAPSHOT_UUID.u,
                .access_cb = snapshot_access,
                .flags = BLE_GATT_CHR_F_WRITE | BLE_GATT_CHR_F_WRITE_AUTHEN,
                .min_key_size = 16,
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
    struct ble_gap_conn_desc desc;
    switch (event->type) {
    case BLE_GAP_EVENT_CONNECT:
        if (event->connect.status == 0) {
            s_connection = event->connect.conn_handle;
            s_link_secured = false;
            ESP_LOGI(TAG, "central connected; handle=%u, starting authenticated encryption",
                     (unsigned)s_connection);
            const int rc = ble_gap_security_initiate(s_connection);
            if (rc != 0 && rc != BLE_HS_EALREADY)
                ESP_LOGW(TAG, "security initiation failed: %d", rc);
            log_memory("connected");
        } else if (s_start_requested) {
            ESP_LOGW(TAG, "connection attempt failed: %d", event->connect.status);
            const int rc = advertise();
            if (rc != 0) {
                ESP_LOGE(TAG, "BLE restart advertising failed: %d", rc);
                notify_app(PASSPORT_BLE_FAILED, NULL);
            }
        }
        break;
    case BLE_GAP_EVENT_DISCONNECT:
        ESP_LOGI(TAG, "central disconnected; reason=%d", event->disconnect.reason);
        s_connection = BLE_HS_CONN_HANDLE_NONE;
        s_request_subscribed = false;
        s_request_indications = false;
        clear_request_tx();
        s_link_secured = false;
        s_pairing_code = 0;
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
        ESP_LOGI(TAG, "subscription changed; attr=%u notify=%d indicate=%d",
                 (unsigned)event->subscribe.attr_handle,
                 event->subscribe.cur_notify, event->subscribe.cur_indicate);
        if (event->subscribe.attr_handle == s_request_value_handle) {
            s_request_indications = event->subscribe.cur_indicate;
            s_request_subscribed = event->subscribe.cur_notify ||
                                   event->subscribe.cur_indicate;
            notify_app(s_request_subscribed && s_link_secured ? PASSPORT_BLE_CONNECTED
                                            : PASSPORT_BLE_DISCONNECTED, NULL);
        }
        break;
    case BLE_GAP_EVENT_NOTIFY_TX:
        if (event->notify_tx.attr_handle != s_request_value_handle ||
            !s_request_tx.active || !s_request_tx.waiting_tx) break;
        if (event->notify_tx.indication && event->notify_tx.status == 0) break;
        if ((!event->notify_tx.indication && event->notify_tx.status != 0) ||
            (event->notify_tx.indication && event->notify_tx.status != BLE_HS_EDONE)) {
            ESP_LOGW(TAG, "request delivery failed: status=%d indication=%d",
                     event->notify_tx.status, event->notify_tx.indication);
            clear_request_tx();
            notify_app(PASSPORT_BLE_REQUEST_FAILED, NULL);
            break;
        }
        s_request_tx.waiting_tx = false;
        s_request_tx.offset += s_request_tx.in_flight_length;
        if ((s_request_tx.opcode == PASSPORT_LOCATION_REQUEST_OPCODE &&
             s_request_tx.offset >= 1) ||
            (s_request_tx.opcode != PASSPORT_LOCATION_REQUEST_OPCODE &&
             s_request_tx.offset >= s_request_tx.payload_length)) {
            clear_request_tx();
        } else if (!schedule_request_tx()) {
            ESP_LOGW(TAG, "failed to defer next request frame");
            clear_request_tx();
            notify_app(PASSPORT_BLE_REQUEST_FAILED, NULL);
        }
        break;
    case BLE_GAP_EVENT_ENC_CHANGE:
        s_link_secured = event->enc_change.status == 0 &&
                         ble_gap_conn_find(event->enc_change.conn_handle, &desc) == 0 &&
                         desc.sec_state.encrypted && desc.sec_state.authenticated &&
                         desc.sec_state.key_size == 16;
        s_pairing_code = 0;
        if (!s_link_secured) {
            ESP_LOGW(TAG, "authenticated encryption unavailable: %d", event->enc_change.status);
            notify_app(PASSPORT_BLE_FAILED, NULL);
            if (ble_gap_conn_find(event->enc_change.conn_handle, &desc) == 0) {
                const int delete_rc = ble_store_util_delete_peer(&desc.peer_id_addr);
                ESP_LOGW(TAG, "removed stale peer keys after security failure: %d", delete_rc);
            }
            const int terminate_rc = ble_gap_terminate(event->enc_change.conn_handle,
                                                       BLE_ERR_REM_USER_CONN_TERM);
            if (terminate_rc != 0 && terminate_rc != BLE_HS_ENOTCONN)
                ESP_LOGW(TAG, "failed to terminate insecure connection: %d", terminate_rc);
        } else if (s_request_subscribed) {
            ESP_LOGI(TAG, "authenticated encrypted link established; key_size=%u",
                     (unsigned)desc.sec_state.key_size);
            notify_app(PASSPORT_BLE_CONNECTED, NULL);
            log_memory("encrypted");
        }
        break;
    case BLE_GAP_EVENT_REPEAT_PAIRING:
        if (ble_gap_conn_find(event->repeat_pairing.conn_handle, &desc) != 0)
            return BLE_GAP_REPEAT_PAIRING_IGNORE;
        ESP_LOGW(TAG, "repeat pairing requested; deleting stale peer keys");
        ble_store_util_delete_peer(&desc.peer_id_addr);
        return BLE_GAP_REPEAT_PAIRING_RETRY;
    case BLE_GAP_EVENT_PASSKEY_ACTION:
        if (event->passkey.params.action == BLE_SM_IOACT_DISP) {
            struct ble_sm_io passkey = { 0 };
            passkey.action = BLE_SM_IOACT_DISP;
            passkey.passkey = 100000 + esp_random() % 900000;
            s_pairing_code = passkey.passkey;
            notify_app(PASSPORT_BLE_PAIRING_CODE, NULL);
            const int rc = ble_sm_inject_io(event->passkey.conn_handle, &passkey);
            if (rc != 0) {
                s_pairing_code = 0;
                ESP_LOGE(TAG, "passkey injection failed: %d", rc);
                notify_app(PASSPORT_BLE_FAILED, NULL);
                return rc;
            }
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
    s_request_indications = false;
    clear_request_tx();
    s_link_secured = false;
    s_pairing_code = 0;
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
    s_request_subscribed = false;
    s_request_indications = false;
    clear_request_tx();

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
    int callout_rc = ble_npl_callout_init(&s_request_tx_callout,
                                           nimble_port_get_dflt_eventq(),
                                           request_tx_callout_cb, NULL);
    if (callout_rc != BLE_NPL_OK) {
        ESP_LOGE(TAG, "request TX callout init failed: %d", callout_rc);
        err = ESP_FAIL;
        goto failed;
    }
    s_request_tx_callout_ready = true;
    log_memory("nimble-ready");
    s_started = true;
    ble_svc_gap_init();
    ble_svc_gatt_init();
    int rc = ble_svc_gap_device_name_set(DEVICE_NAME);
    if (rc == 0) rc = ble_gatts_count_cfg(GATT_SERVICES);
    if (rc == 0) rc = ble_gatts_add_svcs(GATT_SERVICES);
    if (rc != 0) { err = ESP_FAIL; goto failed; }

    ble_hs_cfg.reset_cb = on_reset;
    ble_hs_cfg.sync_cb = on_sync;
    ble_hs_cfg.store_status_cb = ble_store_util_status_rr;
    ble_hs_cfg.sm_io_cap = BLE_HS_IO_DISPLAY_ONLY;
    ble_hs_cfg.sm_bonding = 1;
    ble_hs_cfg.sm_mitm = 1;
    ble_hs_cfg.sm_sc = 1;
    ble_hs_cfg.sm_our_key_dist = BLE_SM_PAIR_KEY_DIST_ENC | BLE_SM_PAIR_KEY_DIST_ID;
    ble_hs_cfg.sm_their_key_dist = BLE_SM_PAIR_KEY_DIST_ENC | BLE_SM_PAIR_KEY_DIST_ID;
    ble_store_config_init();
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
    if (s_request_tx_callout_ready) {
        ble_npl_callout_stop(&s_request_tx_callout);
        ble_npl_callout_deinit(&s_request_tx_callout);
        s_request_tx_callout_ready = false;
    }
    (void)nimble_port_deinit();
    s_started = false;
    return err;
}

void passport_ble_link_bind_trip_model(passport_trip_model_t *model)
{
    s_trip_model = model;
    memset(&s_trip_rx, 0, sizeof(s_trip_rx));
    memset(&s_avatar_rx, 0, sizeof(s_avatar_rx));
}

void passport_ble_link_bind_location_model(passport_location_model_t *model)
{
    s_location_model = model;
}

esp_err_t passport_ble_link_request_location(uint16_t request_id)
{
    if (s_connection == BLE_HS_CONN_HANDLE_NONE || !s_request_subscribed || !s_link_secured)
        return ESP_ERR_INVALID_STATE;
    if (s_request_tx.active) return ESP_ERR_INVALID_STATE;
    clear_request_tx();
    s_request_tx.active = true;
    s_request_tx.opcode = PASSPORT_LOCATION_REQUEST_OPCODE;
    s_request_tx.request_id = request_id;
    const esp_err_t err = send_next_request_frame();
    if (err != ESP_OK) clear_request_tx();
    return err;
}

static esp_err_t send_next_request_frame(void)
{
    if (!s_request_tx.active || s_request_tx.waiting_tx ||
        s_connection == BLE_HS_CONN_HANDLE_NONE || !s_request_subscribed || !s_link_secured)
        return ESP_ERR_INVALID_STATE;

    uint8_t frame[20];
    size_t frame_length = 0;
    size_t delivered_length = 0;
    if (s_request_tx.opcode == PASSPORT_LOCATION_REQUEST_OPCODE) {
        frame_length = passport_location_protocol_encode_request(
            s_request_tx.request_id, frame, sizeof(frame));
        delivered_length = frame_length == PASSPORT_LOCATION_REQUEST_SIZE ? 1 : 0;
    } else {
        const size_t remaining = s_request_tx.payload_length - s_request_tx.offset;
        const size_t part = remaining < 11 ? remaining : 11;
        frame[0] = PASSPORT_LOCATION_PROTOCOL_VERSION;
        frame[1] = PASSPORT_CHECKIN_FRAGMENT_OPCODE;
        frame[2] = s_request_tx.opcode;
        frame[3] = (uint8_t)s_request_tx.request_id;
        frame[4] = (uint8_t)(s_request_tx.request_id >> 8);
        frame[5] = (uint8_t)s_request_tx.payload_length;
        frame[6] = (uint8_t)(s_request_tx.payload_length >> 8);
        frame[7] = (uint8_t)s_request_tx.offset;
        frame[8] = (uint8_t)(s_request_tx.offset >> 8);
        memcpy(frame + 9, s_request_tx.payload + s_request_tx.offset, part);
        frame_length = 9 + part;
        delivered_length = part;
    }
    if (delivered_length == 0) return ESP_ERR_INVALID_SIZE;

    struct os_mbuf *om = ble_hs_mbuf_from_flat(frame, frame_length);
    if (om == NULL) return ESP_ERR_NO_MEM;
    s_request_tx.waiting_tx = true;
    s_request_tx.in_flight_length = delivered_length;
    const int rc = s_request_indications
        ? ble_gatts_indicate_custom(s_connection, s_request_value_handle, om)
        : ble_gatts_notify_custom(s_connection, s_request_value_handle, om);
    if (rc != 0) {
        s_request_tx.waiting_tx = false;
        s_request_tx.in_flight_length = 0;
        ESP_LOGW(TAG, "request transmit start failed: %d", rc);
        return ESP_FAIL;
    }
    ESP_LOGI(TAG, "request frame sent id=%u opcode=%u offset=%u length=%u mode=%s",
             (unsigned)s_request_tx.request_id, (unsigned)s_request_tx.opcode,
             (unsigned)s_request_tx.offset, (unsigned)frame_length,
             s_request_indications ? "indicate" : "notify");
    return ESP_OK;
}

static esp_err_t request_destination_state(uint16_t request_id,
                                          uint8_t opcode,
                                          const char *destination_id,
                                          const char *destination_name)
{
    if (destination_id == NULL || destination_name == NULL ||
        s_connection == BLE_HS_CONN_HANDLE_NONE || !s_request_subscribed || !s_link_secured)
        return ESP_ERR_INVALID_STATE;
    const size_t id_length = strnlen(destination_id, PASSPORT_TRIP_ID_BYTES);
    const size_t name_length = strnlen(destination_name, PASSPORT_TRIP_NAME_BYTES);
    if (id_length == 0 || id_length >= PASSPORT_TRIP_ID_BYTES ||
        name_length == 0 || name_length >= PASSPORT_TRIP_NAME_BYTES)
        return ESP_ERR_INVALID_ARG;

    if (s_request_tx.active) return ESP_ERR_INVALID_STATE;
    uint8_t payload[2 + PASSPORT_TRIP_ID_BYTES + PASSPORT_TRIP_NAME_BYTES];
    size_t used = 0;
    payload[used++] = (uint8_t)id_length;
    memcpy(payload + used, destination_id, id_length); used += id_length;
    payload[used++] = (uint8_t)name_length;
    memcpy(payload + used, destination_name, name_length); used += name_length;
    clear_request_tx();
    s_request_tx.active = true;
    s_request_tx.opcode = opcode;
    s_request_tx.request_id = request_id;
    memcpy(s_request_tx.payload, payload, used);
    s_request_tx.payload_length = used;
    const esp_err_t err = send_next_request_frame();
    if (err != ESP_OK) clear_request_tx();
    return err;
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
    return s_connection != BLE_HS_CONN_HANDLE_NONE && s_request_subscribed && s_link_secured;
}

uint32_t passport_ble_link_pairing_code(void)
{
    return s_pairing_code;
}
