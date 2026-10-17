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

/* 结果页必须保留发起时的操作类型和返回目标，不能依赖请求完成后会被清空的
 * “pending”标志，否则成功页短按会退化成新的普通轨迹请求。 */
typedef enum {
    PASSPORT_MARKER_TRACK = 0,
    PASSPORT_MARKER_CHECKIN,
    PASSPORT_MARKER_UNCHECKIN,
} passport_marker_operation_t;

typedef enum {
    PASSPORT_MARKER_IDLE = 0,
    PASSPORT_MARKER_WAITING,
    PASSPORT_MARKER_SUCCESS,
    PASSPORT_MARKER_FAILURE,
} passport_marker_phase_t;

typedef struct {
    passport_marker_operation_t operation;
    passport_marker_phase_t phase;
    passport_screen_t return_screen;
} passport_marker_flow_t;

void passport_app_model_init(passport_app_model_t *model,
                             size_t feature_count,
                             size_t last_feature);
passport_transition_t passport_app_model_input(passport_app_model_t *model,
                                               passport_input_t input);
void passport_marker_flow_begin(passport_marker_flow_t *flow,
                                passport_marker_operation_t operation,
                                passport_screen_t return_screen);
bool passport_marker_flow_accepts_short_ok(const passport_marker_flow_t *flow);
bool passport_marker_flow_records_location(const passport_marker_flow_t *flow);
bool passport_marker_flow_is_destination(const passport_marker_flow_t *flow);
