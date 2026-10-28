#include <assert.h>
#include <stdint.h>
#include <string.h>

#include "passport_track_name_protocol.h"

static uint32_t crc32(const uint8_t *data, size_t length)
{
    uint32_t crc = 0xFFFFFFFFU;
    for (size_t i = 0; i < length; ++i) {
        crc ^= data[i];
        for (unsigned bit = 0; bit < 8; ++bit) {
            const uint32_t mask = (uint32_t)-(int32_t)(crc & 1U);
            crc = (crc >> 1) ^ (0xEDB88320U & mask);
        }
    }
    return ~crc;
}

static void put_u16(uint8_t *p, uint16_t value)
{
    p[0] = (uint8_t)value;
    p[1] = (uint8_t)(value >> 8);
}

static void put_u32(uint8_t *p, uint32_t value)
{
    p[0] = (uint8_t)value;
    p[1] = (uint8_t)(value >> 8);
    p[2] = (uint8_t)(value >> 16);
    p[3] = (uint8_t)(value >> 24);
}

static size_t one_name_payload(uint8_t *payload, uint16_t request_id,
                               uint32_t timestamp, const uint8_t *name,
                               size_t name_length)
{
    payload[0] = 1;
    put_u16(payload + 1, request_id);
    put_u32(payload + 3, timestamp);
    payload[7] = (uint8_t)name_length;
    memcpy(payload + 8, name, name_length);
    return 8 + name_length;
}

static bool send_batch(passport_trip_rx_t *rx, passport_location_model_t *model,
                       uint16_t revision, const uint8_t *payload, size_t payload_length,
                       bool corrupt_crc, bool *changed)
{
    uint8_t begin[10] = { 3, PASSPORT_TRACK_NAMES_BEGIN_OPCODE };
    put_u16(begin + 2, revision);
    put_u16(begin + 4, (uint16_t)payload_length);
    put_u32(begin + 6, crc32(payload, payload_length) ^ (corrupt_crc ? 1U : 0U));
    if (!passport_track_name_sync_frame(rx, model, begin, sizeof(begin), changed)) return false;

    const size_t split = payload_length / 2;
    uint8_t chunk[64] = { 3, PASSPORT_TRACK_NAMES_CHUNK_OPCODE };
    put_u16(chunk + 2, revision);
    put_u16(chunk + 4, 0);
    memcpy(chunk + 6, payload, split);
    if (!passport_track_name_sync_frame(rx, model, chunk, 6 + split, changed)) return false;
    put_u16(chunk + 4, (uint16_t)split);
    memcpy(chunk + 6, payload + split, payload_length - split);
    if (!passport_track_name_sync_frame(rx, model, chunk,
                                         6 + payload_length - split, changed)) return false;

    uint8_t commit[4] = { 3, PASSPORT_TRACK_NAMES_COMMIT_OPCODE };
    put_u16(commit + 2, revision);
    return passport_track_name_sync_frame(rx, model, commit, sizeof(commit), changed);
}

static void test_fragmented_update_and_idempotence(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);
    assert(passport_location_model_add(&model, 1800000100, 480, 77, NULL));
    passport_trip_rx_t rx = {0};
    uint8_t payload[64];
    const char *name = "上海图书馆";
    const size_t length = one_name_payload(payload, 77, 1800000100,
                                           (const uint8_t *)name, strlen(name));
    bool changed = false;
    assert(send_batch(&rx, &model, 3, payload, length, false, &changed));
    assert(changed);
    assert(strcmp(passport_location_model_newest(&model, 0)->place_name, name) == 0);
    changed = true;
    assert(send_batch(&rx, &model, 4, payload, length, false, &changed));
    assert(!changed);
}

static void test_bad_crc_utf8_and_missing_records(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);
    assert(passport_location_model_add(&model, 1800000200, 0, 88, NULL));
    passport_trip_rx_t rx = {0};
    uint8_t payload[64];
    size_t length = one_name_payload(payload, 88, 1800000200,
                                     (const uint8_t *)"Park", 4);
    bool changed = false;
    assert(!send_batch(&rx, &model, 1, payload, length, true, &changed));
    assert((passport_location_model_newest(&model, 0)->flags &
            PASSPORT_MARKER_HAS_PLACE_NAME) == 0);

    const uint8_t invalid_utf8[] = {0xE4, 0x00, 0x80};
    length = one_name_payload(payload, 88, 1800000200, invalid_utf8,
                              sizeof(invalid_utf8));
    assert(!send_batch(&rx, &model, 2, payload, length, false, &changed));

    length = one_name_payload(payload, 99, 1800000999,
                              (const uint8_t *)"Missing", 7);
    assert(send_batch(&rx, &model, 3, payload, length, false, &changed));
    assert(!changed);
}

int main(void)
{
    test_fragmented_update_and_idempotence();
    test_bad_crc_utf8_and_missing_records();
    return 0;
}
