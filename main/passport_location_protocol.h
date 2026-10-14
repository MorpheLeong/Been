#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#define PASSPORT_LOCATION_PROTOCOL_VERSION 3U
#define PASSPORT_LOCATION_REQUEST_OPCODE 0x01U
#define PASSPORT_LOCATION_RESULT_OPCODE 0x81U
#define PASSPORT_CHECKIN_REQUEST_OPCODE 0x02U
#define PASSPORT_UNCHECKIN_REQUEST_OPCODE 0x03U
#define PASSPORT_CHECKIN_FRAGMENT_OPCODE 0x42U
#define PASSPORT_SNAPSHOT_BEGIN_OPCODE 0x10U
#define PASSPORT_SNAPSHOT_CHUNK_OPCODE 0x11U
#define PASSPORT_SNAPSHOT_COMMIT_OPCODE 0x12U
#define PASSPORT_TIME_SYNC_OPCODE 0x14U
#define PASSPORT_LOCATION_REQUEST_SIZE 4U
#define PASSPORT_LOCATION_SUCCESS_SIZE 11U
#define PASSPORT_LOCATION_FAILURE_SIZE 6U
#define PASSPORT_TIME_SYNC_SIZE 8U

typedef struct {
    uint16_t request_id;
    bool success;
    uint8_t error_code;
    int64_t timestamp_utc;
    int32_t latitude_e7;
    int32_t longitude_e7;
    uint16_t accuracy_meters;
    int16_t utc_offset_minutes;
} passport_ble_result_t;

size_t passport_location_protocol_encode_request(uint16_t request_id,
                                                 uint8_t *frame,
                                                 size_t capacity);
bool passport_location_protocol_decode_result(const uint8_t *frame,
                                              size_t length,
                                              passport_ble_result_t *result);
bool passport_time_sync_decode(const uint8_t *frame, size_t length,
                               int64_t *timestamp_utc,
                               int16_t *utc_offset_minutes);
