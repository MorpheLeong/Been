#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#define PASSPORT_AVATAR_PROTOCOL_VERSION 3U
#define PASSPORT_AVATAR_BEGIN_OPCODE 0x15U
#define PASSPORT_AVATAR_CHUNK_OPCODE 0x16U
#define PASSPORT_AVATAR_COMMIT_OPCODE 0x17U
#define PASSPORT_AVATAR_CLEAR_OPCODE 0x18U
#define PASSPORT_AVATAR_FORMAT_RGB565_LE 1U
#define PASSPORT_AVATAR_WIDTH 46U
#define PASSPORT_AVATAR_HEIGHT 46U
#define PASSPORT_AVATAR_BYTE_COUNT (PASSPORT_AVATAR_WIDTH * PASSPORT_AVATAR_HEIGHT * 2U)

typedef enum {
    PASSPORT_AVATAR_ACTION_NONE,
    PASSPORT_AVATAR_ACTION_COMMIT,
    PASSPORT_AVATAR_ACTION_CLEAR,
} passport_avatar_action_t;

typedef struct {
    bool active;
    uint16_t received;
    uint32_t expected_crc32;
    uint8_t payload[PASSPORT_AVATAR_BYTE_COUNT];
} passport_avatar_rx_t;

uint32_t passport_avatar_crc32(const uint8_t *data, size_t length);
bool passport_avatar_sync_frame(passport_avatar_rx_t *rx,
                                const uint8_t *frame, size_t length,
                                passport_avatar_action_t *action);
