#pragma once

#include "esp_err.h"

#define PASSPORT_DATA_NVS_PARTITION "app_data"

/* Initialize the dedicated application-data NVS partition without erasing it
 * on failure. Default NVS is reserved for Bluetooth keys and small settings. */
esp_err_t passport_data_nvs_init(void);
