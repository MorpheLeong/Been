#include <assert.h>
#include <stdint.h>
#include <string.h>

#include "passport_location_protocol.h"

static void put_u16_le(uint8_t *p, uint16_t value)
{
    p[0] = (uint8_t)value;
    p[1] = (uint8_t)(value >> 8);
}

static void put_u32_le(uint8_t *p, uint32_t value)
{
    for (unsigned i = 0; i < 4; ++i) p[i] = (uint8_t)(value >> (8 * i));
}

static void test_request_encoding(void)
{
    uint8_t frame[PASSPORT_LOCATION_REQUEST_SIZE] = { 0 };
    assert(passport_location_protocol_encode_request(0x5678, frame, sizeof(frame)) == sizeof(frame));
    const uint8_t expected[] = { 3, 1, 0x78, 0x56 };
    assert(memcmp(frame, expected, sizeof(expected)) == 0);
    assert(passport_location_protocol_encode_request(1, frame, sizeof(frame) - 1) == 0);
    assert(passport_location_protocol_encode_request(1, NULL, sizeof(frame)) == 0);
}

static void test_successful_result_decoding(void)
{
    uint8_t frame[PASSPORT_LOCATION_SUCCESS_SIZE] = { 3, 0x81, 0 };
    put_u16_le(&frame[3], 42);
    put_u32_le(&frame[5], 1790985600);
    put_u16_le(&frame[9], 480);

    passport_ble_result_t result;
    assert(passport_location_protocol_decode_result(frame, sizeof(frame), &result));
    assert(result.success && result.request_id == 42);
    assert(result.timestamp_utc == 1790985600);
    assert(result.latitude_e7 == 0 && result.longitude_e7 == 0);
    assert(result.accuracy_meters == 0 && result.utc_offset_minutes == 480);
}

static void test_rejects_malformed_and_out_of_range_results(void)
{
    uint8_t frame[PASSPORT_LOCATION_SUCCESS_SIZE] = { 3, 0x81, 0 };
    put_u32_le(&frame[5], 1790985600);
    put_u16_le(&frame[9], 841);
    passport_ble_result_t result;
    assert(!passport_location_protocol_decode_result(frame, sizeof(frame), &result));
    assert(!passport_location_protocol_decode_result(frame, sizeof(frame) - 1, &result));
    frame[0] = 2;
    assert(!passport_location_protocol_decode_result(frame, sizeof(frame), &result));
    assert(!passport_location_protocol_decode_result(frame, sizeof(frame), NULL));
}

static void test_failure_result_decoding(void)
{
    uint8_t frame[PASSPORT_LOCATION_FAILURE_SIZE] = { 3, 0x81, 3, 0x09, 0, 7 };
    passport_ble_result_t result;
    assert(passport_location_protocol_decode_result(frame, sizeof(frame), &result));
    assert(!result.success && result.request_id == 9 && result.error_code == 7);
    assert(!passport_location_protocol_decode_result(frame, sizeof(frame) - 1, &result));
}

static void test_time_sync_decoding(void)
{
    uint8_t frame[PASSPORT_TIME_SYNC_SIZE] = { 3, PASSPORT_TIME_SYNC_OPCODE };
    put_u32_le(&frame[2], 1790985600);
    put_u16_le(&frame[6], (uint16_t)(int16_t)-420);
    int64_t timestamp = 0;
    int16_t offset = 0;
    assert(passport_time_sync_decode(frame, sizeof(frame), &timestamp, &offset));
    assert(timestamp == 1790985600 && offset == -420);
    assert(!passport_time_sync_decode(frame, sizeof(frame) - 1, &timestamp, &offset));
    frame[6] = 0x49;
    frame[7] = 0x03;
    assert(!passport_time_sync_decode(frame, sizeof(frame), &timestamp, &offset));
}

int main(void)
{
    test_request_encoding();
    test_successful_result_decoding();
    test_rejects_malformed_and_out_of_range_results();
    test_failure_result_decoding();
    test_time_sync_decoding();
    return 0;
}
