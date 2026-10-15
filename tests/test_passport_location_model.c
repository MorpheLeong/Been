#include <assert.h>
#include <stdint.h>

#include "passport_location_model.h"

static void test_marker_creation_and_enrichment(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);
    assert(passport_location_model_valid(&model));

    uint32_t id = 0;
    assert(passport_location_model_add(&model, 1790985600, 480, &id));
    assert(id == 1);
    assert(model.count == 1);

    const passport_marker_t *marker = passport_location_model_newest(&model, 0);
    assert(marker != NULL);
    assert(marker->timestamp_utc == 1790985600);
    assert(marker->utc_offset_minutes == 480);
    assert((marker->flags & PASSPORT_MARKER_HAS_COORDINATES) == 0);

    assert(!passport_location_model_set_coordinates(&model, id, 900000001, 0, 5));
    assert(!passport_location_model_set_coordinates(&model, id, 0, 1800000001, 5));
    assert(passport_location_model_set_coordinates(&model, id, 311230000, 1214737000, 12));
    assert(passport_location_model_set_place(&model, id, "Shanghai Library"));
    marker = passport_location_model_newest(&model, 0);
    assert(marker->latitude_e7 == 311230000);
    assert(marker->longitude_e7 == 1214737000);
    assert(marker->accuracy_meters == 12);
    assert((marker->flags & PASSPORT_MARKER_HAS_COORDINATES) != 0);
    assert((marker->flags & PASSPORT_MARKER_HAS_PLACE_NAME) != 0);
    assert(passport_location_model_valid(&model));
}

static void test_ring_retains_newest_records(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);

    for (uint32_t i = 1; i <= PASSPORT_MARKER_CAPACITY + 1; i++) {
        uint32_t id = 0;
        assert(passport_location_model_add(&model, 1800000000 + i, 480, &id));
        assert(id == i);
    }
    assert(model.count == PASSPORT_MARKER_CAPACITY);
    assert(passport_location_model_newest(&model, 0)->id == PASSPORT_MARKER_CAPACITY + 1);
    assert(passport_location_model_newest(&model, PASSPORT_MARKER_CAPACITY - 1)->id == 2);
    assert(passport_location_model_newest(&model, PASSPORT_MARKER_CAPACITY) == NULL);
    assert(!passport_location_model_set_coordinates(&model, 1, 0, 0, 1));
    assert(passport_location_model_valid(&model));
}

static void test_bad_time_timezone_and_feature_values_are_rejected(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);
    assert(!passport_location_model_add(&model, -1, 0, NULL));
    assert(!passport_location_model_add(&model, 1800000000, 841, NULL));
    assert(!passport_location_model_set_last_feature(&model, PASSPORT_FEATURE_COUNT));
    assert(passport_location_model_set_last_feature(&model, PASSPORT_FEATURE_COUNT - 1));
    assert(passport_location_model_valid(&model));

    model.last_feature = PASSPORT_FEATURE_COUNT;
    assert(!passport_location_model_valid(&model));
}

static void test_only_current_local_day_is_retained(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);

    /* UTC+8: the first two records are before local midnight, the last two after it. */
    assert(passport_location_model_add(&model, 1790956798, 480, NULL));
    assert(passport_location_model_add(&model, 1790956799, 480, NULL));
    assert(passport_location_model_add(&model, 1790956800, 480, NULL));
    assert(passport_location_model_add(&model, 1790956801, 480, NULL));
    assert(passport_location_model_retain_local_day(&model, 1790956802, 480) == 2);
    assert(model.count == 2);
    assert(passport_location_model_newest(&model, 0)->timestamp_utc == 1790956801);
    assert(passport_location_model_newest(&model, 1)->timestamp_utc == 1790956800);
    assert(passport_location_model_valid(&model));

    /* Crossing the next local midnight clears the device-side daily view. */
    assert(passport_location_model_retain_local_day(&model, 1791043200, 480) == 2);
    assert(model.count == 0);
    assert(passport_location_model_valid(&model));
}

static void test_daily_retention_uses_current_timezone(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);
    assert(passport_location_model_add(&model, 1790955000, 0, NULL));
    assert(passport_location_model_add(&model, 1790958600, 0, NULL));

    /* UTC+8, rather than the stored offset, defines the current local day. */
    assert(passport_location_model_retain_local_day(&model, 1790959000, 480) == 1);
    assert(model.count == 1);
    assert(passport_location_model_newest(&model, 0)->timestamp_utc == 1790958600);
    assert(passport_location_model_retain_local_day(&model, 0, 480) == 0);
    assert(passport_location_model_retain_local_day(&model, 1790959000, 841) == 0);
}

int main(void)
{
    test_marker_creation_and_enrichment();
    test_ring_retains_newest_records();
    test_bad_time_timezone_and_feature_values_are_rejected();
    test_only_current_local_day_is_retained();
    test_daily_retention_uses_current_timezone();
    return 0;
}
