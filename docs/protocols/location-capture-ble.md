<p align="right">
  <a href="location-capture-ble.zh_CN.md">简体中文</a> · <strong>English</strong>
</p>

# AI Passport Phone Location Capture BLE Protocol (v1)

> The current firmware and Android app use v3; v1 frames are not used by the current feature set. See the [companion app BLE v3 protocol](passport-companion-ble.md).

AI Passport advertises as a BLE peripheral; the phone app connects as the central. The phone provides the location using the operating system, persists the location record first, and sends the result back. AI Passport does not provide Swarm features.

## GATT service

Scan for the advertised service UUID; the complete device name `AI Passport` is supplied in the scan response, so use active scanning to display the name. UUID strings use the usual big-endian text representation:

| Purpose | UUID | Properties |
| --- | --- | --- |
| Location capture service | `01504941-6E8C-214A-10A9-415000000001` | Primary service |
| Device request characteristic | `02504941-6E8C-214A-10A9-415000000001` | Notify |
| Phone result characteristic | `03504941-6E8C-214A-10A9-415000000001` | Write / Write Without Response |

The phone must subscribe to notifications on the device request characteristic after connecting. The device sends a request only after subscription succeeds. Integers are little-endian; coordinates are signed WGS-84 decimal degrees multiplied by `10^7`.

## Device request

After a short press of the device confirm button, it sends 4 bytes:

| Bytes | Content |
| --- | --- |
| 0 | Protocol version `0x01` |
| 1 | Location capture request `0x01` |
| 2–3 | Request ID, `uint16` |

The phone app should request system location permission and a current location, then persist a record containing the coordinate, accuracy, UTC timestamp, and current UTC offset. It should send a success frame only after the record has been persisted. It should send a failure frame if permission is denied, location cannot be obtained, or persistence fails. The device waits 30 seconds, displays an animation while waiting, and displays a success animation after receiving a successful response with the matching Request ID. Timeout, disconnect, and failure responses show an error state.

## Phone result

A success response is 20 bytes, so it fits the default BLE ATT MTU without fragmentation:

| Bytes | Content |
| --- | --- |
| 0 | Protocol version `0x01` |
| 1 | Result opcode `0x81` |
| 2 | Success status `0x00` |
| 3–4 | Matching Request ID, `uint16` |
| 5–8 | Unix UTC seconds, `uint32` |
| 9–12 | Latitude, signed `int32`, WGS-84 E7 |
| 13–16 | Longitude, signed `int32`, WGS-84 E7 |
| 17 | Horizontal accuracy in meters, `uint8` (cap larger values at 255) |
| 18–19 | Local UTC offset in minutes, signed `int16` |

A failure response is 6 bytes: `version (0x01), opcode (0x81), nonzero status, request_id (uint16), error_code (uint8)`. The phone app defines failure status codes; the device currently displays the generic message “PHONE COULD NOT SAVE”. The device validates time, coordinates, accuracy, time zone, and Request ID, and accepts only a response for the currently pending request.

After receiving a successful result, the device stores the timestamp and coordinate in its NVS record and sets its system clock to the returned UTC time. The mobile app continues to resolve the place name with its map/place service; the place name is not sent in this BLE frame.
