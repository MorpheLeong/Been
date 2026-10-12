#pragma once
#include "esp_err.h"
#include "passport_trip_model.h"
esp_err_t passport_trip_store_init(passport_trip_model_t *model);
esp_err_t passport_trip_store_save(const passport_trip_model_t *model);
uint16_t passport_trip_store_screen_timeout(void);
/* 单独提交亮屏设置，不改变现有行程快照格式；提交成功后才更新内存值。 */
esp_err_t passport_trip_store_set_screen_timeout(uint16_t seconds);
