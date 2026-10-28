#include "passport_track_name_protocol.h"

#include <string.h>

#define PASSPORT_PROTOCOL_VERSION 3U

static uint16_t u16(const uint8_t *p)
{
    return (uint16_t)p[0] | ((uint16_t)p[1] << 8);
}
static uint32_t u32(const uint8_t *p)
{
    return (uint32_t)p[0] | ((uint32_t)p[1] << 8) |
           ((uint32_t)p[2] << 16) | ((uint32_t)p[3] << 24);
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

static bool valid_utf8_name(const uint8_t *data, size_t length)
{
    size_t i = 0;
    while (i < length) {
        const uint8_t first = data[i++];
        if (first < 0x80U) {
            if (first < 0x20U || first == 0x7FU) return false;
            continue;
        }
        size_t continuation = 0;
        uint8_t second_min = 0x80U;
        uint8_t second_max = 0xBFU;
        if (first >= 0xC2U && first <= 0xDFU) {
            continuation = 1;
        } else if (first >= 0xE0U && first <= 0xEFU) {
            continuation = 2;
            if (first == 0xE0U) second_min = 0xA0U;
            if (first == 0xEDU) second_max = 0x9FU;
        } else if (first >= 0xF0U && first <= 0xF4U) {
            continuation = 3;
            if (first == 0xF0U) second_min = 0x90U;
            if (first == 0xF4U) second_max = 0x8FU;
        } else {
            return false;
        }
        if (i + continuation > length || data[i] < second_min || data[i] > second_max)
            return false;
        i++;
        for (size_t j = 1; j < continuation; ++j, ++i) {
            if (data[i] < 0x80U || data[i] > 0xBFU) return false;
        }
    }
    return true;
}
static bool validate_payload(const uint8_t *payload, size_t length)
{
    if (length < 1 || payload[0] == 0 ||
        payload[0] > PASSPORT_TRACK_NAME_BATCH_CAPACITY) return false;
    size_t offset = 1;
    for (size_t i = 0; i < payload[0]; ++i) {
        if (offset + 7 > length) return false;
        const uint16_t request_id = u16(payload + offset);
        const uint32_t timestamp = u32(payload + offset + 2);
        const size_t name_length = payload[offset + 6];
        offset += 7;
        if (request_id == 0 || timestamp < 1577836800U || name_length == 0 ||
            name_length >= PASSPORT_PLACE_NAME_BYTES || offset + name_length > length ||
            !valid_utf8_name(payload + offset, name_length)) return false;
        offset += name_length;
    }
    return offset == length;
}

static bool apply_payload(passport_location_model_t *model,
                          const uint8_t *payload,
                          size_t length)
{
    (void)length;
    bool changed = false;
    size_t offset = 1;
    for (size_t i = 0; i < payload[0]; ++i) {
        const uint16_t request_id = u16(payload + offset);
        const uint32_t timestamp = u32(payload + offset + 2);
        const size_t name_length = payload[offset + 6];
        offset += 7;
        const passport_place_update_result_t result =
            passport_location_model_update_place(model, request_id, timestamp,
                                                  payload + offset, name_length);
        if (result == PASSPORT_PLACE_UPDATE_CHANGED) changed = true;
        offset += name_length;
    }
    return changed;
}

bool passport_track_name_sync_frame(passport_trip_rx_t *rx,
                                    passport_location_model_t *model,
                                    const uint8_t *frame,
                                    size_t length,
                                    bool *changed_out)
{
    if (changed_out != NULL) *changed_out = false;
    if (rx == NULL || model == NULL || frame == NULL || length < 2 ||
        frame[0] != PASSPORT_PROTOCOL_VERSION) return false;

    if (frame[1] == PASSPORT_TRACK_NAMES_BEGIN_OPCODE) {
        if (length != 10) return false;
        const uint16_t size = u16(frame + 4);
        if (size == 0 || size > PASSPORT_TRIP_SNAPSHOT_MAX_BYTES) return false;
        rx->active = true;
        rx->revision = u16(frame + 2);
        rx->length = size;
        rx->received = 0;
        rx->crc32 = u32(frame + 6);
        return true;
    }

    if (length < 4 || !rx->active || u16(frame + 2) != rx->revision) return false;
    if (frame[1] == PASSPORT_TRACK_NAMES_CHUNK_OPCODE) {
        if (length < 7) return false;
        const uint16_t offset = u16(frame + 4);
        const size_t bytes = length - 6;
        if (offset != rx->received || bytes > rx->length - rx->received) {
            rx->active = false;
            return false;
        }
        memcpy(rx->payload + offset, frame + 6, bytes);
        rx->received = (uint16_t)(rx->received + bytes);
        return true;
    }

    if (frame[1] != PASSPORT_TRACK_NAMES_COMMIT_OPCODE || length != 4 ||
        rx->received != rx->length || crc32(rx->payload, rx->length) != rx->crc32 ||
        !validate_payload(rx->payload, rx->length)) {
        rx->active = false;
        return false;
    }
    const bool changed = apply_payload(model, rx->payload, rx->length);
    rx->active = false;
    if (changed_out != NULL) *changed_out = changed;
    return true;
}
