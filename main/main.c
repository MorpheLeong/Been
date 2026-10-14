#include "bsp_battery.h"
#include "bsp_audio.h"
#include "bsp_button.h"
#include "bsp_display.h"
#include "bsp_i2c.h"
#include "bsp_pins.h"
#include "esp_heap_caps.h"
#include "esp_log.h"
#include "esp_random.h"
#include "esp_timer.h"
#include "freertos/FreeRTOS.h"
#include "freertos/queue.h"
#include "freertos/task.h"
#include "lvgl.h"
#include "passport_app_model.h"
#include "passport_ble_link.h"
#include "passport_display_policy.h"
#include "passport_location_model.h"
#include "passport_store.h"
#include "passport_trip_model.h"
#include "passport_trip_store.h"
#include "qrcode.h"

#include <stdio.h>
#include <string.h>
#include <sys/time.h>
#include <time.h>

#define INPUT_QUEUE_DEPTH 24
#define SOUND_QUEUE_DEPTH 4
#define C_BG 0x050B0A
#define C_PANEL 0x101C18
#define C_LINE 0x29483A
#define C_GREEN 0x61FF9B
#define C_DIM 0x72917F
#define C_WHITE 0xD9F7E5
#define C_AMBER 0xFFD166

LV_FONT_DECLARE(lv_font_source_han_sans_sc_16_cjk);
LV_FONT_DECLARE(lv_font_source_han_sans_sc_14_cjk);
LV_FONT_DECLARE(passport_font_14);
LV_FONT_DECLARE(passport_font_20);

typedef enum { APP_EVENT_BUTTON, APP_EVENT_BLE, APP_EVENT_TIMEOUT } app_event_type_t;
typedef struct {
    app_event_type_t type;
    union {
        struct { bsp_btn_t btn; bsp_btn_ev_t event; } button;
        struct {
            passport_ble_event_t event;
            passport_ble_result_t result;
            uint32_t pairing_code;
        } ble;
        uint16_t request_id;
    } data;
} app_event_t;

typedef enum {
    MARKER_IDLE,
    MARKER_WAITING,
    MARKER_SUCCESS,
    MARKER_FAILURE,
} marker_state_t;

static const char *TAG = "passport_app";
static QueueHandle_t s_queue;
static QueueHandle_t s_sound_queue;
static volatile bool s_input_ready;
static bool s_battery_ready;
static passport_app_model_t s_app;
static passport_location_model_t s_records;
static passport_trip_model_t s_trip;
static lv_obj_t *s_screen;
static lv_obj_t *s_home_items[4];
static lv_obj_t *s_marker_state_label;
static lv_obj_t *s_marker_card;
static lv_obj_t *s_clock_text;
static lv_obj_t *s_battery_text;
static lv_obj_t *s_battery_fill;
static lv_obj_t *s_phone_icon;
static lv_obj_t *s_phone_dot;
static lv_timer_t *s_clock_timer;
static size_t s_history_page;
static size_t s_trip_page;
static size_t s_qr_selection;
static bool s_qr_opened_from_list;
/* 息屏仅关闭背光；输入任务独占策略状态，BLE 和定时器继续运行。 */
static passport_display_policy_t s_display_policy;
static bool s_consume_wake_gesture;
static bsp_btn_t s_wake_button;
static volatile bool s_ble_connected;
static volatile marker_state_t s_marker_state;
static volatile uint16_t s_active_request_id;
static volatile uint16_t s_timeout_request_id;
static volatile bool s_marker_timeout_pending;
static bool s_pending_checkin;
static bool s_pairing_visible;
static bool s_pending_uncheckin;
static bool s_last_action_uncheckin;
static bool s_last_action_destination;
static passport_screen_t s_return_screen = PASSPORT_SCREEN_HOME;
static char s_active_destination_id[PASSPORT_TRIP_ID_BYTES];
static char s_active_destination_name[PASSPORT_TRIP_NAME_BYTES];
static volatile int16_t s_local_utc_offset_minutes;
static uint16_t s_next_request_id = 1;
static size_t s_trip_selection;
/* 每个描述符覆盖各自层级的 fallback，避免改写 LVGL 只读内置字体。 */
static lv_font_t s_app_font_14;
static lv_font_t s_app_font_20;
static lv_font_t s_gb2312_font_14;
static lv_font_t s_gb2312_font_20;
static lv_font_t s_cjk_fallback_14;
static lv_font_t s_cjk_fallback_20;
static esp_timer_handle_t s_marker_timeout;
static const char *s_marker_note = "请先连接手机";
static void base_screen(const char *title);
static void show_screen(passport_screen_t screen);
/* The image descriptor is RGB565. lv_color_t is RGB888-sized in LVGL 9 and
 * would waste 16.9 KB while also giving the decoder the wrong pixel layout. */
static uint16_t s_qr_pixels[130 * 130];
static lv_image_dsc_t s_qr_image = {
    .header = {
        .magic = LV_IMAGE_HEADER_MAGIC,
        .cf = LV_COLOR_FORMAT_RGB565,
        .flags = 0,
        .w = 130,
        .h = 130,
        .stride = 130 * sizeof(uint16_t),
        .reserved_2 = 0,
    },
    .data_size = sizeof(s_qr_pixels),
    .data = (const uint8_t *)s_qr_pixels,
};
static bool s_qr_ready;

typedef enum { SOUND_KEY = 1, SOUND_OK, SOUND_ERROR } sound_cue_t;

static void log_memory(const char *stage)
{
    const uint32_t caps = MALLOC_CAP_INTERNAL | MALLOC_CAP_8BIT;
    ESP_LOGI(TAG, "memory %s: free=%u minimum=%u largest=%u stack_free=%u",
             stage,
             (unsigned)heap_caps_get_free_size(caps),
             (unsigned)heap_caps_get_minimum_free_size(caps),
             (unsigned)heap_caps_get_largest_free_block(caps),
             (unsigned)uxTaskGetStackHighWaterMark(NULL));
}

static void sound_task(void *arg)
{
    (void)arg;
    // Keep the 1.28 KB cue buffer out of this task's limited stack.
    static int16_t pcm[640];
    bool audio_ready = false;
    sound_cue_t cue;
    for (;;) {
        if (xQueueReceive(s_sound_queue, &cue, portMAX_DELAY) != pdTRUE) continue;
        if (!audio_ready) {
            if (bsp_audio_init() != ESP_OK || bsp_audio_set_format(16000, 16, 1) != ESP_OK) {
                ESP_LOGW(TAG, "提示音设备初始化失败");
                log_memory("audio-init-failed");
                continue;
            }
            audio_ready = true;
            log_memory("audio-ready");
        }
        bsp_audio_set_volume(s_trip.profile.volume_percent);
        const uint32_t frequency = cue == SOUND_ERROR ? 220U : (cue == SOUND_OK ? 880U : 660U);
        uint32_t phase = 0;
        const uint32_t increment = (uint32_t)(((uint64_t)frequency << 32) / 16000U);
        for (size_t i = 0; i < 640; ++i) {
            const int16_t amp = (i < 64 || i > 575) ? 0 : 1500;
            pcm[i] = (phase & 0x80000000U) == 0 ? amp : (int16_t)-amp;
            phase += increment;
        }
        (void)bsp_audio_write(pcm, sizeof(pcm));
    }
}

static void play_sound(sound_cue_t cue)
{
    if (s_sound_queue != NULL && s_trip.profile.volume_percent > 0)
        (void)xQueueSend(s_sound_queue, &cue, 0);
}

static void app_fonts_init(void)
{
    /* 先用完整常用简体字库；超出 GB2312 的字再查 LVGL 内置 CJK 子集。
     * 各层最后回退到对应字号的 Montserrat，避免字体 fallback 环路。 */
    s_cjk_fallback_14 = lv_font_source_han_sans_sc_14_cjk;
    s_cjk_fallback_14.fallback = &lv_font_montserrat_14;
    s_cjk_fallback_20 = lv_font_source_han_sans_sc_16_cjk;
    s_cjk_fallback_20.fallback = &lv_font_montserrat_20;

    s_gb2312_font_14 = passport_font_14;
    s_gb2312_font_14.fallback = &s_cjk_fallback_14;
    s_gb2312_font_20 = passport_font_20;
    s_gb2312_font_20.fallback = &s_cjk_fallback_20;

    s_app_font_14 = lv_font_montserrat_14;
    s_app_font_14.fallback = &s_gb2312_font_14;
    s_app_font_14.line_height = s_gb2312_font_14.line_height;
    s_app_font_14.base_line = s_gb2312_font_14.base_line;
    s_app_font_20 = lv_font_montserrat_20;
    s_app_font_20.fallback = &s_gb2312_font_20;
    s_app_font_20.line_height = s_gb2312_font_20.line_height;
    s_app_font_20.base_line = s_gb2312_font_20.base_line;
}

static void refresh_header(lv_timer_t *timer)
{
    (void)timer;
    if (s_clock_text == NULL) return;
    const time_t now = time(NULL);
    if (now < 1577836800) {
        lv_label_set_text(s_clock_text, "--:--:--");
    } else {
        const time_t local_now = now + (time_t)s_local_utc_offset_minutes * 60;
        struct tm local_time;
        if (gmtime_r(&local_now, &local_time) != NULL) {
            char clock[16];
            (void)snprintf(clock, sizeof(clock), "%02d:%02d:%02d", local_time.tm_hour, local_time.tm_min, local_time.tm_sec);
            lv_label_set_text(s_clock_text, clock);
        }
    }
    if (s_battery_text != NULL) {
        char battery[16] = "--%";
        int soc = -1;
        if (s_battery_ready) {
            soc = bsp_battery_soc();
            if (soc > 100) soc = 100;
            if (soc >= 0) (void)snprintf(battery, sizeof(battery), "%d%%", soc);
        }
        lv_label_set_text(s_battery_text, battery);
        if (s_battery_fill != NULL) {
            if (soc <= 0) lv_obj_set_hidden(s_battery_fill, true);
            else {
                lv_obj_set_hidden(s_battery_fill, false);
                lv_obj_set_width(s_battery_fill, (soc * 14 + 99) / 100);
            }
        }
    }
    if (s_phone_icon != NULL) {
        lv_obj_set_style_border_color(s_phone_icon,
            lv_color_hex(s_ble_connected ? C_GREEN : C_DIM), 0);
        lv_obj_set_style_bg_color(s_phone_dot,
            lv_color_hex(s_ble_connected ? C_GREEN : C_AMBER), 0);
    }
}

static lv_obj_t *label(lv_obj_t *parent, const char *text, int x, int y,
                       int width, int height, uint32_t color, const lv_font_t *font)
{
    /* 基线与中文字库对齐，并给中文字形保留完整行高；在 LVGL 锁内调用。 */
    const lv_font_t *app_font = font == &lv_font_montserrat_20
                              ? &s_app_font_20 : &s_app_font_14;
    lv_obj_t *obj = lv_label_create(parent);
    lv_label_set_text(obj, text);
    lv_obj_set_pos(obj, x, y);
    lv_obj_set_size(obj, width, height < app_font->line_height
                               ? app_font->line_height : height);
    lv_label_set_long_mode(obj, LV_LABEL_LONG_DOT);
    lv_obj_set_style_text_color(obj, lv_color_hex(color), 0);
    lv_obj_set_style_text_font(obj, app_font, 0);
    return obj;
}

static lv_obj_t *panel(lv_obj_t *parent, int x, int y, int w, int h)
{
    lv_obj_t *obj = lv_obj_create(parent);
    lv_obj_set_pos(obj, x, y);
    lv_obj_set_size(obj, w, h);
    lv_obj_set_style_bg_color(obj, lv_color_hex(C_PANEL), 0);
    lv_obj_set_style_bg_opa(obj, LV_OPA_COVER, 0);
    lv_obj_set_style_border_color(obj, lv_color_hex(C_LINE), 0);
    lv_obj_set_style_border_width(obj, 1, 0);
    lv_obj_set_style_radius(obj, 0, 0);
    lv_obj_set_style_pad_all(obj, 0, 0);
    lv_obj_clear_flag(obj, LV_OBJ_FLAG_SCROLLABLE);
    return obj;
}

static void finish_screen(void)
{
    lv_screen_load(s_screen);
}

static void draw_pairing_code(uint32_t pairing_code)
{
    char code[7];
    (void)snprintf(code, sizeof(code), "%06lu", (unsigned long)pairing_code);
    base_screen("安全配对");
    lv_obj_t *card = panel(s_screen, 16, 66, 208, 168);
    label(card, "安全配对", 20, 18, 168, 24, C_GREEN, &passport_font_20);
    label(card, "请在手机输入此配对码", 20, 52, 168, 20, C_WHITE, &passport_font_14);
    label(card, code, 26, 84, 156, 34, C_AMBER, &lv_font_montserrat_20);
    label(card, "仅接受与你手机相同的号码", 14, 128, 180, 20, C_DIM, &passport_font_14);
    finish_screen();
    s_pairing_visible = true;
}

static void dismiss_pairing_screen(void)
{
    if (!s_pairing_visible) return;
    s_pairing_visible = false;
    if (bsp_lvgl_lock(500)) {
        show_screen(s_app.screen);
        bsp_lvgl_unlock();
    }
}

static void base_screen(const char *title)
{
    (void)title; /* 所有页面统一使用状态顶栏，不再显示品牌或页面名。 */
    s_marker_state_label = NULL;
    s_marker_card = NULL;
    s_clock_text = NULL;
    s_battery_text = NULL;
    s_battery_fill = NULL;
    s_phone_icon = NULL;
    s_phone_dot = NULL;
    if (s_screen == NULL) s_screen = lv_obj_create(NULL);
    else lv_obj_clean(s_screen);
    memset(s_home_items, 0, sizeof(s_home_items));
    lv_obj_set_style_bg_color(s_screen, lv_color_hex(C_BG), 0);
    lv_obj_set_style_bg_opa(s_screen, LV_OPA_COVER, 0);
    lv_obj_set_style_pad_all(s_screen, 0, 0);
    lv_obj_clear_flag(s_screen, LV_OBJ_FLAG_SCROLLABLE);

    lv_obj_t *bar = panel(s_screen, 8, 8, 224, 28);
    s_clock_text = label(bar, "--:--:--", 8, 5, 98, 18, C_GREEN, &lv_font_montserrat_14);
    panel(bar, 116, 8, 20, 11);
    panel(bar, 136, 11, 2, 5);
    s_battery_fill = panel(bar, 119, 11, 14, 5);
    lv_obj_set_style_border_width(s_battery_fill, 0, 0);
    lv_obj_set_style_bg_color(s_battery_fill, lv_color_hex(C_GREEN), 0);
    s_battery_text = label(bar, "--%", 144, 5, 48, 18, C_DIM, &lv_font_montserrat_14);
    s_phone_icon = panel(bar, 199, 5, 12, 18);
    lv_obj_set_style_radius(s_phone_icon, 2, 0);
    s_phone_dot = panel(bar, 215, 11, 6, 6);
    lv_obj_set_style_border_width(s_phone_dot, 0, 0);
    lv_obj_set_style_radius(s_phone_dot, LV_RADIUS_CIRCLE, 0);
    refresh_header(NULL);

    lv_obj_t *foot = panel(s_screen, 8, 282, 224, 28);
    const char *hint = "长按 OK 返回首页";
    if (s_app.screen == PASSPORT_SCREEN_HOME) {
        switch (s_app.selected) {
        case 0: hint = "短按 OK 个人卡片"; break;
        case 1: hint = "短按 OK 记录  长按历史"; break;
        case 2: hint = "短按 OK 打卡  长按清单"; break;
        case 3: hint = "短按 OK 刷码  长按清单"; break;
        default: break;
        }
    } else if (s_app.screen == PASSPORT_SCREEN_CHECKLIST) {
        hint = "上下选择  OK 操作  长按返回";
    } else if (s_app.screen == PASSPORT_SCREEN_QR_LIST) {
        hint = "上下选择  OK 查看  长按返回";
    } else if (s_app.screen == PASSPORT_SCREEN_QR && s_qr_opened_from_list) {
        hint = "长按 OK 返回二维码清单";
    }
    label(foot, hint, 7, 5, 210, 18, C_DIM, &lv_font_montserrat_14);
}

static void draw_home(void)
{
    base_screen("首页");
    lv_obj_t *head = panel(s_screen, 8, 42, 224, 76);
    lv_obj_t *avatar = panel(head, 8, 8, 48, 48);
    label(avatar, "我", 7, 13, 34, 24, C_GREEN, &lv_font_montserrat_20);
    label(head, s_trip.profile.nickname[0] ? s_trip.profile.nickname : "未设置昵称",
          68, 21, 145, 24, C_WHITE, &lv_font_montserrat_20);
    if (s_clock_timer == NULL) s_clock_timer = lv_timer_create(refresh_header, 1000, NULL);
    label(s_screen, "功能", 15, 123, 80, 18, C_GREEN,
          &lv_font_montserrat_14);

    static const char *hints[] = { "定位记录", "直接打卡", "显示二维码" };
    char checklist_name[PASSPORT_TRIP_NAME_BYTES] = "打卡清单";
    const passport_trip_destination_t *next = passport_trip_current(&s_trip);
    if (next != NULL) (void)snprintf(checklist_name, sizeof(checklist_name), "%s", next->name);
    const char *names[] = { "轨迹记录", checklist_name, "刷我的码" };
    for (size_t i = 0; i < 3; ++i) {
        const int y = 151 + (int)i * 41;
        s_home_items[i] = panel(s_screen, 8, y, 224, 34);
        label(s_home_items[i], names[i], 8, 4, 129, 21, C_WHITE,
              &lv_font_montserrat_14);
        label(s_home_items[i], hints[i], 142, 5, 74, 19, C_DIM,
              &lv_font_montserrat_14);
    }
    for (size_t i = 0; i < 3; ++i) {
        const bool selected = (i + 1 == s_app.selected);
        lv_obj_set_style_border_color(s_home_items[i], lv_color_hex(selected ? C_GREEN : C_LINE), 0);
        lv_obj_set_style_bg_color(s_home_items[i], lv_color_hex(selected ? 0x183528 : C_PANEL), 0);
    }
    /* The avatar is the focusable profile entry above the feature list. */
    lv_obj_set_style_border_color(avatar, lv_color_hex(s_app.selected == 0 ? C_GREEN : C_LINE), 0);
    finish_screen();
}

static void draw_profile(void)
{
    base_screen("个人卡片");
    lv_obj_t *card = panel(s_screen, 8, 48, 224, 214);
    label(card, s_trip.profile.nickname[0] ? s_trip.profile.nickname : "未设置昵称",
          16, 14, 192, 24, C_GREEN, &lv_font_montserrat_20);
    label(card, s_trip.profile.bio[0] ? s_trip.profile.bio : "暂无简介",
          16, 46, 192, 45, C_WHITE, &lv_font_montserrat_14);
    label(card, "社交账号", 16, 98, 192, 20, C_DIM, &lv_font_montserrat_14);
    if (s_trip.profile.social_count == 0) {
        label(card, "暂无账号", 16, 123, 192, 20, C_WHITE, &lv_font_montserrat_14);
    } else {
        for (size_t i = 0; i < s_trip.profile.social_count; ++i)
            label(card, s_trip.profile.social_ids[i], 16, 122 + (int)i * 20,
                  192, 19, C_WHITE, &lv_font_montserrat_14);
    }
    finish_screen();
}

static void draw_marker(void)
{
    const bool destination_action = s_pending_checkin || s_pending_uncheckin ||
                                    s_last_action_destination;
    base_screen(destination_action ? "打卡清单" : "轨迹记录");
    s_marker_card = panel(s_screen, 8, 48, 224, 214);
    label(s_marker_card, s_pending_checkin ? "正在打卡" : (s_pending_uncheckin ? "正在取消打卡" : "记录当前位置"), 16, 15, 192, 20, C_GREEN,
          &lv_font_montserrat_14);
    label(s_marker_card, destination_action ? s_active_destination_name : "由手机获取定位", 16, 52, 192, 22, C_WHITE,
          &lv_font_montserrat_14);
    const char *detail = s_marker_state == MARKER_WAITING ? "正在从手机获取定位……" :
                         (s_marker_state == MARKER_SUCCESS ? "手机已保存并确认" :
                          (s_marker_state == MARKER_FAILURE ? "本次操作未完成" :
                           "短按 OK 后由手机定位"));
    label(s_marker_card, detail, 16, 84, 194, 20, C_WHITE,
          &lv_font_montserrat_14);
    label(s_marker_card, s_pending_uncheckin ? "只更新清单，不改动轨迹" : "定位信息保存在手机端", 16, 106, 194, 20, C_WHITE,
          &lv_font_montserrat_14);
    const char *success_text = s_last_action_uncheckin ? "已取消打卡" :
                               (destination_action ? "打卡成功" : "轨迹已记录");
    const char *state = s_marker_state == MARKER_WAITING ? "正在等待手机响应……" :
                        (s_marker_state == MARKER_SUCCESS ? success_text :
                         (s_marker_state == MARKER_FAILURE ? s_marker_note : ""));
    s_marker_state_label = label(s_marker_card, state, 14, 157, 198, 23,
                                 C_GREEN, &lv_font_montserrat_14);
    const char *prompt = s_marker_state == MARKER_FAILURE
                       ? "请检查连接和手机定位"
                       : ((s_marker_state == MARKER_WAITING || s_marker_state == MARKER_SUCCESS)
                            ? "长按 OK 返回"
                            : (s_ble_connected ? "短按 OK 开始" : "请先连接手机"));
    label(s_marker_card, prompt, 16, 187, 194, 18, C_DIM,
          &lv_font_montserrat_14);
    finish_screen();
}

static void draw_history(void)
{
    base_screen("记录历史");
    const size_t total_rows = s_records.count;
    const size_t start = s_history_page * 7;
    const size_t rows = (total_rows - (start < total_rows ? start : total_rows)) < 7
                      ? total_rows - (start < total_rows ? start : total_rows) : 7;
    if (total_rows == 0) {
        lv_obj_t *card = panel(s_screen, 8, 48, 224, 214);
        label(card, "还没有轨迹记录", 16, 22, 190, 24, C_DIM,
              &lv_font_montserrat_14);
        label(card, "开始记录后会显示在这里", 16, 57, 198, 22, C_WHITE,
              &lv_font_montserrat_14);
        finish_screen();
        return;
    }
    for (size_t i = 0; i < rows; ++i) {
        const passport_marker_t *m = passport_location_model_newest(&s_records, start + i);
        char line[64];
        if (m == NULL) continue;
        if ((m->flags & PASSPORT_MARKER_HAS_PLACE_NAME) != 0)
            (void)snprintf(line, sizeof(line), "#%lu  %s", (unsigned long)m->id,
                           m->place_name);
        else if ((m->flags & PASSPORT_MARKER_HAS_COORDINATES) != 0)
            (void)snprintf(line, sizeof(line), "#%lu  %.5f, %.5f",
                           (unsigned long)m->id, m->latitude_e7 / 10000000.0,
                           m->longitude_e7 / 10000000.0);
        else {
            const time_t local = (time_t)m->timestamp_utc +
                                 (time_t)m->utc_offset_minutes * 60;
            struct tm tm_value;
            if (m->timestamp_utc > 0 && gmtime_r(&local, &tm_value) != NULL)
                (void)snprintf(line, sizeof(line), "#%lu  %02d:%02d  仅时间",
                               (unsigned long)m->id, tm_value.tm_hour, tm_value.tm_min);
            else
                (void)snprintf(line, sizeof(line), "#%lu  仅时间",
                               (unsigned long)m->id);
        }
        lv_obj_t *row = panel(s_screen, 8, 48 + (int)i * 31, 224, 27);
        label(row, line, 7, 4, 208, 19, C_WHITE, &lv_font_montserrat_14);
    }
    char page_text[32];
    (void)snprintf(page_text, sizeof(page_text), "%u / %u  上下翻页",
                   (unsigned)(s_history_page + 1),
                   (unsigned)((total_rows + 6) / 7));
    label(s_screen, page_text, 24, 263, 196, 17, C_DIM, &lv_font_montserrat_14);
    finish_screen();
}

static void draw_checklist(void)
{
    base_screen("打卡清单");
    const size_t page_count = (s_trip.destination_count + 6U) / 7U;
    if (page_count == 0) s_trip_page = 0;
    else if (s_trip_page >= page_count) s_trip_page = page_count - 1;
    if (s_trip_selection >= s_trip.destination_count) s_trip_selection = 0;
    const size_t start = s_trip_page * 7U;
    const size_t remaining = s_trip.destination_count > start
                           ? s_trip.destination_count - start : 0;
    const int rows = remaining < 7U ? (int)remaining : 7;
    if (rows == 0) {
        lv_obj_t *card = panel(s_screen, 8, 48, 224, 214);
        label(card, "清单还是空的", 14, 28, 198, 22, C_DIM, &lv_font_montserrat_14);
        label(card, "请先从手机同步项目", 14, 64, 198, 20, C_WHITE, &lv_font_montserrat_14);
        finish_screen();
        return;
    }
    int visible = 0;
    for (size_t i = start; i < s_trip.destination_count && visible < 7; ++i) {
        const passport_trip_destination_t *d = &s_trip.destinations[i];
        char text[72];
        /* UTF-8 圆点是多字节字符串，不能用单字节的 %c 输出。 */
        (void)snprintf(text, sizeof(text), "%s %02u  %.48s",
                       d->completed ? "+" : (i == s_trip_selection ? ">" : "·"),
                       (unsigned)(i + 1), d->name);
        lv_obj_t *row = panel(s_screen, 8, 48 + visible * 30, 224, 26);
        label(row, text, 7, 3, 210, 20,
              d->completed ? C_DIM : (i == s_trip_selection ? C_GREEN : C_WHITE),
              &lv_font_montserrat_14);
        ++visible;
    }
    char page[32];
    (void)snprintf(page, sizeof(page), "%u / %u  上下选择",
                   (unsigned)(s_trip_page + 1), (unsigned)page_count);
    label(s_screen, page, 24, 263, 196, 17, C_DIM, &lv_font_montserrat_14);
    finish_screen();
}

static void qr_render_callback(esp_qrcode_handle_t code)
{
    const int size = esp_qrcode_get_size(code);
    if (size < 21 || size > 57) return;
    const uint16_t white = 0xFFFFU;
    const uint16_t black = 0x0000U;
    for (size_t i = 0; i < 130U * 130U; ++i) s_qr_pixels[i] = white;
    const int scale = 2;
    const int quiet = 4;
    for (int y = 0; y < size; ++y) {
        for (int x = 0; x < size; ++x) {
            if (!esp_qrcode_get_module(code, x, y)) continue;
            const int left = (x + quiet) * scale;
            const int top = (y + quiet) * scale;
            for (int py = 0; py < scale; ++py)
                for (int px = 0; px < scale; ++px)
                    s_qr_pixels[(top + py) * 130 + left + px] = black;
        }
    }
    s_qr_ready = true;
}

static const passport_trip_qr_t *selected_qr(void)
{
    if (s_trip.qr_count == 0) return NULL;
    if (s_qr_selection >= s_trip.qr_count) s_qr_selection = 0;
    return &s_trip.qrs[s_qr_selection];
}

/* 清单展示全部已同步的码；与直接刷码共用索引，最多两页。 */
static void draw_qr_list(void)
{
    base_screen("二维码清单");
    label(s_screen, "二维码清单", 16, 43, 206, 20, C_GREEN, &lv_font_montserrat_14);
    if (s_trip.qr_count == 0) {
        label(s_screen, "还没有同步二维码", 24, 106, 192, 24, C_DIM, &lv_font_montserrat_14);
        label(s_screen, "请在手机端添加我的码", 24, 136, 192, 20, C_WHITE, &lv_font_montserrat_14);
    } else {
        if (s_qr_selection >= s_trip.qr_count) s_qr_selection = 0;
        const size_t start = s_qr_selection / 7U * 7U;
        for (size_t i = start; i < s_trip.qr_count && i < start + 7U; ++i) {
            const bool selected = i == s_qr_selection;
            lv_obj_t *row = panel(s_screen, 8, 72 + (int)(i - start) * 26, 224, 24);
            lv_obj_set_style_border_color(row, lv_color_hex(selected ? C_GREEN : C_LINE), 0);
            char text[64];
            (void)snprintf(text, sizeof(text), "%s %02u  %s", s_trip.qrs[i].starred ? "*" : " ",
                           (unsigned)(i + 1), s_trip.qrs[i].name);
            label(row, text, 7, 3, 210, 18, selected ? C_GREEN : C_WHITE, &lv_font_montserrat_14);
        }
        char page[32];
        (void)snprintf(page, sizeof(page), "%u / %u", (unsigned)(s_qr_selection / 7U + 1U),
                       (unsigned)((s_trip.qr_count + 6U) / 7U));
        label(s_screen, page, 24, 263, 196, 17, C_DIM, &lv_font_montserrat_14);
    }
    finish_screen();
}

static void draw_qr(void)
{
    base_screen("我的码");
    const passport_trip_qr_t *qr = selected_qr();
    if (qr == NULL) {
        label(s_screen, "还没有同步二维码", 24, 106, 192, 24, C_DIM, &lv_font_montserrat_14);
        label(s_screen, "请在手机端添加我的码", 24, 136, 192, 20, C_WHITE, &lv_font_montserrat_14);
        finish_screen();
        return;
    }
    s_qr_ready = false;
    esp_qrcode_config_t config = ESP_QRCODE_CONFIG_DEFAULT();
    config.display_func = qr_render_callback;
    config.max_qrcode_version = 10;
    config.qrcode_ecc_level = ESP_QRCODE_ECC_LOW;
    const esp_err_t encoded = esp_qrcode_generate(&config, qr->payload);
    log_memory("qr-generated");
    if (encoded != ESP_OK || !s_qr_ready) {
        label(s_screen, "二维码内容太长", 12, 118, 216, 24, C_AMBER, &lv_font_montserrat_14);
        label(s_screen, "请在手机端换用较短内容", 12, 148, 216, 20, C_DIM, &lv_font_montserrat_14);
    } else {
        char title[64];
        (void)snprintf(title, sizeof(title), "%u/%u  %.48s",
                       (unsigned)(s_qr_selection + 1), (unsigned)s_trip.qr_count, qr->name);
        label(s_screen, title, 16, 48, 208, 24, C_WHITE, &lv_font_montserrat_14);
        lv_obj_t *image = lv_image_create(s_screen);
        lv_image_set_src(image, &s_qr_image);
        lv_obj_align(image, LV_ALIGN_CENTER, 0, 24);
        label(s_screen, "上下切换二维码", 24, 263, 196, 17, C_DIM, &lv_font_montserrat_14);
    }
    finish_screen();
}

static void show_screen(passport_screen_t screen)
{
    switch (screen) {
    case PASSPORT_SCREEN_PROFILE: draw_profile(); break;
    case PASSPORT_SCREEN_MARKER: draw_marker(); break;
    case PASSPORT_SCREEN_HISTORY: draw_history(); break;
    case PASSPORT_SCREEN_CHECKLIST: draw_checklist(); break;
    case PASSPORT_SCREEN_QR: draw_qr(); break;
    case PASSPORT_SCREEN_QR_LIST: draw_qr_list(); break;
    case PASSPORT_SCREEN_HOME:
    default: draw_home(); break;
    }
}

static bool ble_event_callback(passport_ble_event_t event,
                               const passport_ble_result_t *result,
                               void *context)
{
    (void)context;
    if (s_queue == NULL) return false;
    app_event_t event_item = { .type = APP_EVENT_BLE };
    event_item.data.ble.event = event;
    if (result != NULL) event_item.data.ble.result = *result;
    if (event == PASSPORT_BLE_PAIRING_CODE)
        event_item.data.ble.pairing_code = passport_ble_link_pairing_code();
    return xQueueSend(s_queue, &event_item, pdMS_TO_TICKS(20)) == pdTRUE;
}

static void marker_timeout_callback(void *context)
{
    (void)context;
    s_marker_timeout_pending = true;
}

static void update_marker_screen(void)
{
    if (s_app.screen == PASSPORT_SCREEN_MARKER && bsp_lvgl_lock(500)) {
        draw_marker();
        bsp_lvgl_unlock();
    }
}

static void process_ble_event(const app_event_t *item)
{
    if (item->data.ble.event == PASSPORT_BLE_PAIRING_CODE) {
        (void)passport_display_policy_activity(&s_display_policy, esp_timer_get_time());
        bsp_display_backlight(100);
        if (bsp_lvgl_lock(500)) {
            draw_pairing_code(item->data.ble.pairing_code);
            bsp_lvgl_unlock();
        }
        return;
    }
    if (item->data.ble.event == PASSPORT_BLE_SETTINGS) {
        (void)passport_display_policy_set_timeout(&s_display_policy,
                passport_trip_store_screen_timeout(), esp_timer_get_time());
        return;
    }
    if (item->data.ble.event == PASSPORT_BLE_TIME) {
        const passport_ble_result_t *time_result = &item->data.ble.result;
        s_local_utc_offset_minutes = time_result->utc_offset_minutes;
        const struct timeval tv = {
            .tv_sec = (time_t)time_result->timestamp_utc,
            .tv_usec = 0,
        };
        (void)settimeofday(&tv, NULL);
        if (bsp_lvgl_lock(500)) {
            refresh_header(NULL);
            bsp_lvgl_unlock();
        }
        return;
    }
    if (item->data.ble.event == PASSPORT_BLE_SNAPSHOT) {
        if (bsp_lvgl_lock(500)) {
            if (s_app.screen == PASSPORT_SCREEN_HOME) draw_home();
            else if (s_app.screen == PASSPORT_SCREEN_CHECKLIST) draw_checklist();
            else if (s_app.screen == PASSPORT_SCREEN_QR_LIST) draw_qr_list();
            else if (s_app.screen == PASSPORT_SCREEN_QR) draw_qr();
            else if (s_app.screen == PASSPORT_SCREEN_PROFILE) draw_profile();
            bsp_lvgl_unlock();
        }
        return;
    }
    if (item->data.ble.event == PASSPORT_BLE_CONNECTED) {
        dismiss_pairing_screen();
        s_ble_connected = true;
        if (s_marker_state == MARKER_FAILURE) {
            s_marker_state = MARKER_IDLE;
            s_marker_note = "请先连接手机";
        }
        update_marker_screen();
        return;
    }
    if (item->data.ble.event == PASSPORT_BLE_DISCONNECTED) {
        dismiss_pairing_screen();
        s_ble_connected = false;
        if (s_marker_state == MARKER_WAITING) {
            (void)esp_timer_stop(s_marker_timeout);
            s_marker_timeout_pending = false;
            s_marker_state = MARKER_FAILURE;
            s_marker_note = "手机已断开连接";
        }
        update_marker_screen();
        return;
    }
    if (item->data.ble.event == PASSPORT_BLE_FAILED) {
        dismiss_pairing_screen();
        s_ble_connected = false;
        if (s_marker_state == MARKER_WAITING) {
            (void)esp_timer_stop(s_marker_timeout);
            s_marker_timeout_pending = false;
        }
        s_marker_state = MARKER_FAILURE;
        s_marker_note = "蓝牙连接失败";
        update_marker_screen();
        return;
    }
    if (item->data.ble.event == PASSPORT_BLE_REQUEST_FAILED) {
        if (s_marker_state == MARKER_WAITING) {
            (void)esp_timer_stop(s_marker_timeout);
            s_marker_timeout_pending = false;
            s_marker_state = MARKER_FAILURE;
            s_marker_note = "请求未送达手机";
            play_sound(SOUND_ERROR);
            update_marker_screen();
        }
        return;
    }

    const passport_ble_result_t *result = &item->data.ble.result;
    if (s_marker_state != MARKER_WAITING ||
        result->request_id != s_active_request_id) return;
    (void)esp_timer_stop(s_marker_timeout);
    s_marker_timeout_pending = false;
    if (!result->success) {
        s_marker_state = MARKER_FAILURE;
        s_marker_note = "手机未能保存定位";
        play_sound(SOUND_ERROR);
        update_marker_screen();
        return;
    }

    uint32_t marker_id = 0;
    bool recorded = true;
    if (s_pending_uncheckin) {
        recorded = passport_trip_model_set_completed(&s_trip, s_active_destination_id, false);
    } else {
        recorded = passport_location_model_add(
            &s_records, result->timestamp_utc, result->utc_offset_minutes, &marker_id);
        if (recorded && s_pending_checkin) {
            recorded = passport_location_model_set_place(&s_records, marker_id,
                                                           s_active_destination_name) &&
                       passport_trip_model_complete(&s_trip, s_active_destination_id);
        }
    }
    if (!recorded) {
        s_marker_state = MARKER_FAILURE;
        s_marker_note = "手机定位数据无效";
        update_marker_screen();
        return;
    }

    if (!s_pending_uncheckin) {
        s_local_utc_offset_minutes = result->utc_offset_minutes;
        if (passport_store_can_save() && passport_store_save(&s_records) != ESP_OK)
            ESP_LOGE(TAG, "Phone saved location but device NVS save failed");
        const struct timeval tv = { .tv_sec = (time_t)result->timestamp_utc, .tv_usec = 0 };
        (void)settimeofday(&tv, NULL);
    }
    if (s_pending_checkin || s_pending_uncheckin) (void)passport_trip_store_save(&s_trip);
    s_marker_state = MARKER_SUCCESS;
    play_sound(SOUND_OK);
    s_pending_checkin = false;
    s_pending_uncheckin = false;
    update_marker_screen();
}

static void start_marker_request(void)
{
    s_last_action_destination = false;
    if (!s_ble_connected) {
        s_marker_state = MARKER_FAILURE;
        s_marker_note = "请先连接手机";
        update_marker_screen();
        return;
    }
    if (s_marker_state == MARKER_WAITING) return;
    (void)esp_timer_stop(s_marker_timeout);
    s_marker_timeout_pending = false;
    uint16_t request_id = s_next_request_id++;
    if (request_id == 0) request_id = s_next_request_id++;
    s_active_request_id = request_id;
    ESP_LOGI(TAG, "starting track request id=%u", (unsigned)request_id);
    const esp_err_t err = passport_ble_link_request_location(request_id);
    if (err != ESP_OK) {
        s_marker_state = MARKER_FAILURE;
        s_marker_note = "请求发送失败";
        update_marker_screen();
        return;
    }
    s_marker_state = MARKER_WAITING;
    s_timeout_request_id = request_id;
    if (esp_timer_start_once(s_marker_timeout, 30000000) != ESP_OK) {
        s_marker_state = MARKER_FAILURE;
        s_marker_note = "等待响应超时";
    }
    update_marker_screen();
}

static void start_destination_request(const passport_trip_destination_t *destination,
                                      bool undo, passport_screen_t return_screen)
{
    if (destination == NULL) {
        s_app.screen = PASSPORT_SCREEN_CHECKLIST;
        if (bsp_lvgl_lock(500)) { draw_checklist(); bsp_lvgl_unlock(); }
        return;
    }
    (void)snprintf(s_active_destination_id, sizeof(s_active_destination_id), "%s",
                   destination->id);
    (void)snprintf(s_active_destination_name, sizeof(s_active_destination_name), "%s",
                   destination->name);
    s_pending_checkin = !undo;
    s_pending_uncheckin = undo;
    s_last_action_uncheckin = undo;
    s_last_action_destination = true;
    s_return_screen = return_screen;
    s_marker_note = "请检查连接后重试";
    if (!s_ble_connected) {
        s_marker_state = MARKER_FAILURE;
        s_marker_note = "请先连接手机";
        update_marker_screen();
        return;
    }
    if (s_marker_state == MARKER_WAITING) return;
    (void)esp_timer_stop(s_marker_timeout);
    s_marker_timeout_pending = false;
    uint16_t request_id = s_next_request_id++;
    if (request_id == 0) request_id = s_next_request_id++;
    s_active_request_id = request_id;
    ESP_LOGI(TAG, "starting checklist request id=%u opcode=%s destination=%s",
             (unsigned)request_id, undo ? "uncheck" : "check", destination->id);
    const esp_err_t err = undo
        ? passport_ble_link_request_uncheckin(request_id, destination->id, destination->name)
        : passport_ble_link_request_checkin(request_id, destination->id, destination->name);
    if (err != ESP_OK) {
        s_marker_state = MARKER_FAILURE;
        s_marker_note = "请求发送失败";
        update_marker_screen();
        return;
    }
    s_marker_state = MARKER_WAITING;
    s_timeout_request_id = request_id;
    if (esp_timer_start_once(s_marker_timeout, 30000000) != ESP_OK) {
        s_marker_state = MARKER_FAILURE;
        s_marker_note = "等待响应超时";
    }
    update_marker_screen();
}

static void start_checkin_request(void)
{
    const passport_trip_destination_t *destination = passport_trip_current(&s_trip);
    if (destination == NULL) {
        s_app.screen = PASSPORT_SCREEN_CHECKLIST;
        if (bsp_lvgl_lock(500)) { draw_checklist(); bsp_lvgl_unlock(); }
        return;
    }
    start_destination_request(destination, false, PASSPORT_SCREEN_HOME);
}

static void start_uncheckin_request(const passport_trip_destination_t *destination)
{
    start_destination_request(destination, true, PASSPORT_SCREEN_CHECKLIST);
}

static void process_input(const app_event_t *ev)
{
    if (ev->type == APP_EVENT_BLE) {
        process_ble_event(ev);
        return;
    }
    if (ev->type == APP_EVENT_TIMEOUT) {
        if (s_marker_state == MARKER_WAITING &&
            ev->data.request_id == s_active_request_id) {
            s_marker_state = MARKER_FAILURE;
            s_marker_note = "等待手机响应超时";
            update_marker_screen();
        }
        return;
    }
    /* 只把物理按键当作活动；BLE 通知、时间刷新都不会延长亮屏期限。
     * 第一次唤醒的整个手势被消费，直到下一个 PRESS，避免长按误进入页面。 */
    if (ev->data.button.event == BSP_BTN_PRESS) s_consume_wake_gesture = false;
    if (passport_display_policy_activity(&s_display_policy, esp_timer_get_time())) {
        bsp_display_backlight(100);
        s_consume_wake_gesture = true;
        s_wake_button = ev->data.button.btn;
        return;
    }
    if (s_consume_wake_gesture && ev->data.button.btn == s_wake_button) return;
    passport_input_t key = PASSPORT_INPUT_OTHER;
    if (ev->data.button.btn == BSP_BTN_OK && ev->data.button.event == BSP_BTN_LONG)
        key = PASSPORT_INPUT_OK_LONG;
    else if (ev->data.button.event == BSP_BTN_CLICK) {
        if (ev->data.button.btn == BSP_BTN_UP) key = PASSPORT_INPUT_UP;
        if (ev->data.button.btn == BSP_BTN_DOWN) key = PASSPORT_INPUT_DOWN;
        if (ev->data.button.btn == BSP_BTN_OK) key = PASSPORT_INPUT_OK;
    }
    if (key == PASSPORT_INPUT_OTHER) return;
    play_sound(key == PASSPORT_INPUT_OK_LONG ? SOUND_OK : SOUND_KEY);
    if (s_app.screen == PASSPORT_SCREEN_HOME) {
        if (key == PASSPORT_INPUT_UP || key == PASSPORT_INPUT_DOWN) {
            (void)passport_app_model_input(&s_app, key);
            if (bsp_lvgl_lock(500)) { draw_home(); bsp_lvgl_unlock(); }
            return;
        }
        if (key == PASSPORT_INPUT_OK_LONG) {
            if (s_app.selected > 0) s_app.last_feature = s_app.selected - 1;
            if (s_app.selected == 0) s_app.screen = PASSPORT_SCREEN_PROFILE;
            else if (s_app.selected == 1) {
                s_app.screen = PASSPORT_SCREEN_HISTORY;
                s_history_page = 0;
            } else if (s_app.selected == 2) { s_app.screen = PASSPORT_SCREEN_CHECKLIST; s_trip_page = 0; s_trip_selection = 0; }
            else if (s_app.selected == 3) {
                s_app.screen = PASSPORT_SCREEN_QR_LIST;
                s_qr_selection = 0;
                s_qr_opened_from_list = false;
            }
            if (bsp_lvgl_lock(500)) { show_screen(s_app.screen); bsp_lvgl_unlock(); }
            return;
        }
        if (key != PASSPORT_INPUT_OK) return;
        if (s_app.selected == 0) {
            s_app.screen = PASSPORT_SCREEN_PROFILE;
            if (bsp_lvgl_lock(500)) { draw_profile(); bsp_lvgl_unlock(); }
            return;
        }
        const uint8_t feature = (uint8_t)(s_app.selected - 1);
        s_app.last_feature = feature;
        (void)passport_location_model_set_last_feature(&s_records, feature);
        if (passport_store_can_save()) (void)passport_store_save(&s_records);
        if (feature == 0) {
            s_pending_checkin = false;
            s_app.screen = PASSPORT_SCREEN_MARKER;
            s_marker_state = MARKER_IDLE;
            if (bsp_lvgl_lock(500)) { draw_marker(); bsp_lvgl_unlock(); }
            start_marker_request();
        } else if (feature == 1) {
            s_app.screen = PASSPORT_SCREEN_MARKER;
            s_marker_state = MARKER_IDLE;
            if (bsp_lvgl_lock(500)) { draw_marker(); bsp_lvgl_unlock(); }
            start_checkin_request();
        }
        else {
            s_app.screen = PASSPORT_SCREEN_QR;
            s_qr_selection = 0;
            s_qr_opened_from_list = false;
            if (bsp_lvgl_lock(500)) { draw_qr(); bsp_lvgl_unlock(); }
        }
        return;
    }

    if (key == PASSPORT_INPUT_OK_LONG) {
        if (s_marker_state == MARKER_WAITING) {
            (void)esp_timer_stop(s_marker_timeout);
            s_marker_timeout_pending = false;
        }
        const passport_screen_t target = s_app.screen == PASSPORT_SCREEN_MARKER
            ? s_return_screen : (s_app.screen == PASSPORT_SCREEN_QR && s_qr_opened_from_list
                ? PASSPORT_SCREEN_QR_LIST : PASSPORT_SCREEN_HOME);
        s_app.screen = target;
        s_pending_checkin = false;
        s_pending_uncheckin = false;
        s_marker_state = MARKER_IDLE;
        if (bsp_lvgl_lock(500)) { show_screen(s_app.screen); bsp_lvgl_unlock(); }
        return;
    }
    if (key == PASSPORT_INPUT_OK && s_app.screen == PASSPORT_SCREEN_MARKER) {
        if (s_pending_checkin || s_pending_uncheckin) {
            const passport_trip_destination_t *destination = NULL;
            for (size_t i = 0; i < s_trip.destination_count; ++i)
                if (strcmp(s_trip.destinations[i].id, s_active_destination_id) == 0) {
                    destination = &s_trip.destinations[i]; break;
                }
            start_destination_request(destination, s_pending_uncheckin, s_return_screen);
        } else start_marker_request();
        return;
    }
    if (s_app.screen == PASSPORT_SCREEN_QR_LIST) {
        if (key == PASSPORT_INPUT_UP || key == PASSPORT_INPUT_DOWN) {
            if (s_trip.qr_count > 0) {
                s_qr_selection = key == PASSPORT_INPUT_UP
                    ? (s_qr_selection + s_trip.qr_count - 1U) % s_trip.qr_count
                    : (s_qr_selection + 1U) % s_trip.qr_count;
            }
            if (bsp_lvgl_lock(500)) { draw_qr_list(); bsp_lvgl_unlock(); }
        } else if (key == PASSPORT_INPUT_OK && s_trip.qr_count > 0) {
            s_qr_opened_from_list = true;
            s_app.screen = PASSPORT_SCREEN_QR;
            if (bsp_lvgl_lock(500)) { draw_qr(); bsp_lvgl_unlock(); }
        }
        return;
    }
    if ((key == PASSPORT_INPUT_UP || key == PASSPORT_INPUT_DOWN) &&
        s_app.screen == PASSPORT_SCREEN_CHECKLIST) {
        if (s_trip.destination_count > 0) {
            if (key == PASSPORT_INPUT_UP)
                s_trip_selection = (s_trip_selection + s_trip.destination_count - 1) % s_trip.destination_count;
            else s_trip_selection = (s_trip_selection + 1) % s_trip.destination_count;
            s_trip_page = s_trip_selection / 7U;
        }
        if (bsp_lvgl_lock(500)) { draw_checklist(); bsp_lvgl_unlock(); }
        return;
    }
    if (key == PASSPORT_INPUT_OK && s_app.screen == PASSPORT_SCREEN_CHECKLIST &&
        s_trip_selection < s_trip.destination_count) {
        const passport_trip_destination_t *destination = &s_trip.destinations[s_trip_selection];
        s_app.screen = PASSPORT_SCREEN_MARKER;
        s_marker_state = MARKER_IDLE;
        if (bsp_lvgl_lock(500)) { draw_marker(); bsp_lvgl_unlock(); }
        start_destination_request(destination, destination->completed, PASSPORT_SCREEN_CHECKLIST);
        return;
    }
    if ((key == PASSPORT_INPUT_UP || key == PASSPORT_INPUT_DOWN) &&
        s_app.screen == PASSPORT_SCREEN_QR) {
        if (s_trip.qr_count > 0) {
            if (key == PASSPORT_INPUT_UP)
                s_qr_selection = (s_qr_selection + s_trip.qr_count - 1) % s_trip.qr_count;
            else s_qr_selection = (s_qr_selection + 1) % s_trip.qr_count;
        }
        if (bsp_lvgl_lock(500)) { draw_qr(); bsp_lvgl_unlock(); }
        return;
    }
    if ((key == PASSPORT_INPUT_UP || key == PASSPORT_INPUT_DOWN) &&
        s_app.screen == PASSPORT_SCREEN_HISTORY) {
        const size_t page_count = (s_records.count + 6) / 7;
        if (page_count > 0) {
            if (key == PASSPORT_INPUT_UP)
                s_history_page = (s_history_page + page_count - 1) % page_count;
            else
                s_history_page = (s_history_page + 1) % page_count;
        }
        if (bsp_lvgl_lock(500)) { draw_history(); bsp_lvgl_unlock(); }
    }
}

static void input_task(void *arg)
{
    (void)arg;
    app_event_t ev;
    for (;;) {
        if (xQueueReceive(s_queue, &ev, pdMS_TO_TICKS(250)) == pdTRUE) process_input(&ev);
        if (s_marker_timeout_pending) {
            s_marker_timeout_pending = false;
            app_event_t timeout = { .type = APP_EVENT_TIMEOUT };
            timeout.data.request_id = s_timeout_request_id;
            process_input(&timeout);
        }
        if (passport_display_policy_tick(&s_display_policy, esp_timer_get_time()))
            bsp_display_backlight(0);
    }
}

static void on_key(bsp_btn_t btn, bsp_btn_ev_t event, void *user)
{
    (void)user;
    if (!s_input_ready || s_queue == NULL) return;
    const app_event_t ev = { .type = APP_EVENT_BUTTON,
        .data.button = { .btn = btn, .event = event } };
    (void)xQueueSend(s_queue, &ev, 0);
}

void app_main(void)
{
    ESP_LOGI(TAG, "AI Passport location notebook boot");
    bsp_i2c_init();
    if (bsp_display_init() != ESP_OK || bsp_lvgl_init() == NULL) {
        ESP_LOGE(TAG, "Display/LVGL initialization failed (MOSI=%d SCLK=%d CS=%d DC=%d BL=%d)",
                 BSP_LCD_MOSI, BSP_LCD_SCLK, BSP_LCD_CS, BSP_LCD_DC, BSP_LCD_BL);
        return;
    }
    log_memory("display-ready");
    app_fonts_init();
    bsp_display_backlight(100);
    s_battery_ready = bsp_battery_init() == ESP_OK;
    passport_location_model_init(&s_records);
    const esp_err_t store_err = passport_store_init(&s_records);
    if (store_err != ESP_OK || !passport_store_can_save())
        ESP_LOGW(TAG, "Starting with RAM records only");
    const esp_err_t trip_err = passport_trip_store_init(&s_trip);
    if (trip_err != ESP_OK)
        ESP_LOGW(TAG, "Starting with RAM itinerary only: %s", esp_err_to_name(trip_err));
    log_memory("storage-ready");
    passport_display_policy_init(&s_display_policy, esp_timer_get_time(),
                                 passport_trip_store_screen_timeout());
    passport_ble_link_bind_trip_model(&s_trip);
    const passport_marker_t *newest = passport_location_model_newest(&s_records, 0);
    if (newest != NULL) {
        s_local_utc_offset_minutes = newest->utc_offset_minutes;
    }
    passport_app_model_init(&s_app, PASSPORT_FEATURE_COUNT, s_records.last_feature);
    s_next_request_id = (uint16_t)esp_random();
    if (s_next_request_id == 0) s_next_request_id = 1;

    s_queue = xQueueCreate(INPUT_QUEUE_DEPTH, sizeof(app_event_t));
    s_sound_queue = xQueueCreate(SOUND_QUEUE_DEPTH, sizeof(sound_cue_t));
    if (s_queue == NULL || xTaskCreate(input_task, "passport_input", 4096, NULL, 5, NULL) != pdPASS) {
        ESP_LOGE(TAG, "Input task/queue creation failed");
        return;
    }
    if (s_sound_queue != NULL && xTaskCreate(sound_task, "passport_sound", 3072, NULL, 3, NULL) != pdPASS)
        ESP_LOGW(TAG, "提示音任务未启动");
    if (bsp_button_init(on_key, NULL) != ESP_OK) {
        ESP_LOGE(TAG, "Button initialization failed");
        return;
    }
    const esp_timer_create_args_t timeout_args = {
        .callback = marker_timeout_callback,
        .name = "marker_timeout",
    };
    if (esp_timer_create(&timeout_args, &s_marker_timeout) != ESP_OK) {
        ESP_LOGE(TAG, "Could not create marker timeout timer");
        return;
    }
    if (passport_ble_link_start(ble_event_callback, NULL) != ESP_OK) {
        s_marker_note = "蓝牙启动失败";
        s_marker_state = MARKER_FAILURE;
        ESP_LOGE(TAG, "BLE location link failed to start");
    }
    log_memory("ble-started");
    if (bsp_lvgl_lock(1000)) {
        draw_home();
        bsp_lvgl_unlock();
        s_input_ready = true;
    }
}
