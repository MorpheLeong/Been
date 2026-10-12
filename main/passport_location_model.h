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
} passport_marker_t;

typedef struct {
    uint32_t next_id;
    uint16_t count;
    uint16_t next_write;
    uint8_t last_feature;
    uint8_t reserved[3];
    passport_marker_t markers[PASSPORT_MARKER_CAPACITY];
} passport_location_model_t;

void passport_location_model_init(passport_location_model_t *model);
bool passport_location_model_valid(const passport_location_model_t *model);
bool passport_location_model_add(passport_location_model_t *model,
                                 int64_t timestamp_utc,
                                 int16_t utc_offset_minutes,
                                 uint32_t *id_out);
bool passport_location_model_set_coordinates(passport_location_model_t *model,
                                             uint32_t id,
                                             int32_t latitude_e7,
                                             int32_t longitude_e7,
                                             uint16_t accuracy_meters);
bool passport_location_model_set_place(passport_location_model_t *model,
                                       uint32_t id,
                                       const char *place_name);
const passport_marker_t *passport_location_model_newest(
    const passport_location_model_t *model,
    size_t newest_offset);
bool passport_location_model_set_last_feature(passport_location_model_t *model,
                                              uint8_t feature_index);
