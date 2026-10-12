#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#define PASSPORT_TRIP_DESTINATION_CAPACITY 64U
#define PASSPORT_TRIP_QR_CAPACITY 10U
#define PASSPORT_TRIP_ID_BYTES 37U
#define PASSPORT_TRIP_NAME_BYTES 49U
#define PASSPORT_TRIP_PAYLOAD_BYTES 193U
#define PASSPORT_TRIP_SNAPSHOT_MAX_BYTES 12000U
#define PASSPORT_PROFILE_NICKNAME_BYTES 49U
#define PASSPORT_PROFILE_BIO_BYTES 97U
#define PASSPORT_PROFILE_SOCIAL_CAPACITY 4U
#define PASSPORT_PROFILE_SOCIAL_BYTES 49U

typedef struct {
    char id[PASSPORT_TRIP_ID_BYTES];
    char name[PASSPORT_TRIP_NAME_BYTES];
    bool completed;
} passport_trip_destination_t;

typedef struct {
    char id[PASSPORT_TRIP_ID_BYTES];
    char name[PASSPORT_TRIP_NAME_BYTES];
    char payload[PASSPORT_TRIP_PAYLOAD_BYTES];
    char destination_id[PASSPORT_TRIP_ID_BYTES];
    bool starred;
} passport_trip_qr_t;

typedef struct {
    char nickname[PASSPORT_PROFILE_NICKNAME_BYTES];
    char bio[PASSPORT_PROFILE_BIO_BYTES];
    uint8_t social_count;
    char social_ids[PASSPORT_PROFILE_SOCIAL_CAPACITY][PASSPORT_PROFILE_SOCIAL_BYTES];
    uint8_t volume_percent;
} passport_profile_t;

typedef struct {
    uint16_t revision;
    uint8_t destination_count;
    uint8_t qr_count;
    passport_trip_destination_t destinations[PASSPORT_TRIP_DESTINATION_CAPACITY];
    passport_trip_qr_t qrs[PASSPORT_TRIP_QR_CAPACITY];
    passport_profile_t profile;
} passport_trip_model_t;

typedef struct {
    bool active;
    uint16_t revision;
    uint16_t length;
    uint16_t received;
    uint32_t crc32;
    uint8_t payload[PASSPORT_TRIP_SNAPSHOT_MAX_BYTES];
} passport_trip_rx_t;

void passport_trip_model_init(passport_trip_model_t *model);
bool passport_trip_model_valid(const passport_trip_model_t *model);
const passport_trip_destination_t *passport_trip_current(
    const passport_trip_model_t *model);
const passport_trip_qr_t *passport_trip_qr_for_destination(
    const passport_trip_model_t *model, const char *destination_id);
bool passport_trip_model_complete(passport_trip_model_t *model,
                                  const char *destination_id);
bool passport_trip_model_set_completed(passport_trip_model_t *model,
                                       const char *destination_id,
                                       bool completed);
bool passport_trip_sync_frame(passport_trip_rx_t *rx,
                              passport_trip_model_t *model,
                              const uint8_t *frame, size_t length);
