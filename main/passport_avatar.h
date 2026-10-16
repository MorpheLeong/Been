#pragma once

#include <stdbool.h>
#include <stdint.h>

#include "esp_err.h"
#include "passport_avatar_protocol.h"

esp_err_t passport_avatar_init(void);
bool passport_avatar_has_image(void);
const uint16_t *passport_avatar_pixels(void);
esp_err_t passport_avatar_save(const uint8_t rgb565[PASSPORT_AVATAR_BYTE_COUNT]);
esp_err_t passport_avatar_clear(void);
