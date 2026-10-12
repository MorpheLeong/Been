#include <assert.h>

#include "passport_app_model.h"

static void test_default_focus_and_feature_restore(void)
{
    passport_app_model_t model;
    passport_app_model_init(&model, 2, 1);
    assert(model.screen == PASSPORT_SCREEN_HOME);
    assert(model.selected == 2);
    assert(model.last_feature == 1);

    passport_transition_t result = passport_app_model_input(&model, PASSPORT_INPUT_OK);
    assert(result.action == PASSPORT_ACTION_OPEN);
    assert(result.screen == PASSPORT_SCREEN_MARKER);
    assert(model.last_feature == 1);

    result = passport_app_model_input(&model, PASSPORT_INPUT_OK_LONG);
    assert(result.action == PASSPORT_ACTION_BACK_HOME);
    assert(model.screen == PASSPORT_SCREEN_HOME);
    assert(model.selected == 2);
}

static void test_avatar_is_a_focusable_home_item(void)
{
    passport_app_model_t model;
    passport_app_model_init(&model, 2, 0);

    passport_transition_t result = passport_app_model_input(&model, PASSPORT_INPUT_UP);
    assert(result.action == PASSPORT_ACTION_REFRESH);
    assert(model.selected == 0);

    result = passport_app_model_input(&model, PASSPORT_INPUT_OK);
    assert(result.action == PASSPORT_ACTION_OPEN);
    assert(result.screen == PASSPORT_SCREEN_PROFILE);

    result = passport_app_model_input(&model, PASSPORT_INPUT_OK_LONG);
    assert(result.action == PASSPORT_ACTION_BACK_HOME);
    assert(model.selected == 1);
    assert(model.last_feature == 0);
}

static void test_navigation_wraps_and_keeps_last_used_feature(void)
{
    passport_app_model_t model;
    passport_app_model_init(&model, 2, 1);

    passport_transition_t result = passport_app_model_input(&model, PASSPORT_INPUT_DOWN);
    assert(result.action == PASSPORT_ACTION_REFRESH);
    assert(model.selected == 0);
    result = passport_app_model_input(&model, PASSPORT_INPUT_DOWN);
    assert(model.selected == 1);
    result = passport_app_model_input(&model, PASSPORT_INPUT_DOWN);
    assert(model.selected == 2);
    result = passport_app_model_input(&model, PASSPORT_INPUT_OK);
    assert(result.screen == PASSPORT_SCREEN_MARKER);
    assert(model.last_feature == 1);

    result = passport_app_model_input(&model, PASSPORT_INPUT_OK_LONG);
    assert(result.action == PASSPORT_ACTION_BACK_HOME);
    assert(model.selected == 2);
}

int main(void)
{
    const passport_screen_t long_screens[] = {
        PASSPORT_SCREEN_HISTORY, PASSPORT_SCREEN_CHECKLIST, PASSPORT_SCREEN_QR_LIST
    };
    for (size_t i = 0; i < 3; ++i) {
        passport_app_model_t model;
        passport_app_model_init(&model, 3, i);
        assert(passport_app_model_input(&model, PASSPORT_INPUT_OK_LONG).screen == long_screens[i]);
        assert(passport_app_model_input(&model, PASSPORT_INPUT_OK_LONG).screen == PASSPORT_SCREEN_HOME);
        assert(model.selected == i + 1);
        if (i == 2)
            assert(passport_app_model_input(&model, PASSPORT_INPUT_OK).screen == PASSPORT_SCREEN_QR);
    }
    test_default_focus_and_feature_restore();
    test_avatar_is_a_focusable_home_item();
    test_navigation_wraps_and_keeps_last_used_feature();
    return 0;
}
