#pragma once

#include <stdbool.h>

#include "esp_err.h"
#include "passport_location_model.h"

// Initializes the app-owned NVS namespace and restores a validated snapshot.
// Storage errors leave a usable in-RAM model but disable writes; NVS is never
// erased automatically.
esp_err_t passport_store_init(passport_location_model_t *model);
bool passport_store_can_save(void);
esp_err_t passport_store_save(const passport_location_model_t *model);
