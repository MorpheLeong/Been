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
                                 uint16_t source_request_id,
                                 uint32_t *id_out)
{
    if (!passport_location_model_valid(model) || timestamp_utc < 0 ||
        utc_offset_minutes < -840 || utc_offset_minutes > 840 ||
        source_request_id == 0) {
        return false;
    }

    passport_marker_t marker = {
        .id = model->next_id,
        .timestamp_utc = timestamp_utc,
        .utc_offset_minutes = utc_offset_minutes,
        .source_request_id = source_request_id,
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

passport_place_update_result_t passport_location_model_update_place(
    passport_location_model_t *model,
    uint16_t source_request_id,
    int64_t timestamp_utc,
    const uint8_t *place_name,
    size_t place_name_length)
{
    if (!passport_location_model_valid(model) || source_request_id == 0 ||
        timestamp_utc <= 0 || place_name == NULL || place_name_length == 0 ||
        place_name_length >= PASSPORT_PLACE_NAME_BYTES ||
        memchr(place_name, '\0', place_name_length) != NULL) {
        return PASSPORT_PLACE_UPDATE_INVALID;
    }

    passport_marker_t *match = NULL;
    for (size_t i = 0; i < model->count; ++i) {
        const size_t index = (model->next_write + PASSPORT_MARKER_CAPACITY - 1 - i)
                           % PASSPORT_MARKER_CAPACITY;
        passport_marker_t *marker = &model->markers[index];
        if (marker->source_request_id == source_request_id &&
            marker->timestamp_utc == timestamp_utc) {
            match = marker;
            break;
        }
    }

    if (match == NULL) {
        /* Version-1 records did not persist the request ID. Bind only an
         * unambiguous timestamp match so old history can still be enriched. */
        for (size_t i = 0; i < model->count; ++i) {
            const size_t index = (model->next_write + PASSPORT_MARKER_CAPACITY - 1 - i)
                               % PASSPORT_MARKER_CAPACITY;
            passport_marker_t *marker = &model->markers[index];
            if (marker->source_request_id == 0 && marker->timestamp_utc == timestamp_utc) {
                if (match != NULL) return PASSPORT_PLACE_UPDATE_NOT_FOUND;
                match = marker;
            }
        }
    }
    if (match == NULL) return PASSPORT_PLACE_UPDATE_NOT_FOUND;

    const bool same_name =
        (match->flags & PASSPORT_MARKER_HAS_PLACE_NAME) != 0 &&
        strlen(match->place_name) == place_name_length &&
        memcmp(match->place_name, place_name, place_name_length) == 0;
    if (same_name && match->source_request_id == source_request_id)
        return PASSPORT_PLACE_UPDATE_UNCHANGED;

    memset(match->place_name, 0, sizeof(match->place_name));
    memcpy(match->place_name, place_name, place_name_length);
    match->flags |= PASSPORT_MARKER_HAS_PLACE_NAME;
    match->source_request_id = source_request_id;
    return PASSPORT_PLACE_UPDATE_CHANGED;
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

static int64_t marker_local_day(const passport_marker_t *marker)
{
    return (marker->timestamp_utc + (int64_t)marker->utc_offset_minutes * 60) / 86400;
}

size_t passport_location_model_retain_local_days(
    passport_location_model_t *model,
    int64_t timestamp_utc,
    int16_t utc_offset_minutes,
    unsigned day_count)
{
    if (!passport_location_model_valid(model) || timestamp_utc <= 0 ||
        utc_offset_minutes < -840 || utc_offset_minutes > 840 || day_count == 0) {
        return 0;
    }

    const int64_t adjusted_now = timestamp_utc + (int64_t)utc_offset_minutes * 60;
    if (adjusted_now < 0) return 0;
    const int64_t earliest_day = adjusted_now / 86400 - (int64_t)(day_count - 1U);
    size_t keep = 0;
    while (keep < model->count) {
        const passport_marker_t *marker = passport_location_model_newest(model, keep);
        if (marker == NULL ||
            (marker->flags & PASSPORT_MARKER_HAS_TIMESTAMP) == 0) break;
        if (marker->timestamp_utc <= 0 || marker_local_day(marker) < earliest_day) break;
        keep++;
    }

    const size_t removed = model->count - keep;
    model->count = (uint16_t)keep;
    return removed;
}

size_t passport_location_model_history_page_count(
    const passport_location_model_t *model)
{
    if (!passport_location_model_valid(model)) return 0;
    size_t pages = 0;
    size_t offset = 0;
    while (offset < model->count) {
        const passport_marker_t *first = passport_location_model_newest(model, offset);
        if (first == NULL) break;
        const int64_t day = marker_local_day(first);
        size_t records = 0;
        while (offset + records < model->count) {
            const passport_marker_t *marker =
                passport_location_model_newest(model, offset + records);
            if (marker == NULL || marker_local_day(marker) != day) break;
            records++;
        }
        pages += (records + 5U) / 6U;
        offset += records;
    }
    return pages;
}

bool passport_location_model_history_page(
    const passport_location_model_t *model,
    size_t page_index,
    passport_history_page_t *page)
{
    if (!passport_location_model_valid(model) || page == NULL) return false;
    size_t global_page = 0;
    size_t offset = 0;
    while (offset < model->count) {
        const passport_marker_t *first = passport_location_model_newest(model, offset);
        if (first == NULL) return false;
        const int64_t day = marker_local_day(first);
        size_t records = 0;
        while (offset + records < model->count) {
            const passport_marker_t *marker =
                passport_location_model_newest(model, offset + records);
            if (marker == NULL || marker_local_day(marker) != day) break;
            records++;
        }
        const size_t day_pages = (records + 5U) / 6U;
        if (page_index < global_page + day_pages) {
            const size_t day_page = page_index - global_page;
            const size_t first_offset = offset + day_page * 6U;
            const size_t remaining = records - day_page * 6U;
            *page = (passport_history_page_t) {
                .local_day = day,
                .first_newest_offset = first_offset,
                .record_count = remaining < 6U ? remaining : 6U,
                .page_in_day = day_page,
                .page_count_for_day = day_pages,
            };
            return true;
        }
        global_page += day_pages;
        offset += records;
    }
    return false;
}

bool passport_location_model_set_last_feature(passport_location_model_t *model,
                                              uint8_t feature_index)
{
    if (model == NULL || feature_index >= PASSPORT_FEATURE_COUNT) return false;
    model->last_feature = feature_index;
    return true;
}
