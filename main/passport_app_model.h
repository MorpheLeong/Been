#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

typedef enum {
    PASSPORT_SCREEN_HOME = 0,
    PASSPORT_SCREEN_PROFILE,
    PASSPORT_SCREEN_MARKER,
    PASSPORT_SCREEN_HISTORY,
    PASSPORT_SCREEN_CHECKLIST,
    PASSPORT_SCREEN_QR,
    PASSPORT_SCREEN_QR_LIST,
} passport_screen_t;

typedef enum {
    PASSPORT_INPUT_OTHER = 0,
    PASSPORT_INPUT_UP,
    PASSPORT_INPUT_DOWN,
    PASSPORT_INPUT_OK,
    PASSPORT_INPUT_OK_LONG,
} passport_input_t;

typedef enum {
    PASSPORT_ACTION_NONE = 0,
    PASSPORT_ACTION_REFRESH,
    PASSPORT_ACTION_OPEN,
    PASSPORT_ACTION_BACK_HOME,
    PASSPORT_ACTION_FORWARD,
} passport_action_t;

typedef struct {
    passport_action_t action;
    passport_screen_t screen;
    size_t selected;
} passport_transition_t;

typedef struct {
    passport_screen_t screen;
    size_t selected;
    size_t last_feature;
    size_t feature_count;
} passport_app_model_t;

void passport_app_model_init(passport_app_model_t *model,
                             size_t feature_count,
                             size_t last_feature);
passport_transition_t passport_app_model_input(passport_app_model_t *model,
                                               passport_input_t input);
