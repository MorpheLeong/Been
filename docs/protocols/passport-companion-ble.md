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
| Device commands | 02504941-6E8C-214A-10A9-415000000001 | Indicate preferred, Notify fallback, authenticated |
| App result | 03504941-6E8C-214A-10A9-415000000001 | Write, authenticated |
| App snapshot sync | 04504941-6E8C-214A-10A9-415000000001 | Write, authenticated |

Multi-byte integers are little-endian. The Android app maintains the connection with connectedDevice and location foreground-service types and shows a system notification. Android can still stop the process after force-stop, reboot, permission revocation, or OEM battery management; this is not an unconditional keep-alive guarantee.

## Link security and pairing

The GATT data path requires bonded, authenticated LE Secure Connections with a 128-bit key. On first connection, Passport shows a random six-digit passkey; enter that passkey in Android's system pairing dialog. Bond keys are retained in NVS and reused on later encrypted connections. The app does not accept a device by its advertised name: discovery and service validation require the exact service and characteristic UUIDs above.

On first use, the app opens Android system Bluetooth settings and lets the system handle passkey entry and bonding. The app no longer calls `createBond()` or uses its own timer to decide whether system pairing completed. After Android shows AI Passport as paired, return to the app and tap Connect device. After upgrading from firmware that did not enforce secure pairing, remove the old AI Passport entry from Android Bluetooth settings before pairing again. A rejected or stale bond never enables command notifications or app writes. BLE link encryption protects data in transit between the bonded phone and Passport; QR payloads are still stored in application/device storage rather than being separately encrypted by protocol v3.

## Device commands

### Track capture

Four bytes: version 0x03, opcode 0x01, request ID uint16. The app obtains the current location and persists it locally with the localized track label. It sends success only after persistence; otherwise it sends a failure result. The app database retains every track until the user deletes it manually. The Passport stores at most 32 records from the current local date and the preceding two local dates. The returned time and offset drive this rolling cleanup, which the device also applies as its clock crosses local midnight.

### Checklist check-in

To fit the default ATT MTU, the device sends ordered indication fragments and waits for the
central's confirmation before sending the next fragment. Notification mode remains a legacy
fallback and is sequenced from transmit-completion events:

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

The success timestamp is the immutable timestamp of the saved phone record, not the later time at which a retry is transmitted. The device stores the timestamp, request ID, and checklist item name; it receives no latitude or longitude. Only a successful response allows it to advance the current item.

## Asynchronous track-name update

Reverse geocoding and manual edits never delay the success result. The app sends names separately on the snapshot characteristic using an acknowledged, CRC-protected transaction. A batch contains at most the latest 32 device-originated tracks; records already pruned by the device are accepted as no-ops. The app replays the latest batch after each secure connection, so edits made while disconnected need no separate pending table.

- Begin (10 bytes): version `03`, opcode `19`, revision uint16, payload length uint16, CRC32 uint32.
- Chunk: version `03`, opcode `1A`, revision uint16, offset uint16, then negotiated-MTU-sized data.
- Commit (4 bytes): version `03`, opcode `1B`, revision uint16.

The payload starts with count uint8. Each item contains request ID uint16, the original track Unix UTC seconds uint32, UTF-8 name length uint8, and 1–47 name bytes. Request ID plus timestamp identifies a record and repeated updates are idempotent. Names must contain valid UTF-8 without control characters. Firmware reuses the existing snapshot receive buffer; track-name and ordinary snapshot transactions are serialized. A new app treats rejection of these optional opcodes by old firmware as a skipped name batch without failing location results or ordinary synchronization.

## App-to-device snapshot

A snapshot contains up to 64 destinations and up to 10 QR codes. QR names are at most 48 bytes and payloads at most 192 bytes. Up to three starred codes claim slots first; remaining slots are filled by QR codes for incomplete destinations in checklist order, then by completed destinations if space remains. A destination can bind to at most one QR code. The app is the sole owner of ordering, starring, completion, and rolling-window rules.

Only an explicit tap on the top-right Sync button sends checklist, QR, profile/avatar, and screen-timeout data through acknowledged GATT writes, serially waiting for each write callback. Editing the checklist, QR codes, profile, or settings saves locally and does not automatically generate synchronization traffic. The BLE connection stays active so it can receive track and check-in requests initiated by Passport.

- Begin frame (10 bytes): version 0x03, opcode 0x10, revision uint16, payload length uint16, CRC32 uint32.
- Data frame: version 0x03, opcode 0x11, revision uint16, offset uint16, with ATT MTU minus 9 data bytes: 14 bytes at the default MTU 23 or up to 238 bytes at MTU 247.
- Commit frame (4 bytes): version 0x03, opcode 0x12, revision uint16.

Android requests MTU 247 after connecting and sizes chunks from the negotiated result; it
falls back to 14-byte chunks when negotiation fails or the peer retains the default MTU.
Writes remain acknowledged and serialized with at most one operation in flight, and a failed
frame is retried at most twice, so the larger MTU does not change ordering, CRC, or transactional
commit semantics. A future iOS client should derive the same chunk size from the system-reported
maximum write length instead of assuming a fixed MTU.

The payload contains destination count uint8; each destination has ID length and ID, name length and name, and completion status uint8; then QR count uint8; each QR has ID length and ID, name length and name, payload length and payload, starred status uint8, and bound destination ID length and ID. String lengths count UTF-8 bytes. CRC uses standard CRC-32/ISO-HDLC. The device replaces and persists the snapshot only after receiving and validating the complete snapshot; malformed data or a CRC mismatch leaves the previous snapshot intact.

## Limits

- Exact coordinates, the full QR library, and map edits remain on the phone; the device caches only the current snapshot and up to ten QR payloads.
- BLE traffic is authenticated and encrypted after pairing. Storage encryption, application-layer end-to-end encryption, and production hardware acceptance remain separate review items; do not treat a successful build as a complete security certification.
- The device generates QR codes locally with a maximum Version 10 and low error correction. The 192-byte limit and scan quality still require verification on the target display with real phone scanners.



## Profile and sound settings

The snapshot adds UTF-8 nickname (up to 48 bytes), bio (96 bytes), up to four social account IDs (48 bytes each), and a final uint8 sound volume (0–100, default 50). Snapshot protocol version is 3. Both device font sizes cover all 6,763 GB2312 ideographs.

### Avatar transfer

The app keeps the user's square crop as a local PNG. During an explicit sync it composites transparency on `#101C18`, scales once to the 46×46 content area inside the 48×48 home avatar border, and sends 4,232 bytes of little-endian RGB565 on the snapshot characteristic. Avatar transfer is separate from the trip snapshot so the existing snapshot payload stays compatible.

- Begin: `03 15 01 2E 2E 88 10 CRC32`, where format 1 is RGB565 little-endian, dimensions are uint8, length and CRC are little-endian.
- Chunk: `03 16 OFFSET DATA`; offset is uint16 little-endian and chunks must be contiguous.
- Commit: `03 17`; the device accepts it only after all bytes and CRC match, then persists the image.
- Clear: `03 18`; removes the persisted avatar and restores the fallback profile glyph (`U+6211`).

All frames use acknowledged writes. With the legacy ATT MTU the app sends 16 data bytes per avatar chunk; with negotiated MTU 247 it sends up to 240. Invalid, out-of-order, truncated, or corrupt transfers leave the previous avatar intact.

## Screen timeout setting and device navigation

Write four bytes to the snapshot characteristic: `03 13 LL HH`. The last two bytes are timeout seconds as uint16 little-endian, accepted from 15 through 600; the default is 60. This independent setting does not change the snapshot format or revision. The device acknowledges success after persisting the value under NVS key `screen_secs` in namespace `passport_trip`; invalid frames or persistence failures receive a GATT error. Send it after the snapshot commit using acknowledged writes. Older firmware may accept the snapshot but reject this setting; clients must report that partial failure.

Each secure connection sends one eight-byte time frame, and manual synchronization sends it again at the end: version `03`, opcode
`14`, Unix UTC seconds as uint32, and local UTC offset minutes as little-endian int16. A valid
frame updates the device clock and header immediately without requiring a successful track or
check-in. The time frame is not persisted to NVS, so it must be sent again by reconnecting or
manually synchronizing after the device restarts.

Android offers 15/30/60/120/300/600 seconds. A future iOS client should use the same command, range, and acknowledgement rules. Timeout counts physical button inactivity using a monotonic clock; clock refresh and BLE events do not extend it. Turning off the backlight keeps BLE running. The first button gesture wakes the screen without activating an action, and changing the timeout while dark does not wake it.

All pages show time, battery, and phone connection indicators in the top bar. The home profile card shows only the nickname beside the portrait. On the QR home entry, short OK displays a code and long OK opens all synced codes (up to 10, seven rows per page). Up/down selects a code, short OK opens it, long OK returns to the list, and long OK in the list returns home.
