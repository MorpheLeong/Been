#include "passport_avatar.h"

#include "passport_data_nvs.h"
#include "nvs.h"

#include <string.h>

#define AVATAR_NAMESPACE "passport_avatar"
#define AVATAR_KEY "image"
#define AVATAR_MAGIC 0x41565431U

typedef struct {
    uint32_t magic;
    uint16_t version;
    uint16_t payload_size;
    uint32_t crc32;
    uint8_t payload[PASSPORT_AVATAR_BYTE_COUNT];
} avatar_blob_t;

static uint16_t s_pixels[PASSPORT_AVATAR_WIDTH * PASSPORT_AVATAR_HEIGHT];
static avatar_blob_t s_blob;
static bool s_ready;
static bool s_has_image;

esp_err_t passport_avatar_init(void)
{
    if (s_ready) return ESP_OK;
    esp_err_t err = passport_data_nvs_init();
    if (err != ESP_OK) { s_ready = true; return err; }
    nvs_handle_t handle;
    err = nvs_open_from_partition(PASSPORT_DATA_NVS_PARTITION, AVATAR_NAMESPACE,
                                  NVS_READWRITE, &handle);
    if (err != ESP_OK) { s_ready = true; return err; }
    size_t size = sizeof(s_blob);
    err = nvs_get_blob(handle, AVATAR_KEY, &s_blob, &size);
    nvs_close(handle);
    if (err == ESP_ERR_NVS_NOT_FOUND) {
        s_ready = true;
        return ESP_OK;
    }
    if (err == ESP_OK && size == sizeof(s_blob) && s_blob.magic == AVATAR_MAGIC &&
        s_blob.version == 1 && s_blob.payload_size == PASSPORT_AVATAR_BYTE_COUNT &&
        s_blob.crc32 == passport_avatar_crc32(s_blob.payload, sizeof(s_blob.payload))) {
        memcpy(s_pixels, s_blob.payload, sizeof(s_pixels));
        s_has_image = true;
    } else if (err == ESP_OK) {
        err = ESP_ERR_INVALID_CRC;
    }
    s_ready = true;
    return err;
}

bool passport_avatar_has_image(void) { return s_has_image; }

const uint16_t *passport_avatar_pixels(void) { return s_has_image ? s_pixels : NULL; }

esp_err_t passport_avatar_save(const uint8_t rgb565[PASSPORT_AVATAR_BYTE_COUNT])
{
    if (rgb565 == NULL) return ESP_ERR_INVALID_ARG;
    esp_err_t err = passport_data_nvs_init();
    if (err != ESP_OK) return err;
    memset(&s_blob, 0, sizeof(s_blob));
    s_blob.magic = AVATAR_MAGIC;
    s_blob.version = 1;
    s_blob.payload_size = PASSPORT_AVATAR_BYTE_COUNT;
    memcpy(s_blob.payload, rgb565, sizeof(s_blob.payload));
    s_blob.crc32 = passport_avatar_crc32(s_blob.payload, sizeof(s_blob.payload));
    nvs_handle_t handle;
    err = nvs_open_from_partition(PASSPORT_DATA_NVS_PARTITION, AVATAR_NAMESPACE,
                                  NVS_READWRITE, &handle);
    if (err != ESP_OK) return err;
    err = nvs_set_blob(handle, AVATAR_KEY, &s_blob, sizeof(s_blob));
    if (err == ESP_OK) err = nvs_commit(handle);
    nvs_close(handle);
    if (err == ESP_OK) {
        memcpy(s_pixels, rgb565, sizeof(s_pixels));
        s_has_image = true;
    }
    return err;
}

esp_err_t passport_avatar_clear(void)
{
    esp_err_t err = passport_data_nvs_init();
    if (err != ESP_OK) return err;
    nvs_handle_t handle;
    err = nvs_open_from_partition(PASSPORT_DATA_NVS_PARTITION, AVATAR_NAMESPACE,
                                  NVS_READWRITE, &handle);
    if (err != ESP_OK) return err;
    err = nvs_erase_key(handle, AVATAR_KEY);
    if (err == ESP_ERR_NVS_NOT_FOUND) err = ESP_OK;
    if (err == ESP_OK) err = nvs_commit(handle);
    nvs_close(handle);
    if (err == ESP_OK) {
        memset(s_pixels, 0, sizeof(s_pixels));
        s_has_image = false;
    }
    return err;
}
