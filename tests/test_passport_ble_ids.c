#include "passport_ble_ids.h"

#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

static void canonical_uuid(const uint8_t little_endian[16], char output[37])
{
    static const int groups[] = { 4, 2, 2, 2, 6 };
    size_t out = 0;
    int source = 15;
    for (size_t group = 0; group < sizeof(groups) / sizeof(groups[0]); ++group) {
        if (group != 0) output[out++] = '-';
        for (int byte = 0; byte < groups[group]; ++byte)
            out += (size_t)snprintf(output + out, 37 - out, "%02X", little_endian[source--]);
    }
}

static void expect_uuid(const uint8_t bytes[16], const char *expected)
{
    char actual[37] = { 0 };
    canonical_uuid(bytes, actual);
    assert(strcmp(actual, expected) == 0);
}

int main(void)
{
    const uint8_t service[] = { PASSPORT_BLE_SERVICE_UUID_BYTES };
    const uint8_t command[] = { PASSPORT_BLE_COMMAND_UUID_BYTES };
    const uint8_t result[] = { PASSPORT_BLE_RESULT_UUID_BYTES };
    const uint8_t snapshot[] = { PASSPORT_BLE_SNAPSHOT_UUID_BYTES };
    expect_uuid(service, PASSPORT_BLE_SERVICE_UUID);
    expect_uuid(command, PASSPORT_BLE_COMMAND_UUID);
    expect_uuid(result, PASSPORT_BLE_RESULT_UUID);
    expect_uuid(snapshot, PASSPORT_BLE_SNAPSHOT_UUID);
    return 0;
}
