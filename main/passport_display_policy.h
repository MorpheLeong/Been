#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#define PASSPORT_SCREEN_TIMEOUT_DEFAULT 60U
#define PASSPORT_SCREEN_TIMEOUT_MIN 15U
#define PASSPORT_SCREEN_TIMEOUT_MAX 600U
#define PASSPORT_SCREEN_SETTING_OPCODE 0x13U

/* 由应用输入任务独占；时间使用单调微秒，不受手机同步时钟影响。 */
typedef struct {
    int64_t last_activity_us;
    uint16_t timeout_seconds;
    bool lit;
} passport_display_policy_t;

void passport_display_policy_init(passport_display_policy_t *policy, int64_t now_us,
                                  uint16_t seconds);
bool passport_display_policy_set_timeout(passport_display_policy_t *policy,
                                         uint16_t seconds, int64_t now_us);
/* 更新活动时间；返回 true 表示本次活动从息屏状态唤醒。 */
bool passport_display_policy_activity(passport_display_policy_t *policy, int64_t now_us);
/* 到达空闲期限时只返回一次 true，供输入任务关闭背光。 */
bool passport_display_policy_tick(passport_display_policy_t *policy, int64_t now_us);
bool passport_display_setting_decode(const uint8_t *frame, size_t length,
                                      uint16_t *seconds);
