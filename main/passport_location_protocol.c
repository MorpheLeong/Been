#include "passport_location_protocol.h"

static uint16_t read_u16_le(const uint8_t *p)
{
    return (uint16_t)p[0] | ((uint16_t)p[1] << 8);
}

static uint32_t read_u32_le(const uint8_t *p)
{
    return (uint32_t)p[0] | ((uint32_t)p[1] << 8) |
           ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
}

size_t passport_location_protocol_encode_request(uint16_t request_id,
                                                 uint8_t *frame,
                                                 size_t capacity)
{
    if (frame == NULL || capacity < PASSPORT_LOCATION_REQUEST_SIZE) return 0;
    frame[0] = PASSPORT_LOCATION_PROTOCOL_VERSION;
    frame[1] = PASSPORT_LOCATION_REQUEST_OPCODE;
    frame[2] = (uint8_t)request_id;
    frame[3] = (uint8_t)(request_id >> 8);
    return PASSPORT_LOCATION_REQUEST_SIZE;
}

bool passport_location_protocol_decode_result(const uint8_t *frame,
                                              size_t length,
                                              passport_ble_result_t *result)
{
    if (frame == NULL || result == NULL || length < PASSPORT_LOCATION_FAILURE_SIZE ||
        frame[0] != PASSPORT_LOCATION_PROTOCOL_VERSION ||
        frame[1] != PASSPORT_LOCATION_RESULT_OPCODE) return false;

    passport_ble_result_t parsed = {
        .request_id = read_u16_le(&frame[3]),
        .success = frame[2] == 0,
        .error_code = frame[2] == 0 ? 0 : frame[5],
    };
    if (parsed.success) {
        if (length != PASSPORT_LOCATION_SUCCESS_SIZE) return false;
        parsed.timestamp_utc = read_u32_le(&frame[5]);
        parsed.utc_offset_minutes = (int16_t)read_u16_le(&frame[9]);
        if (parsed.timestamp_utc < 1577836800 ||
            parsed.utc_offset_minutes < -840 || parsed.utc_offset_minutes > 840) {
            return false;
        }
    } else if (length != PASSPORT_LOCATION_FAILURE_SIZE) {
        return false;
    }
    *result = parsed;
    return true;
}

bool passport_time_sync_decode(const uint8_t *frame, size_t length,
                               int64_t *timestamp_utc,
                               int16_t *utc_offset_minutes)
{
    if (frame == NULL || timestamp_utc == NULL || utc_offset_minutes == NULL ||
        length != PASSPORT_TIME_SYNC_SIZE ||
        frame[0] != PASSPORT_LOCATION_PROTOCOL_VERSION ||
        frame[1] != PASSPORT_TIME_SYNC_OPCODE) return false;

    const int64_t timestamp = read_u32_le(&frame[2]);
    const int16_t offset = (int16_t)read_u16_le(&frame[6]);
    if (timestamp < 1577836800 || offset < -840 || offset > 840) return false;
    *timestamp_utc = timestamp;
    *utc_offset_minutes = offset;
    return true;
}
