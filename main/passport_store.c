#include "passport_store.h"

#include "esp_log.h"
#include "nvs.h"
#include "nvs_flash.h"

#include <stddef.h>
#include <string.h>

#define STORE_NAMESPACE "passport"
#define STORE_KEY "records"
#define STORE_VERSION 1U

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

    nvs_handle_t handle;
    err = nvs_open(STORE_NAMESPACE, NVS_READWRITE, &handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "NVS namespace unavailable; records remain RAM only: %s",
                 esp_err_to_name(err));
        s_initialized = true;
        s_writable = false;
        return err;
    }

    store_blob_t blob;
    size_t size = sizeof(blob);
    err = nvs_get_blob(handle, STORE_KEY, &blob, &size);
    bool storage_valid = false;
    if (err == ESP_OK) {
        if (size == sizeof(blob) && blob.magic == 0x41505031U &&
            blob.version == STORE_VERSION && blob.payload_size == sizeof(blob.model) &&
            blob.crc32 == blob_crc(&blob) &&
            passport_location_model_valid(&blob.model)) {
            *model = blob.model;
            storage_valid = true;
        } else {
            ESP_LOGE(TAG, "Stored records failed validation; preserving data, using RAM only");
            s_writable = false;
            err = ESP_ERR_INVALID_CRC;
        }
    } else if (err == ESP_ERR_NVS_NOT_FOUND) {
        passport_location_model_init(model);
        storage_valid = true;
    } else {
        ESP_LOGE(TAG, "Stored records could not be read; preserving data, using RAM only: %s",
                 esp_err_to_name(err));
        s_writable = false;
    }
    nvs_close(handle);
    s_initialized = true;
    s_writable = storage_valid;
    return (err == ESP_ERR_NVS_NOT_FOUND) ? ESP_OK : err;
}

bool passport_store_can_save(void)
{
    return s_initialized && s_writable;
}

esp_err_t passport_store_save(const passport_location_model_t *model)
{
    if (model == NULL || !passport_location_model_valid(model)) return ESP_ERR_INVALID_ARG;
    if (!passport_store_can_save()) return ESP_ERR_INVALID_STATE;

    store_blob_t blob = {
        .magic = 0x41505031U,
        .version = STORE_VERSION,
        .payload_size = sizeof(model[0]),
        .model = *model,
    };
    blob.crc32 = blob_crc(&blob);

    nvs_handle_t handle;
    esp_err_t err = nvs_open(STORE_NAMESPACE, NVS_READWRITE, &handle);
    if (err != ESP_OK) return err;
    err = nvs_set_blob(handle, STORE_KEY, &blob, sizeof(blob));
    if (err == ESP_OK) err = nvs_commit(handle);
    nvs_close(handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Save failed; existing data was not erased: %s", esp_err_to_name(err));
        s_writable = false;
    }
    return err;
}
