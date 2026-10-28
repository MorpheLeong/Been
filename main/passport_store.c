#include "passport_store.h"
#include "passport_data_nvs.h"

#include "esp_log.h"
#include "nvs.h"
#include "nvs_flash.h"

#include <stddef.h>
#include <stdlib.h>
#include <string.h>

#define STORE_NAMESPACE "passport"
#define STORE_KEY "records"
#define STORE_VERSION 2U
#define STORE_LEGACY_VERSION 1U

typedef struct {
    uint32_t magic;
    uint16_t version;
    uint16_t payload_size;
    uint32_t crc32;
    passport_location_model_t model;
} store_blob_t;

static const char *TAG = "passport_store";
static bool s_initialized;
static bool s_writable;

static uint32_t crc32_bytes(const uint8_t *data, size_t size)
{
    uint32_t crc = 0xFFFFFFFFU;
    for (size_t i = 0; i < size; ++i) {
        crc ^= data[i];
        for (unsigned bit = 0; bit < 8; ++bit) {
            const uint32_t mask = (uint32_t)-(int32_t)(crc & 1U);
            crc = (crc >> 1) ^ (0xEDB88320U & mask);
        }
    }
    return ~crc;
}

static uint32_t blob_crc(const store_blob_t *blob)
{
    return crc32_bytes((const uint8_t *)&blob->model, sizeof(blob->model));
}

static esp_err_t load_blob(nvs_handle_t handle, passport_location_model_t *model,
                           bool *migration_needed)
{
    if (migration_needed != NULL) *migration_needed = false;
    store_blob_t *blob = malloc(sizeof(*blob));
    if (blob == NULL) return ESP_ERR_NO_MEM;
    size_t size = sizeof(*blob);
    esp_err_t err = nvs_get_blob(handle, STORE_KEY, blob, &size);
    if (err == ESP_OK &&
        (size != sizeof(*blob) || blob->magic != 0x41505031U ||
         (blob->version != STORE_VERSION && blob->version != STORE_LEGACY_VERSION) ||
         blob->payload_size != sizeof(blob->model) || blob->crc32 != blob_crc(blob))) {
        err = ESP_ERR_INVALID_CRC;
    }
    if (err == ESP_OK && blob->version == STORE_LEGACY_VERSION) {
        for (size_t i = 0; i < PASSPORT_MARKER_CAPACITY; ++i)
            blob->model.markers[i].source_request_id = 0;
        if (migration_needed != NULL) *migration_needed = true;
    }
    if (err == ESP_OK && !passport_location_model_valid(&blob->model))
        err = ESP_ERR_INVALID_CRC;
    if (err == ESP_OK) *model = blob->model;
    free(blob);
    return err;
}

static esp_err_t write_blob(nvs_handle_t handle, const passport_location_model_t *model)
{
    store_blob_t *blob = malloc(sizeof(*blob));
    if (blob == NULL) return ESP_ERR_NO_MEM;
    memset(blob, 0, sizeof(*blob));
    blob->magic = 0x41505031U;
    blob->version = STORE_VERSION;
    blob->payload_size = sizeof(model[0]);
    memcpy(&blob->model, model, sizeof(blob->model));
    blob->crc32 = blob_crc(blob);
    esp_err_t err = nvs_set_blob(handle, STORE_KEY, blob, sizeof(*blob));
    if (err == ESP_OK) err = nvs_commit(handle);
    if (err == ESP_OK) {
        memset(blob, 0, sizeof(*blob));
        size_t size = sizeof(*blob);
        err = nvs_get_blob(handle, STORE_KEY, blob, &size);
        if (err == ESP_OK &&
            (size != sizeof(*blob) || blob->magic != 0x41505031U ||
             blob->version != STORE_VERSION || blob->payload_size != sizeof(blob->model) ||
             blob->crc32 != blob_crc(blob) || !passport_location_model_valid(&blob->model))) {
            err = ESP_ERR_INVALID_CRC;
        }
    }
    free(blob);
    return err;
}

static void erase_legacy_namespace(void)
{
    nvs_handle_t legacy;
    if (nvs_open(STORE_NAMESPACE, NVS_READWRITE, &legacy) != ESP_OK) return;
    esp_err_t err = nvs_erase_all(legacy);
    if (err == ESP_OK) err = nvs_commit(legacy);
    nvs_close(legacy);
    if (err != ESP_OK) {
        ESP_LOGW(TAG, "Could not reclaim legacy record storage: %s", esp_err_to_name(err));
    }
}

esp_err_t passport_store_init(passport_location_model_t *model)
{
    if (model == NULL) return ESP_ERR_INVALID_ARG;
    if (s_initialized) return ESP_OK;

    esp_err_t err = nvs_flash_init();
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "NVS init failed; preserving flash and using RAM only: %s",
                 esp_err_to_name(err));
        s_initialized = true;
        s_writable = false;
        return err;
    }

    err = passport_data_nvs_init();
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "App-data NVS init failed; preserving flash and using RAM only: %s",
                 esp_err_to_name(err));
        s_initialized = true;
        s_writable = false;
        return err;
    }

    nvs_handle_t handle;
    err = nvs_open_from_partition(PASSPORT_DATA_NVS_PARTITION, STORE_NAMESPACE,
                                  NVS_READWRITE, &handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "NVS namespace unavailable; records remain RAM only: %s",
                 esp_err_to_name(err));
        s_initialized = true;
        s_writable = false;
        return err;
    }

    bool storage_valid = false;
    bool migration_needed = false;
    err = load_blob(handle, model, &migration_needed);
    if (err == ESP_OK) {
        if (migration_needed) {
            err = write_blob(handle, model);
            if (err == ESP_OK)
                ESP_LOGI(TAG, "Migrated location records to storage version %u",
                         (unsigned)STORE_VERSION);
        }
        storage_valid = err == ESP_OK;
    } else if (err == ESP_ERR_NVS_NOT_FOUND) {
        nvs_handle_t legacy;
        esp_err_t legacy_err = nvs_open(STORE_NAMESPACE, NVS_READONLY, &legacy);
        if (legacy_err == ESP_OK) {
            legacy_err = load_blob(legacy, model, NULL);
            nvs_close(legacy);
        }
        if (legacy_err == ESP_OK) {
            err = write_blob(handle, model);
            if (err == ESP_OK) {
                ESP_LOGI(TAG, "Migrated location records to dedicated app-data NVS");
                erase_legacy_namespace();
                storage_valid = true;
            }
        } else if (legacy_err == ESP_ERR_NVS_NOT_FOUND) {
            passport_location_model_init(model);
            storage_valid = true;
            err = ESP_OK;
        } else {
            err = legacy_err;
        }
    }
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Stored records could not be read; preserving data, using RAM only: %s",
                 esp_err_to_name(err));
    }
    nvs_close(handle);
    s_initialized = true;
    s_writable = storage_valid;
    return err;
}

bool passport_store_can_save(void)
{
    return s_initialized && s_writable;
}

esp_err_t passport_store_save(const passport_location_model_t *model)
{
    if (model == NULL || !passport_location_model_valid(model)) return ESP_ERR_INVALID_ARG;
    if (!passport_store_can_save()) return ESP_ERR_INVALID_STATE;

    nvs_handle_t handle;
    esp_err_t err = nvs_open_from_partition(PASSPORT_DATA_NVS_PARTITION, STORE_NAMESPACE,
                                            NVS_READWRITE, &handle);
    if (err != ESP_OK) return err;
    err = write_blob(handle, model);
    nvs_close(handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Save failed; existing data was not erased: %s", esp_err_to_name(err));
        s_writable = false;
    }
    return err;
}
