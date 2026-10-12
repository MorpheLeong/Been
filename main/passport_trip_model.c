#include "passport_trip_model.h"

#include <string.h>

#define SNAPSHOT_BEGIN 0x10U
#define SNAPSHOT_CHUNK 0x11U
#define SNAPSHOT_COMMIT 0x12U
#define PASSPORT_TRIP_PROTOCOL_VERSION 3U

static passport_trip_model_t s_staging;

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
    uint32_t value = 0xFFFFFFFFU;
    for (size_t i = 0; i < length; ++i) {
        value ^= data[i];
        for (unsigned bit = 0; bit < 8; ++bit) {
            const uint32_t mask = (uint32_t)-(int32_t)(value & 1U);
            value = (value >> 1) ^ (0xEDB88320U & mask);
        }
    }
    return ~value;
}

static bool read_string(const uint8_t *payload, size_t length, size_t *offset,
                        char *out, size_t capacity, bool allow_empty)
{
    if (*offset >= length) return false;
    const size_t n = payload[(*offset)++];
    if ((!allow_empty && n == 0) || n >= capacity || n > length - *offset)
        return false;
    if (memchr(payload + *offset, '\0', n) != NULL) return false;
    memcpy(out, payload + *offset, n);
    out[n] = '\0';
    *offset += n;
    return true;
}

void passport_trip_model_init(passport_trip_model_t *model)
{
    if (model != NULL) {
        memset(model, 0, sizeof(*model));
        model->profile.volume_percent = 50;
    }
}

bool passport_trip_model_valid(const passport_trip_model_t *model)
{
    if (model == NULL ||
        model->destination_count > PASSPORT_TRIP_DESTINATION_CAPACITY ||
        model->qr_count > PASSPORT_TRIP_QR_CAPACITY ||
        model->profile.social_count > PASSPORT_PROFILE_SOCIAL_CAPACITY ||
        model->profile.volume_percent > 100 ||
        memchr(model->profile.nickname, '\0', sizeof(model->profile.nickname)) == NULL ||
        memchr(model->profile.bio, '\0', sizeof(model->profile.bio)) == NULL) return false;
    for (size_t i = 0; i < model->profile.social_count; ++i) {
        if (model->profile.social_ids[i][0] == '\0' ||
            memchr(model->profile.social_ids[i], '\0', sizeof(model->profile.social_ids[i])) == NULL)
            return false;
    }
    unsigned stars = 0;
    for (size_t i = 0; i < model->destination_count; ++i) {
        const passport_trip_destination_t *d = &model->destinations[i];
        if (d->id[0] == '\0' || d->name[0] == '\0' ||
            memchr(d->id, '\0', sizeof(d->id)) == NULL ||
            memchr(d->name, '\0', sizeof(d->name)) == NULL) return false;
        for (size_t j = i + 1; j < model->destination_count; ++j)
            if (strncmp(d->id, model->destinations[j].id, sizeof(d->id)) == 0) return false;
    }
    for (size_t i = 0; i < model->qr_count; ++i) {
        const passport_trip_qr_t *q = &model->qrs[i];
        if (q->id[0] == '\0' || q->name[0] == '\0' || q->payload[0] == '\0' ||
            memchr(q->id, '\0', sizeof(q->id)) == NULL ||
            memchr(q->name, '\0', sizeof(q->name)) == NULL ||
            memchr(q->payload, '\0', sizeof(q->payload)) == NULL ||
            memchr(q->destination_id, '\0', sizeof(q->destination_id)) == NULL) return false;
        if (q->starred) ++stars;
        for (size_t j = i + 1; j < model->qr_count; ++j) {
            if (strncmp(q->id, model->qrs[j].id, sizeof(q->id)) == 0) return false;
            if (q->destination_id[0] != '\0' &&
                strncmp(q->destination_id, model->qrs[j].destination_id,
                        sizeof(q->destination_id)) == 0)
                return false;
        }
        if (q->destination_id[0] != '\0') {
            bool found = false;
            for (size_t j = 0; j < model->destination_count; ++j)
                found |= strcmp(q->destination_id, model->destinations[j].id) == 0;
            if (!found) return false;
        }
    }
    return stars <= 3;
}

const passport_trip_destination_t *passport_trip_current(
    const passport_trip_model_t *model)
{
    if (!passport_trip_model_valid(model)) return NULL;
    for (size_t i = 0; i < model->destination_count; ++i)
        if (!model->destinations[i].completed) return &model->destinations[i];
    return NULL;
}

const passport_trip_qr_t *passport_trip_qr_for_destination(
    const passport_trip_model_t *model, const char *destination_id)
{
    if (!passport_trip_model_valid(model) || destination_id == NULL) return NULL;
    for (size_t i = 0; i < model->qr_count; ++i)
        if (strcmp(model->qrs[i].destination_id, destination_id) == 0)
            return &model->qrs[i];
    return NULL;
}

bool passport_trip_model_complete(passport_trip_model_t *model,
                                  const char *destination_id)
{
    return passport_trip_model_set_completed(model, destination_id, true);
}

bool passport_trip_model_set_completed(passport_trip_model_t *model,
                                       const char *destination_id,
                                       bool completed)
{
    if (!passport_trip_model_valid(model) || destination_id == NULL) return false;
    for (size_t i = 0; i < model->destination_count; ++i) {
        if (strcmp(model->destinations[i].id, destination_id) == 0) {
            model->destinations[i].completed = completed;
            return true;
        }
    }
    return false;
}

static bool parse_snapshot(const uint8_t *data, size_t length,
                           passport_trip_model_t *out)
{
    size_t offset = 0;
    passport_trip_model_init(&s_staging);
    if (length < 2) return false;
    const uint8_t destination_count = data[offset++];
    if (destination_count > PASSPORT_TRIP_DESTINATION_CAPACITY) return false;
    s_staging.destination_count = destination_count;
    for (size_t i = 0; i < destination_count; ++i) {
        passport_trip_destination_t *d = &s_staging.destinations[i];
        if (!read_string(data, length, &offset, d->id, sizeof(d->id), false) ||
            !read_string(data, length, &offset, d->name, sizeof(d->name), false) ||
            offset >= length || data[offset] > 1) return false;
        d->completed = data[offset++] != 0;
    }
    if (offset >= length) return false;
    const uint8_t qr_count = data[offset++];
    if (qr_count > PASSPORT_TRIP_QR_CAPACITY) return false;
    s_staging.qr_count = qr_count;
    for (size_t i = 0; i < qr_count; ++i) {
        passport_trip_qr_t *q = &s_staging.qrs[i];
        if (!read_string(data, length, &offset, q->id, sizeof(q->id), false) ||
            !read_string(data, length, &offset, q->name, sizeof(q->name), false) ||
            !read_string(data, length, &offset, q->payload, sizeof(q->payload), false) ||
            offset >= length || data[offset] > 1) return false;
        q->starred = data[offset++] != 0;
        if (!read_string(data, length, &offset, q->destination_id,
                         sizeof(q->destination_id), true)) return false;
    }
    if (!read_string(data, length, &offset, s_staging.profile.nickname,
                     sizeof(s_staging.profile.nickname), true) ||
        !read_string(data, length, &offset, s_staging.profile.bio,
                     sizeof(s_staging.profile.bio), true) || offset >= length)
        return false;
    s_staging.profile.social_count = data[offset++];
    if (s_staging.profile.social_count > PASSPORT_PROFILE_SOCIAL_CAPACITY) return false;
    for (size_t i = 0; i < s_staging.profile.social_count; ++i) {
        if (!read_string(data, length, &offset, s_staging.profile.social_ids[i],
                         sizeof(s_staging.profile.social_ids[i]), false)) return false;
    }
    if (offset >= length || data[offset] > 100) return false;
    s_staging.profile.volume_percent = data[offset++];
    if (offset != length || !passport_trip_model_valid(&s_staging)) return false;
    *out = s_staging;
    return true;
}

bool passport_trip_sync_frame(passport_trip_rx_t *rx,
                              passport_trip_model_t *model,
                              const uint8_t *frame, size_t length)
{
    if (rx == NULL || model == NULL || frame == NULL || length < 2 ||
        frame[0] != PASSPORT_TRIP_PROTOCOL_VERSION) return false;
    if (frame[1] == SNAPSHOT_BEGIN) {
        if (length != 10) return false;
        const uint16_t size = u16(&frame[4]);
        if (size == 0 || size > PASSPORT_TRIP_SNAPSHOT_MAX_BYTES) return false;
        rx->active = true;
        rx->revision = u16(&frame[2]);
        rx->length = size;
        rx->received = 0;
        rx->crc32 = u32(&frame[6]);
        return true;
    }
    if (!rx->active || u16(&frame[2]) != rx->revision) return false;
    if (frame[1] == SNAPSHOT_CHUNK) {
        if (length < 7) return false;
        const uint16_t offset = u16(&frame[4]);
        const size_t bytes = length - 6;
        if (offset != rx->received || bytes > rx->length - rx->received) {
            rx->active = false;
            return false;
        }
        memcpy(rx->payload + offset, frame + 6, bytes);
        rx->received += bytes;
        return true;
    }
    if (frame[1] != SNAPSHOT_COMMIT || length != 4 ||
        rx->received != rx->length || crc32(rx->payload, rx->length) != rx->crc32) {
        rx->active = false;
        return false;
    }
    if (!parse_snapshot(rx->payload, rx->length, &s_staging)) {
        rx->active = false;
        return false;
    }
    s_staging.revision = rx->revision;
    *model = s_staging;
    rx->active = false;
    return true;
}
