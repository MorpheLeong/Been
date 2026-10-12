#include <assert.h>
#include "passport_display_policy.h"

int main(void)
{
    passport_display_policy_t p;
    passport_display_policy_init(&p, 100, 0);
    assert(p.lit && p.timeout_seconds == 60);
    assert(!passport_display_policy_tick(&p, 99));
    assert(!passport_display_policy_tick(&p, 60000099));
    assert(passport_display_policy_tick(&p, 60000100));
    assert(!passport_display_policy_tick(&p, 90000100));
    assert(passport_display_policy_set_timeout(&p, 15, 90000100));
    assert(!p.lit);
    assert(passport_display_policy_activity(&p, 100000000));
    assert(!passport_display_policy_activity(&p, 110000000));
    assert(!passport_display_policy_tick(&p, 124999999));
    assert(passport_display_policy_tick(&p, 125000000));
    assert(!passport_display_policy_set_timeout(&p, 14, 0));
    assert(!passport_display_policy_set_timeout(&p, 601, 0));
    assert(passport_display_policy_set_timeout(&p, 600, 0));
    assert(passport_display_policy_activity(&p, 0));
    assert(!passport_display_policy_tick(&p, 599999999));
    assert(passport_display_policy_tick(&p, 600000000));
    uint16_t seconds = 777;
    uint8_t frame[] = {3, 0x13, 60, 0};
    assert(passport_display_setting_decode(frame, 4, &seconds) && seconds == 60);
    frame[2] = 0x58; frame[3] = 2;
    assert(passport_display_setting_decode(frame, 4, &seconds) && seconds == 600);
    frame[2]++;
    assert(!passport_display_setting_decode(frame, 4, &seconds));
    frame[2] = 14; frame[3] = 0;
    assert(!passport_display_setting_decode(frame, 4, &seconds));
    frame[2] = 15;
    assert(passport_display_setting_decode(frame, 4, &seconds) && seconds == 15);
    assert(!passport_display_setting_decode(frame, 3, &seconds));
    frame[0] = 2;
    assert(!passport_display_setting_decode(frame, 4, &seconds));
    frame[0] = 3; frame[1] = 0x12;
    assert(!passport_display_setting_decode(frame, 4, &seconds));
    assert(!passport_display_setting_decode(NULL, 4, &seconds));
    assert(!passport_display_setting_decode(frame, 4, NULL));
    return 0;
}
