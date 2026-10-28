#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#include "passport_location_model.h"
#include "passport_trip_model.h"

#define PASSPORT_TRACK_NAMES_BEGIN_OPCODE 0x19U
#define PASSPORT_TRACK_NAMES_CHUNK_OPCODE 0x1AU
#define PASSPORT_TRACK_NAMES_COMMIT_OPCODE 0x1BU
#define PASSPORT_TRACK_NAME_BATCH_CAPACITY PASSPORT_MARKER_CAPACITY

/* Reuses the existing trip snapshot receiver. Track-name and trip snapshot
 * transactions must be serialized on the BLE write queue. */
bool passport_track_name_sync_frame(passport_trip_rx_t *rx,
                                    passport_location_model_t *model,
                                    const uint8_t *frame,
                                    size_t length,
                                    bool *changed_out);
