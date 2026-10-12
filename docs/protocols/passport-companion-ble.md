<p align="right">
  <a href="passport-companion-ble.zh_CN.md">简体中文</a> · <strong>English</strong>
</p>

# AI Passport Companion App BLE Protocol (v3)

AI Passport is the BLE peripheral; the Android app is the central. The device requests a track capture or checklist check-in. The app obtains and stores coordinates locally. Coordinates are not sent back to the device.

## GATT service

Scan for the service UUID below. The device name is AI Passport.

| Purpose | UUID | Properties |
| --- | --- | --- |
| Passport service | 01504941-6E8C-214A-10A9-415000000001 | Primary service |
| Device commands | 02504941-6E8C-214A-10A9-415000000001 | Notify |
| App result | 03504941-6E8C-214A-10A9-415000000001 | Write / Write Without Response |
| App snapshot sync | 04504941-6E8C-214A-10A9-415000000001 | Write / Write Without Response |

Multi-byte integers are little-endian. The Android app maintains the connection with connectedDevice and location foreground-service types and shows a system notification. Android can still stop the process after force-stop, reboot, permission revocation, or OEM battery management; this is not an unconditional keep-alive guarantee.

## Device commands

### Track capture

Four bytes: version 0x03, opcode 0x01, request ID uint16. The app obtains the current location and persists it locally with the localized track label. It sends success only after persistence; otherwise it sends a failure result.

### Checklist check-in

To fit the default ATT MTU, the device sends ordered notification fragments:

| Bytes | Content |
| --- | --- |
| 0 | Version 0x03 |
| 1 | Fragment opcode 0x42 |
| 2 | Opcode: check-in 0x02, undo check-in 0x03 |
| 3–4 | request_id, uint16 |
| 5–6 | Total request payload length |
| 7–8 | Current fragment offset |
| 9–19 | Up to 11 payload bytes |

The assembled payload is destination ID length uint8, UTF-8 destination ID, name length uint8, and UTF-8 name. IDs are at most 36 bytes and names at most 47 bytes. Fragments must be reassembled contiguously by offset.

A check-in can target the incomplete item selected on the device. The app uses its local destination name and advances the list only after location capture and persistence succeed. Undoing a check-in marks the destination incomplete without deleting or modifying any track record and without creating a new track. It obtains the location and sends success only after saving to its local database. Permission, location, or storage failure returns failure; the device stays on the same item for a manual retry. The request ID plus destination ID provides idempotency.

## App result

An 11-byte success frame contains version 0x03, result opcode 0x81, status 0x00, matching request ID uint16, Unix UTC seconds uint32, and local UTC offset in minutes int16.

A 6-byte failure frame contains version 0x03, opcode 0x81, nonzero status, request ID uint16, and error code uint8.

The device stores only the timestamp and checklist item name from a successful response; it receives no latitude or longitude. Only a successful response allows it to advance the current item.

## App-to-device snapshot

A snapshot contains up to 64 destinations and up to 10 QR codes. QR names are at most 48 bytes and payloads at most 192 bytes. Up to three starred codes claim slots first; remaining slots are filled by QR codes for incomplete destinations in checklist order, then by completed destinations if space remains. A destination can bind to at most one QR code. The app is the sole owner of ordering, starring, completion, and rolling-window rules.

The app sends acknowledged GATT writes serially, waiting for each write callback:

- Begin frame (10 bytes): version 0x03, opcode 0x10, revision uint16, payload length uint16, CRC32 uint32.
- Data frame: version 0x03, opcode 0x11, revision uint16, offset uint16, up to 14 data bytes.
- Commit frame (4 bytes): version 0x03, opcode 0x12, revision uint16.

The payload contains destination count uint8; each destination has ID length and ID, name length and name, and completion status uint8; then QR count uint8; each QR has ID length and ID, name length and name, payload length and payload, starred status uint8, and bound destination ID length and ID. String lengths count UTF-8 bytes. CRC uses standard CRC-32/ISO-HDLC. The device replaces and persists the snapshot only after receiving and validating the complete snapshot; malformed data or a CRC mismatch leaves the previous snapshot intact.

## Limits

- Exact coordinates, the full QR library, and map edits remain on the phone; the device caches only the current snapshot and up to ten QR payloads.
- BLE pairing/encryption is not implemented yet. Do not use real payment codes or other sensitive QR payloads in an exposed test environment; link security must be added before real-device trials.
- The device generates QR codes locally with a maximum Version 10 and low error correction. The 192-byte limit and scan quality still require verification on the target display with real phone scanners.



## Profile and sound settings

The snapshot adds UTF-8 nickname (up to 48 bytes), bio (96 bytes), up to four social account IDs (48 bytes each), and a final uint8 sound volume (0–100, default 50). Snapshot protocol version is 3. Both device font sizes cover all 6,763 GB2312 ideographs.

## Screen timeout setting and device navigation

Write four bytes to the snapshot characteristic: `03 13 LL HH`. The last two bytes are timeout seconds as uint16 little-endian, accepted from 15 through 600; the default is 60. This independent setting does not change the snapshot format or revision. The device acknowledges success after persisting the value under NVS key `screen_secs` in namespace `passport_trip`; invalid frames or persistence failures receive a GATT error. Send it after the snapshot commit using acknowledged writes. Older firmware may accept the snapshot but reject this setting; clients must report that partial failure.

Android offers 15/30/60/120/300/600 seconds. A future iOS client should use the same command, range, and acknowledgement rules. Timeout counts physical button inactivity using a monotonic clock; clock refresh and BLE events do not extend it. Turning off the backlight keeps BLE running. The first button gesture wakes the screen without activating an action, and changing the timeout while dark does not wake it.

All pages show time, battery, and phone connection indicators in the top bar. The home profile card shows only the nickname beside the portrait. On the QR home entry, short OK displays a code and long OK opens all synced codes (up to 10, seven rows per page). Up/down selects a code, short OK opens it, long OK returns to the list, and long OK in the list returns home.
