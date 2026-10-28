#include <assert.h>
#include <stdint.h>

#include "passport_location_model.h"

static void test_memory_budget_is_unchanged(void)
{
    assert(sizeof(passport_marker_t) == 80);
    assert(sizeof(passport_location_model_t) == 2576);
}

static void test_marker_creation_and_enrichment(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);
    assert(passport_location_model_valid(&model));

    uint32_t id = 0;
    assert(passport_location_model_add(&model, 1790985600, 480, 42, &id));
    assert(id == 1);
    assert(model.count == 1);

    const passport_marker_t *marker = passport_location_model_newest(&model, 0);
    assert(marker != NULL);
    assert(marker->timestamp_utc == 1790985600);
    assert(marker->utc_offset_minutes == 480);
    assert(marker->source_request_id == 42);
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
        assert(passport_location_model_add(&model, 1800000000 + i, 480,
                                           (uint16_t)i, &id));
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
    assert(!passport_location_model_add(&model, -1, 0, 1, NULL));
    assert(!passport_location_model_add(&model, 1800000000, 841, 1, NULL));
    assert(!passport_location_model_add(&model, 1800000000, 0, 0, NULL));
    assert(!passport_location_model_set_last_feature(&model, PASSPORT_FEATURE_COUNT));
    assert(passport_location_model_set_last_feature(&model, PASSPORT_FEATURE_COUNT - 1));
    assert(passport_location_model_valid(&model));

    model.last_feature = PASSPORT_FEATURE_COUNT;
    assert(!passport_location_model_valid(&model));
}

static void test_recent_three_local_days_are_retained(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);

    const int64_t today = 20000LL * 86400;
    assert(passport_location_model_add(&model, today - 3 * 86400 + 100, 0, 1, NULL));
    assert(passport_location_model_add(&model, today - 2 * 86400 + 100, 0, 2, NULL));
    assert(passport_location_model_add(&model, today - 1 * 86400 + 100, 0, 3, NULL));
    assert(passport_location_model_add(&model, today + 100, 0, 4, NULL));
    assert(passport_location_model_retain_local_days(&model, today + 200, 0, 3) == 1);
    assert(model.count == 3);
    assert(passport_location_model_newest(&model, 0)->source_request_id == 4);
    assert(passport_location_model_newest(&model, 2)->source_request_id == 2);
    assert(passport_location_model_valid(&model));
}

static void test_retention_uses_each_record_local_date(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);
    const int64_t today = 20000LL * 86400;
    assert(passport_location_model_add(&model, today - 3 * 86400 + 23 * 3600,
                                       480, 1, NULL));
    assert(passport_location_model_retain_local_days(&model, today + 3600, 0, 3) == 0);
    assert(model.count == 1); /* UTC+8 moves the record into the three-day window. */
    assert(passport_location_model_retain_local_days(&model, 0, 480, 3) == 0);
    assert(passport_location_model_retain_local_days(&model, today, 841, 3) == 0);
}

static void test_place_updates_are_idempotent_and_bind_legacy_records(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);
    assert(passport_location_model_add(&model, 1800000100, 0, 77, NULL));
    assert(passport_location_model_update_place(
        &model, 77, 1800000100, (const uint8_t *)"Shanghai", 8) ==
        PASSPORT_PLACE_UPDATE_CHANGED);
    assert(passport_location_model_update_place(
        &model, 77, 1800000100, (const uint8_t *)"Shanghai", 8) ==
        PASSPORT_PLACE_UPDATE_UNCHANGED);
    assert(passport_location_model_update_place(
        &model, 78, 1800000100, (const uint8_t *)"Other", 5) ==
        PASSPORT_PLACE_UPDATE_NOT_FOUND);

    passport_marker_t *legacy = (passport_marker_t *)passport_location_model_newest(&model, 0);
    legacy->source_request_id = 0;
    assert(passport_location_model_update_place(
        &model, 99, 1800000100, (const uint8_t *)"Legacy", 6) ==
        PASSPORT_PLACE_UPDATE_CHANGED);
    assert(legacy->source_request_id == 99);
}

static void test_history_pages_repeat_date_and_hold_six_records(void)
{
    passport_location_model_t model;
    passport_location_model_init(&model);
    const int64_t day = 20000LL * 86400;
    for (uint16_t i = 1; i <= 2; ++i)
        assert(passport_location_model_add(&model, day - 86400 + i, 0, i, NULL));
    for (uint16_t i = 3; i <= 9; ++i)
        assert(passport_location_model_add(&model, day + i, 0, i, NULL));
    assert(passport_location_model_history_page_count(&model) == 3);
    passport_history_page_t page;
    assert(passport_location_model_history_page(&model, 0, &page));
    assert(page.record_count == 6 && page.page_in_day == 0 && page.page_count_for_day == 2);
    assert(passport_location_model_history_page(&model, 1, &page));
    assert(page.record_count == 1 && page.page_in_day == 1);
    assert(passport_location_model_history_page(&model, 2, &page));
    assert(page.record_count == 2 && page.page_count_for_day == 1);
    assert(!passport_location_model_history_page(&model, 3, &page));
}

int main(void)
{
    test_memory_budget_is_unchanged();
    test_marker_creation_and_enrichment();
    test_ring_retains_newest_records();
    test_bad_time_timezone_and_feature_values_are_rejected();
    test_recent_three_local_days_are_retained();
    test_retention_uses_each_record_local_date();
    test_place_updates_are_idempotent_and_bind_legacy_records();
    test_history_pages_repeat_date_and_hold_six_records();
    return 0;
}
