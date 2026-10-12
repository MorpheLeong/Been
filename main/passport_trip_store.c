#include "passport_trip_store.h"
#include "passport_display_policy.h"
#include "esp_log.h"
#include "nvs.h"
#include "nvs_flash.h"
#include <stddef.h>
#include <stdlib.h>
#include <string.h>

#define TRIP_NAMESPACE "passport_trip"
#define TRIP_KEY "snapshot"
#define TRIP_MAGIC 0x54525032U

typedef struct {
    uint32_t magic;
    uint16_t version;
    uint16_t payload_size;
    uint32_t crc;
    passport_trip_model_t model;
} trip_blob_t;

/* Preserve v1 snapshots created before profile and sound settings were added. */
typedef struct {
    char id[PASSPORT_TRIP_ID_BYTES];
    char name[PASSPORT_TRIP_NAME_BYTES];
    bool completed;
} passport_trip_destination_v1_t;
typedef struct {
    char id[PASSPORT_TRIP_ID_BYTES];
    char name[PASSPORT_TRIP_NAME_BYTES];
    char payload[PASSPORT_TRIP_PAYLOAD_BYTES];
    char destination_id[PASSPORT_TRIP_ID_BYTES];
    bool starred;
} passport_trip_qr_v1_t;
typedef struct {
    uint16_t revision;
    uint8_t destination_count;
    uint8_t qr_count;
    passport_trip_destination_v1_t destinations[PASSPORT_TRIP_DESTINATION_CAPACITY];
    passport_trip_qr_v1_t qrs[PASSPORT_TRIP_QR_CAPACITY];
} passport_trip_model_v1_t;
typedef struct {
    uint32_t magic;
    uint16_t version;
    uint16_t payload_size;
    uint32_t crc;
    passport_trip_model_v1_t model;
} trip_blob_v1_t;

static const char *TAG = "passport_trip_store";
static bool s_ready;
static bool s_writable;
static uint16_t s_screen_timeout = PASSPORT_SCREEN_TIMEOUT_DEFAULT;

static uint32_t crc32(const uint8_t *data, size_t length)
{
    uint32_t crc = 0xFFFFFFFFU;
    for (size_t i = 0; i < length; ++i) {
        crc ^= data[i];
        for (unsigned bit = 0; bit < 8; ++bit) {
            const uint32_t mask = (uint32_t)-(int32_t)(crc & 1U);
            crc = (crc >> 1) ^ (0xEDB88320U & mask);
        }
    }
    return ~crc;
}
esp_err_t passport_trip_store_init(passport_trip_model_t *model)
{
    if (model == NULL) return ESP_ERR_INVALID_ARG;
    if (s_ready) return ESP_OK;
    passport_trip_model_init(model);
    nvs_handle_t handle;
    esp_err_t err = nvs_open(TRIP_NAMESPACE, NVS_READWRITE, &handle);
    if (err == ESP_ERR_NVS_NOT_FOUND) {
        s_ready = s_writable = true;
        return ESP_OK;
    }
    if (err != ESP_OK) { s_ready = true; return err; }
    uint16_t stored_timeout = 0;
    if (nvs_get_u16(handle, "screen_secs", &stored_timeout) == ESP_OK &&
        stored_timeout >= PASSPORT_SCREEN_TIMEOUT_MIN &&
        stored_timeout <= PASSPORT_SCREEN_TIMEOUT_MAX)
        s_screen_timeout = stored_timeout;
    size_t size = 0;
    err = nvs_get_blob(handle, TRIP_KEY, NULL, &size);
    if (err == ESP_ERR_NVS_NOT_FOUND) {
        s_writable = true;
        err = ESP_OK;
    } else if (err == ESP_OK && size == sizeof(trip_blob_t)) {
        trip_blob_t *blob = malloc(sizeof(*blob));
        if (blob == NULL) err = ESP_ERR_NO_MEM;
        else {
            err = nvs_get_blob(handle, TRIP_KEY, blob, &size);
            if (err == ESP_OK && blob->magic == TRIP_MAGIC && blob->version == 2 &&
                blob->payload_size == sizeof(blob->model) &&
                blob->crc == crc32((const uint8_t *)&blob->model, sizeof(blob->model)) &&
                passport_trip_model_valid(&blob->model)) {
                *model = blob->model;
                s_writable = true;
            } else if (err == ESP_OK) err = ESP_ERR_INVALID_CRC;
            free(blob);
        }
    } else if (err == ESP_OK && size == sizeof(trip_blob_v1_t)) {
        trip_blob_v1_t *old = malloc(sizeof(*old));
        if (old == NULL) err = ESP_ERR_NO_MEM;
        else {
            err = nvs_get_blob(handle, TRIP_KEY, old, &size);
            if (err == ESP_OK && old->magic == TRIP_MAGIC && old->version == 1 &&
                old->payload_size == sizeof(old->model) &&
                old->crc == crc32((const uint8_t *)&old->model, sizeof(old->model))) {
                model->revision = old->model.revision;
                model->destination_count = old->model.destination_count;
                model->qr_count = old->model.qr_count;
                memcpy(model->destinations, old->model.destinations, sizeof(old->model.destinations));
                memcpy(model->qrs, old->model.qrs, sizeof(old->model.qrs));
                model->profile.volume_percent = 50;
                if (passport_trip_model_valid(model)) s_writable = true;
                else err = ESP_ERR_INVALID_CRC;
            }
            free(old);
        }
    } else if (err == ESP_OK) {
        err = ESP_ERR_INVALID_SIZE;
    }
    if (err != ESP_OK && err != ESP_ERR_NO_MEM) {
        ESP_LOGE(TAG, "Stored itinerary invalid; preserving flash and using RAM");
        s_writable = false;
        if (err == ESP_OK) err = ESP_ERR_INVALID_CRC;
    }
    nvs_close(handle);
    s_ready = true;
    /* Rewrite a valid v1 snapshot in the current format without erasing user data. */
    if (err == ESP_OK && s_writable) (void)passport_trip_store_save(model);
    return err;
}

uint16_t passport_trip_store_screen_timeout(void)
{
    return s_screen_timeout;
}

esp_err_t passport_trip_store_set_screen_timeout(uint16_t seconds)
{
    if (seconds < PASSPORT_SCREEN_TIMEOUT_MIN || seconds > PASSPORT_SCREEN_TIMEOUT_MAX)
        return ESP_ERR_INVALID_ARG;
    if (!s_ready) return ESP_ERR_INVALID_STATE;
    nvs_handle_t handle;
    esp_err_t err = nvs_open(TRIP_NAMESPACE, NVS_READWRITE, &handle);
    if (err != ESP_OK) return err;
    err = nvs_set_u16(handle, "screen_secs", seconds);
    if (err == ESP_OK) err = nvs_commit(handle);
    nvs_close(handle);
    if (err == ESP_OK) s_screen_timeout = seconds;
    return err;
}
esp_err_t passport_trip_store_save(const passport_trip_model_t *model)
{
    if (model == NULL || !passport_trip_model_valid(model)) return ESP_ERR_INVALID_ARG;
    if (!s_ready || !s_writable) return ESP_ERR_INVALID_STATE;
    trip_blob_t *blob = malloc(sizeof(*blob));
    if (blob == NULL) return ESP_ERR_NO_MEM;
    memset(blob, 0, sizeof(*blob));
    blob->magic = TRIP_MAGIC;
    blob->version = 2;
    blob->payload_size = sizeof(blob->model);
    blob->model = *model;
    blob->crc = crc32((const uint8_t *)&blob->model, sizeof(blob->model));
    nvs_handle_t handle;
    esp_err_t err = nvs_open(TRIP_NAMESPACE, NVS_READWRITE, &handle);
    if (err == ESP_OK) {
        err = nvs_set_blob(handle, TRIP_KEY, blob, sizeof(*blob));
        if (err == ESP_OK) err = nvs_commit(handle);
        nvs_close(handle);
    }
    free(blob);
    if (err != ESP_OK) {
        s_writable = false;
        ESP_LOGE(TAG, "Snapshot persistence failed; kept in RAM: %s", esp_err_to_name(err));
    }
    return err;
}
