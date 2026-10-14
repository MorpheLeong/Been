#include <assert.h>
#include <stdint.h>
#include <string.h>

#include "passport_trip_model.h"

static void put_u16(uint8_t *out, uint16_t value)
{
    out[0] = (uint8_t)value;
    out[1] = (uint8_t)(value >> 8);
}

static void put_u32(uint8_t *out, uint32_t value)
{
    for (unsigned i = 0; i < 4; ++i) out[i] = (uint8_t)(value >> (8U * i));
}

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

static void test_large_mtu_chunk_commits_to_candidate(void)
{
    uint8_t payload[102] = { 0 };
    size_t offset = 0;
    payload[offset++] = 0; /* destinations */
    payload[offset++] = 0; /* QR codes */
    payload[offset++] = 0; /* nickname */
    payload[offset++] = 96;
    memset(&payload[offset], 'a', 96);
    offset += 96;
    payload[offset++] = 0;  /* social ids */
    payload[offset++] = 50; /* volume */
    assert(offset == sizeof(payload));

    passport_trip_rx_t rx = { 0 };
    passport_trip_model_t candidate;
    memset(&candidate, 0xA5, sizeof(candidate));
    uint8_t begin[10] = { 3, 0x10 };
    put_u16(&begin[2], 7);
    put_u16(&begin[4], sizeof(payload));
    put_u32(&begin[6], crc32(payload, sizeof(payload)));
    assert(passport_trip_sync_frame(&rx, &candidate, begin, sizeof(begin)));

    uint8_t chunk[6 + sizeof(payload)] = { 3, 0x11 };
    put_u16(&chunk[2], 7);
    put_u16(&chunk[4], 0);
    memcpy(&chunk[6], payload, sizeof(payload));
    assert(sizeof(chunk) > 20);
    assert(passport_trip_sync_frame(&rx, &candidate, chunk, sizeof(chunk)));

    const uint8_t commit[] = { 3, 0x12, 7, 0 };
    assert(passport_trip_sync_frame(&rx, &candidate, commit, sizeof(commit)));
    assert(candidate.revision == 7);
    assert(strlen(candidate.profile.bio) == 96);
    for (size_t i = 0; i < 96; ++i) assert(candidate.profile.bio[i] == 'a');
    assert(candidate.profile.volume_percent == 50);
}

int main(void)
{
    test_large_mtu_chunk_commits_to_candidate();
    return 0;
}
