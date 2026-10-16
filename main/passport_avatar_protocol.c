#include "passport_avatar_protocol.h"

#include <string.h>

static uint16_t read_u16(const uint8_t *data)
{
    return (uint16_t)data[0] | ((uint16_t)data[1] << 8);
}

static uint32_t read_u32(const uint8_t *data)
{
    return (uint32_t)data[0] | ((uint32_t)data[1] << 8) |
           ((uint32_t)data[2] << 16) | ((uint32_t)data[3] << 24);
}

uint32_t passport_avatar_crc32(const uint8_t *data, size_t length)
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

bool passport_avatar_sync_frame(passport_avatar_rx_t *rx,
                                const uint8_t *frame, size_t length,
                                passport_avatar_action_t *action)
{
    if (rx == NULL || frame == NULL || action == NULL || length < 2 ||
        frame[0] != PASSPORT_AVATAR_PROTOCOL_VERSION) return false;
    *action = PASSPORT_AVATAR_ACTION_NONE;
    switch (frame[1]) {
    case PASSPORT_AVATAR_BEGIN_OPCODE:
        memset(rx, 0, sizeof(*rx));
        if (length != 11 || frame[2] != PASSPORT_AVATAR_FORMAT_RGB565_LE ||
            frame[3] != PASSPORT_AVATAR_WIDTH || frame[4] != PASSPORT_AVATAR_HEIGHT ||
            read_u16(frame + 5) != PASSPORT_AVATAR_BYTE_COUNT) return false;
        rx->expected_crc32 = read_u32(frame + 7);
        rx->active = true;
        return true;
    case PASSPORT_AVATAR_CHUNK_OPCODE: {
        if (!rx->active || length < 5) { rx->active = false; return false; }
        const uint16_t offset = read_u16(frame + 2);
        const size_t payload_length = length - 4;
        if (offset != rx->received || payload_length > PASSPORT_AVATAR_BYTE_COUNT - rx->received) {
            rx->active = false;
            return false;
        }
        memcpy(rx->payload + rx->received, frame + 4, payload_length);
        rx->received = (uint16_t)(rx->received + payload_length);
        return true;
    }
    case PASSPORT_AVATAR_COMMIT_OPCODE:
        if (length != 2 || !rx->active || rx->received != PASSPORT_AVATAR_BYTE_COUNT ||
            passport_avatar_crc32(rx->payload, rx->received) != rx->expected_crc32) {
            rx->active = false;
            return false;
        }
        rx->active = false;
        *action = PASSPORT_AVATAR_ACTION_COMMIT;
        return true;
    case PASSPORT_AVATAR_CLEAR_OPCODE:
        if (length != 2) return false;
        memset(rx, 0, sizeof(*rx));
        *action = PASSPORT_AVATAR_ACTION_CLEAR;
        return true;
    default:
        return false;
    }
}
