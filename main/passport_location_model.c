#include "passport_location_model.h"

#include <string.h>

#define PASSPORT_KNOWN_MARKER_FLAGS \
    (PASSPORT_MARKER_HAS_COORDINATES | PASSPORT_MARKER_HAS_PLACE_NAME | \
     PASSPORT_MARKER_HAS_TIMESTAMP)

static bool marker_valid(const passport_marker_t *marker)
{
    if (marker->id == 0 || marker->timestamp_utc < 0 ||
        marker->utc_offset_minutes < -840 || marker->utc_offset_minutes > 840 ||
        (marker->flags & (uint8_t)~PASSPORT_KNOWN_MARKER_FLAGS) != 0) {
        return false;
    }
    if (((marker->flags & PASSPORT_MARKER_HAS_TIMESTAMP) != 0) !=
        (marker->timestamp_utc > 0)) return false;

    if ((marker->flags & PASSPORT_MARKER_HAS_COORDINATES) != 0 &&
        (marker->latitude_e7 < -900000000 || marker->latitude_e7 > 900000000 ||
         marker->longitude_e7 < -1800000000 || marker->longitude_e7 > 1800000000)) {
        return false;
    }

    if ((marker->flags & PASSPORT_MARKER_HAS_PLACE_NAME) != 0 &&
        marker->place_name[0] == '\0') {
        return false;
    }
    if (memchr(marker->place_name, '\0', sizeof(marker->place_name)) == NULL) {
        return false;
    }
    return true;
}

void passport_location_model_init(passport_location_model_t *model)
{
    if (model == NULL) return;
    memset(model, 0, sizeof(*model));
    model->next_id = 1;
}

bool passport_location_model_valid(const passport_location_model_t *model)
{
    if (model == NULL || model->next_id == 0 ||
        model->count > PASSPORT_MARKER_CAPACITY ||
        model->next_write >= PASSPORT_MARKER_CAPACITY ||
        model->last_feature >= PASSPORT_FEATURE_COUNT) {
        return false;
    }

    for (size_t i = 0; i < model->count; i++) {
        const passport_marker_t *marker =
            passport_location_model_newest(model, i);
        if (marker == NULL || !marker_valid(marker)) return false;
    }
    return true;
}

bool passport_location_model_add(passport_location_model_t *model,
                                 int64_t timestamp_utc,
                                 int16_t utc_offset_minutes,
                                 uint32_t *id_out)
{
    if (!passport_location_model_valid(model) || timestamp_utc < 0 ||
        utc_offset_minutes < -840 || utc_offset_minutes > 840) {
        return false;
    }

    passport_marker_t marker = {
        .id = model->next_id,
        .timestamp_utc = timestamp_utc,
        .utc_offset_minutes = utc_offset_minutes,
        .flags = timestamp_utc > 0 ? PASSPORT_MARKER_HAS_TIMESTAMP : 0,
    };
    model->markers[model->next_write] = marker;
    model->count = model->count < PASSPORT_MARKER_CAPACITY
                 ? (uint16_t)(model->count + 1)
                 : model->count;
    model->next_write = (uint16_t)((model->next_write + 1) % PASSPORT_MARKER_CAPACITY);
    model->next_id++;
    if (model->next_id == 0) model->next_id = 1;
    if (id_out != NULL) *id_out = marker.id;
    return true;
}

bool passport_location_model_set_coordinates(passport_location_model_t *model,
                                             uint32_t id,
                                             int32_t latitude_e7,
                                             int32_t longitude_e7,
                                             uint16_t accuracy_meters)
{
    if (!passport_location_model_valid(model) || id == 0 ||
        latitude_e7 < -900000000 || latitude_e7 > 900000000 ||
        longitude_e7 < -1800000000 || longitude_e7 > 1800000000) {
        return false;
    }

    for (size_t i = 0; i < model->count; i++) {
        const size_t index = (model->next_write + PASSPORT_MARKER_CAPACITY - 1 - i)
                           % PASSPORT_MARKER_CAPACITY;
        passport_marker_t *marker = &model->markers[index];
        if (marker->id == id) {
            marker->latitude_e7 = latitude_e7;
            marker->longitude_e7 = longitude_e7;
            marker->accuracy_meters = accuracy_meters;
            marker->flags |= PASSPORT_MARKER_HAS_COORDINATES;
            return true;
        }
    }
    return false;
}

bool passport_location_model_set_place(passport_location_model_t *model,
                                       uint32_t id,
                                       const char *place_name)
{
    if (!passport_location_model_valid(model) || id == 0 || place_name == NULL) {
        return false;
    }
    size_t length = 0;
    while (length < PASSPORT_PLACE_NAME_BYTES && place_name[length] != '\0') length++;
    if (length == 0 || length >= PASSPORT_PLACE_NAME_BYTES) return false;

    for (size_t i = 0; i < model->count; i++) {
        const size_t index = (model->next_write + PASSPORT_MARKER_CAPACITY - 1 - i)
                           % PASSPORT_MARKER_CAPACITY;
        passport_marker_t *marker = &model->markers[index];
        if (marker->id == id) {
            memcpy(marker->place_name, place_name, length + 1);
            marker->flags |= PASSPORT_MARKER_HAS_PLACE_NAME;
            return true;
        }
    }
    return false;
}

const passport_marker_t *passport_location_model_newest(
    const passport_location_model_t *model,
    size_t newest_offset)
{
    if (model == NULL || newest_offset >= model->count ||
        model->next_write >= PASSPORT_MARKER_CAPACITY) {
        return NULL;
    }
    const size_t index = (model->next_write + PASSPORT_MARKER_CAPACITY - 1 - newest_offset)
                       % PASSPORT_MARKER_CAPACITY;
    return &model->markers[index];
}

bool passport_location_model_set_last_feature(passport_location_model_t *model,
                                              uint8_t feature_index)
{
    if (model == NULL || feature_index >= PASSPORT_FEATURE_COUNT) return false;
    model->last_feature = feature_index;
    return true;
}
