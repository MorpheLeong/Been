#include "passport_avatar_protocol.h"

#include <assert.h>
#include <stdio.h>
#include <string.h>

static void write_u16(uint8_t *data, uint16_t value)
{
    data[0] = (uint8_t)value;
    data[1] = (uint8_t)(value >> 8);
}

static void write_u32(uint8_t *data, uint32_t value)
{
    data[0] = (uint8_t)value;
    data[1] = (uint8_t)(value >> 8);
    data[2] = (uint8_t)(value >> 16);
    data[3] = (uint8_t)(value >> 24);
}

int main(void)
{
    uint8_t pixels[PASSPORT_AVATAR_BYTE_COUNT];
    for (size_t i = 0; i < sizeof(pixels); ++i) pixels[i] = (uint8_t)(i * 13U);
    const uint32_t crc = passport_avatar_crc32(pixels, sizeof(pixels));
    uint8_t begin[11] = {
        PASSPORT_AVATAR_PROTOCOL_VERSION, PASSPORT_AVATAR_BEGIN_OPCODE,
        PASSPORT_AVATAR_FORMAT_RGB565_LE, PASSPORT_AVATAR_WIDTH, PASSPORT_AVATAR_HEIGHT,
    };
    write_u16(begin + 5, PASSPORT_AVATAR_BYTE_COUNT);
    write_u32(begin + 7, crc);

    passport_avatar_rx_t rx = { 0 };
    passport_avatar_action_t action;
    assert(passport_avatar_sync_frame(&rx, begin, sizeof(begin), &action));
    assert(rx.active && action == PASSPORT_AVATAR_ACTION_NONE);
    size_t offset = 0;
    while (offset < sizeof(pixels)) {
        const size_t part = sizeof(pixels) - offset < 31 ? sizeof(pixels) - offset : 31;
        uint8_t frame[4 + 31] = {
            PASSPORT_AVATAR_PROTOCOL_VERSION, PASSPORT_AVATAR_CHUNK_OPCODE,
        };
        write_u16(frame + 2, (uint16_t)offset);
        memcpy(frame + 4, pixels + offset, part);
        assert(passport_avatar_sync_frame(&rx, frame, 4 + part, &action));
        offset += part;
    }
    const uint8_t commit[] = { PASSPORT_AVATAR_PROTOCOL_VERSION, PASSPORT_AVATAR_COMMIT_OPCODE };
    assert(passport_avatar_sync_frame(&rx, commit, sizeof(commit), &action));
    assert(action == PASSPORT_AVATAR_ACTION_COMMIT);
    assert(memcmp(rx.payload, pixels, sizeof(pixels)) == 0);

    begin[7] ^= 1U;
    assert(passport_avatar_sync_frame(&rx, begin, sizeof(begin), &action));
    uint8_t one_chunk[4 + PASSPORT_AVATAR_BYTE_COUNT];
    one_chunk[0] = PASSPORT_AVATAR_PROTOCOL_VERSION;
    one_chunk[1] = PASSPORT_AVATAR_CHUNK_OPCODE;
    write_u16(one_chunk + 2, 0);
    memcpy(one_chunk + 4, pixels, sizeof(pixels));
    assert(passport_avatar_sync_frame(&rx, one_chunk, sizeof(one_chunk), &action));
    assert(!passport_avatar_sync_frame(&rx, commit, sizeof(commit), &action));

    const uint8_t clear[] = { PASSPORT_AVATAR_PROTOCOL_VERSION, PASSPORT_AVATAR_CLEAR_OPCODE };
    assert(passport_avatar_sync_frame(&rx, clear, sizeof(clear), &action));
    assert(action == PASSPORT_AVATAR_ACTION_CLEAR && !rx.active);
    puts("passport avatar protocol tests passed");
    return 0;
}
