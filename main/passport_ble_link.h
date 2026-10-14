#pragma once

#include <stdbool.h>
#include <stdint.h>

#include "esp_err.h"
#include "passport_location_protocol.h"
#include "passport_trip_model.h"

typedef enum {
    PASSPORT_BLE_CONNECTED,
    PASSPORT_BLE_DISCONNECTED,
    PASSPORT_BLE_RESULT,
    PASSPORT_BLE_SNAPSHOT,
    PASSPORT_BLE_FAILED,
    PASSPORT_BLE_SETTINGS,
    PASSPORT_BLE_TIME,
    PASSPORT_BLE_PAIRING_CODE,
    PASSPORT_BLE_REQUEST_FAILED,
} passport_ble_event_t;

typedef bool (*passport_ble_event_cb_t)(passport_ble_event_t event,
                                        const passport_ble_result_t *result,
                                        void *context);

// The phone acts as BLE central and writes the completed location result back.
esp_err_t passport_ble_link_start(passport_ble_event_cb_t callback, void *context);
esp_err_t passport_ble_link_request_location(uint16_t request_id);
esp_err_t passport_ble_link_request_checkin(uint16_t request_id,
                                            const char *destination_id,
                                            const char *destination_name);
esp_err_t passport_ble_link_request_uncheckin(uint16_t request_id,
                                              const char *destination_id,
                                              const char *destination_name);
void passport_ble_link_bind_trip_model(passport_trip_model_t *model);
bool passport_ble_link_is_connected(void);
uint32_t passport_ble_link_pairing_code(void);
