#include "passport_display_policy.h"

static bool valid_timeout(uint16_t seconds)
{
    return seconds >= PASSPORT_SCREEN_TIMEOUT_MIN && seconds <= PASSPORT_SCREEN_TIMEOUT_MAX;
}

void passport_display_policy_init(passport_display_policy_t *policy, int64_t now_us,
                                  uint16_t seconds)
{
    if (policy == NULL) return;
    policy->last_activity_us = now_us;
    policy->timeout_seconds = valid_timeout(seconds) ? seconds : PASSPORT_SCREEN_TIMEOUT_DEFAULT;
    policy->lit = true;
}

bool passport_display_policy_set_timeout(passport_display_policy_t *policy,
                                         uint16_t seconds, int64_t now_us)
{
    if (policy == NULL || !valid_timeout(seconds)) return false;
    policy->timeout_seconds = seconds;
    policy->last_activity_us = now_us;
    return true;
}

bool passport_display_policy_activity(passport_display_policy_t *policy, int64_t now_us)
{
    if (policy == NULL) return false;
    const bool woke = !policy->lit;
    policy->lit = true;
    policy->last_activity_us = now_us;
    return woke;
}

bool passport_display_policy_tick(passport_display_policy_t *policy, int64_t now_us)
{
    if (policy == NULL || !policy->lit || now_us < policy->last_activity_us ||
        now_us - policy->last_activity_us < (int64_t)policy->timeout_seconds * 1000000)
        return false;
    policy->lit = false;
    return true;
}

bool passport_display_setting_decode(const uint8_t *frame, size_t length,
                                      uint16_t *seconds)
{
    if (frame == NULL || seconds == NULL || length != 4 || frame[0] != 3 ||
        frame[1] != PASSPORT_SCREEN_SETTING_OPCODE) return false;
    const uint16_t value = (uint16_t)frame[2] | ((uint16_t)frame[3] << 8);
    if (!valid_timeout(value)) return false;
    *seconds = value;
    return true;
}
