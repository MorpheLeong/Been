#include "passport_data_nvs.h"

#include "nvs_flash.h"

#include <stdbool.h>

static bool s_initialized;

esp_err_t passport_data_nvs_init(void)
{
    if (s_initialized) return ESP_OK;
    const esp_err_t err = nvs_flash_init_partition(PASSPORT_DATA_NVS_PARTITION);
    if (err == ESP_OK) s_initialized = true;
    return err;
}
