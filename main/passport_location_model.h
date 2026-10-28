#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#define PASSPORT_MARKER_CAPACITY 32
#define PASSPORT_PLACE_NAME_BYTES 48
#define PASSPORT_FEATURE_COUNT 3

enum {
    PASSPORT_MARKER_HAS_COORDINATES = 1U << 0,
    PASSPORT_MARKER_HAS_PLACE_NAME = 1U << 1,
    PASSPORT_MARKER_HAS_TIMESTAMP = 1U << 2,
};

typedef struct {
    uint32_t id;
    int64_t timestamp_utc;
    int32_t latitude_e7;
    int32_t longitude_e7;
    int16_t utc_offset_minutes;
    uint16_t accuracy_meters;
    uint8_t flags;
    char place_name[PASSPORT_PLACE_NAME_BYTES];
    /* Uses the former tail padding, keeping passport_marker_t at 80 bytes. */
    uint16_t source_request_id;
} passport_marker_t;

typedef struct {
    uint32_t next_id;
    uint16_t count;
    uint16_t next_write;
    uint8_t last_feature;
    uint8_t reserved[3];
    passport_marker_t markers[PASSPORT_MARKER_CAPACITY];
} passport_location_model_t;

typedef enum {
    PASSPORT_PLACE_UPDATE_INVALID = 0,
    PASSPORT_PLACE_UPDATE_NOT_FOUND,
    PASSPORT_PLACE_UPDATE_UNCHANGED,
    PASSPORT_PLACE_UPDATE_CHANGED,
} passport_place_update_result_t;

typedef struct {
    int64_t local_day;
    size_t first_newest_offset;
    size_t record_count;
    size_t page_in_day;
    size_t page_count_for_day;
} passport_history_page_t;

_Static_assert(sizeof(passport_marker_t) == 80,
               "passport marker RAM/NVS budget changed");
_Static_assert(sizeof(passport_location_model_t) == 2576,
               "passport location model RAM/NVS budget changed");

void passport_location_model_init(passport_location_model_t *model);
bool passport_location_model_valid(const passport_location_model_t *model);
bool passport_location_model_add(passport_location_model_t *model,
                                 int64_t timestamp_utc,
                                 int16_t utc_offset_minutes,
                                 uint16_t source_request_id,
                                 uint32_t *id_out);
bool passport_location_model_set_coordinates(passport_location_model_t *model,
                                             uint32_t id,
                                             int32_t latitude_e7,
                                             int32_t longitude_e7,
                                             uint16_t accuracy_meters);
bool passport_location_model_set_place(passport_location_model_t *model,
                                       uint32_t id,
                                       const char *place_name);
passport_place_update_result_t passport_location_model_update_place(
    passport_location_model_t *model,
    uint16_t source_request_id,
    int64_t timestamp_utc,
    const uint8_t *place_name,
    size_t place_name_length);
const passport_marker_t *passport_location_model_newest(
    const passport_location_model_t *model,
    size_t newest_offset);
size_t passport_location_model_retain_local_days(
    passport_location_model_t *model,
    int64_t timestamp_utc,
    int16_t utc_offset_minutes,
    unsigned day_count);
size_t passport_location_model_history_page_count(
    const passport_location_model_t *model);
bool passport_location_model_history_page(
    const passport_location_model_t *model,
    size_t page_index,
    passport_history_page_t *page);
bool passport_location_model_set_last_feature(passport_location_model_t *model,
                                              uint8_t feature_index);
