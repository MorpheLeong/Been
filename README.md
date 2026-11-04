<p align="right">
  <a href="README.zh_CN.md">简体中文</a> · <strong>English</strong>
</p>

# Been

**A personal place-memory system built with AI Passport and Android.**

I have a habit of checking in and recording the places I visit. For me, those records are more than a location history: they are a way to preserve preferences, routines, and small moments in a form that can be revisited later.

The problem is that location memories are usually scattered across different services. Recording one often means taking out a phone, opening the right app, finding the right page, and completing its workflow. That interruption is easy to postpone and can pull attention away from the moment itself.

Been turns AI Passport into a lightweight physical entry point. A short action on the device asks the Android companion to capture one location fix and save it locally. The record can then be reviewed, renamed, organized, and edited on the phone when there is a better time to do so.

## What is included

- **AI Passport firmware:** quick check-in, track history, checklist access, QR presentation, and BLE communication.
- **Android app:** journey timeline, track editor, map view, place library, checklists, QR management, and device sync.
- **Local-first workflow:** edits remain on the phone until the user explicitly syncs them to AI Passport.
- **Intentional location capture:** Been records a location when requested instead of continuously tracking in the background.

The Android app is built with Jetpack Compose and Material 3. The device firmware targets the ESP32-C3-based AI Passport and uses ESP-IDF 5.5.3.

## Privacy

Been has no project-operated account system, analytics backend, or location server. The current data flow is:

- **Local storage:** the Android app stores tracks, places, checklists, QR content, profile data, and settings in its private app storage. The AI Passport stores only content and settings sent to it through the paired BLE connection. Neither side automatically uploads this data to a Been-operated server.
- **Location:** the phone obtains a location when the user requests a check-in; Been is not designed as a continuous background location tracker. Reverse geocoding uses Android's system `Geocoder`, whose implementation may send coordinates to the geocoding service configured by the device.
- **Maps:** opening the map loads styles and tiles from [OpenFreeMap](https://openfreemap.org/), using OpenStreetMap-derived data. These requests disclose the requested map area and ordinary network metadata to OpenFreeMap and its delivery providers under the [OpenFreeMap privacy policy](https://openfreemap.org/privacy/).
- **OCR:** imported images are recognized on-device with the bundled Google ML Kit text-recognition model. Google states that input images and recognition results are not sent to its servers, although ML Kit may send operational and usage metrics as described in its [terms and privacy information](https://developers.google.com/ml-kit/terms).
- **Device sync:** communication with AI Passport uses a system-paired BLE link. It does not pass through a cloud relay. Data deliberately opened in an external map app, shared, exported, or placed in a user-selected backup folder is handled by the chosen app or storage provider.
- **Backups:** Android system cloud backup is disabled for Been. User-created backups are ZIP files written to a folder selected through Android's document picker; they are not additionally encrypted by Been, and that folder may itself belong to a cloud provider.

The project does not currently include an advertising SDK, a dedicated analytics SDK, or a crash-reporting SDK. Public screenshots and documentation use fictional place names, anonymized coordinates, and non-geographic sample routes. Real check-in coordinates are preserved as recorded; choosing a nearby place changes only the display name.

## Project origin

Been is an independent derivative project built on the open-source [FoloToy AI Passport](https://github.com/FoloToy/ai-passport) hardware platform and board-support foundation. Its product concept, Android companion, interaction design, and custom firmware experience are maintained independently and are not an official FoloToy release.

This repository retains the upstream MIT license and copyright notice. See [LICENSE](LICENSE).

## Open-source software and external services

The main external projects and services used directly by Been are:

- [FoloToy AI Passport](https://github.com/FoloToy/ai-passport), the upstream hardware and board-support foundation — MIT License.
- [ESP-IDF](https://github.com/espressif/esp-idf) and Espressif managed components for the firmware — primarily Apache License 2.0; each bundled component retains its own notice.
- [LVGL](https://github.com/lvgl/lvgl) for the embedded interface — MIT License.
- [AndroidX, Jetpack Compose, and Material 3](https://developer.android.com/jetpack/androidx) for the Android interface — Apache License 2.0.
- [MapLibre Native](https://github.com/maplibre/maplibre-native) for map rendering — BSD 2-Clause License.
- [OpenFreeMap](https://openfreemap.org/) for hosted map styles and tiles — subject to its [Terms of Service](https://openfreemap.org/tos/); underlying [OpenStreetMap](https://www.openstreetmap.org/copyright) data is licensed under ODbL 1.0 and requires attribution.
- [Google ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2/android) for on-device OCR — subject to the Google ML Kit terms.
- [Apache POI](https://poi.apache.org/), [ZXing](https://github.com/zxing/zxing), and [Reorderable](https://github.com/Calvin-LL/Reorderable) — Apache License 2.0; [jsoup](https://jsoup.org/) — MIT License.

The repository's [MIT License](LICENSE) covers Been's own and adapted project code. Third-party software, services, map data, models, and transitive dependencies remain subject to their respective licenses and terms.
