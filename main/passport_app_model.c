#include "passport_app_model.h"

static const passport_screen_t FEATURE_SCREENS[] = {
    PASSPORT_SCREEN_MARKER,
    PASSPORT_SCREEN_MARKER,
    PASSPORT_SCREEN_QR,
};

static const passport_screen_t LONG_PRESS_SCREENS[] = {
    PASSPORT_SCREEN_HISTORY, PASSPORT_SCREEN_CHECKLIST, PASSPORT_SCREEN_QR_LIST,
};

static passport_transition_t transition(passport_app_model_t *model,
                                        passport_action_t action,
                                        passport_screen_t screen)
{
    passport_transition_t result = {
        .action = action,
        .screen = screen,
        .selected = model ? model->selected : 0,
    };
    return result;
}

void passport_app_model_init(passport_app_model_t *model,
                             size_t feature_count,
                             size_t last_feature)
{
    if (model == NULL) return;
    if (feature_count > sizeof(FEATURE_SCREENS) / sizeof(FEATURE_SCREENS[0])) {
        feature_count = sizeof(FEATURE_SCREENS) / sizeof(FEATURE_SCREENS[0]);
    }
    model->feature_count = feature_count;
    if (feature_count == 0) {
        model->last_feature = 0;
        model->selected = 0;
    } else {
        model->last_feature = last_feature < feature_count ? last_feature : 0;
        model->selected = model->last_feature + 1;
    }
    model->screen = PASSPORT_SCREEN_HOME;
}

passport_transition_t passport_app_model_input(passport_app_model_t *model,
                                               passport_input_t input)
{
    if (model == NULL) {
        return (passport_transition_t){
            .action = PASSPORT_ACTION_NONE,
            .screen = PASSPORT_SCREEN_HOME,
            .selected = 0,
        };
    }

    if (model->screen != PASSPORT_SCREEN_HOME) {
        if (input == PASSPORT_INPUT_OK_LONG) {
            model->screen = PASSPORT_SCREEN_HOME;
            model->selected = model->feature_count == 0
                            ? 0
                            : model->last_feature + 1;
            return transition(model, PASSPORT_ACTION_BACK_HOME, model->screen);
        }
        return transition(model, PASSPORT_ACTION_FORWARD, model->screen);
    }

    const size_t item_count = model->feature_count + 1; // Avatar plus features.
    if (item_count == 0) return transition(model, PASSPORT_ACTION_NONE, model->screen);

    if (input == PASSPORT_INPUT_UP) {
        model->selected = (model->selected + item_count - 1) % item_count;
        return transition(model, PASSPORT_ACTION_REFRESH, model->screen);
    }
    if (input == PASSPORT_INPUT_DOWN) {
        model->selected = (model->selected + 1) % item_count;
        return transition(model, PASSPORT_ACTION_REFRESH, model->screen);
    }
    if ((input != PASSPORT_INPUT_OK && input != PASSPORT_INPUT_OK_LONG) ||
        model->selected > model->feature_count) {
        return transition(model, PASSPORT_ACTION_NONE, model->screen);
    }

    if (model->selected == 0) {
        model->screen = PASSPORT_SCREEN_PROFILE;
    } else {
        model->last_feature = model->selected - 1;
        model->screen = input == PASSPORT_INPUT_OK_LONG
                      ? LONG_PRESS_SCREENS[model->last_feature]
                      : FEATURE_SCREENS[model->last_feature];
    }
    return transition(model, PASSPORT_ACTION_OPEN, model->screen);
}
