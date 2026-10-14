<p align="right"><a href="README.zh_CN.md">简体中文</a> · <strong>English</strong></p>

# AI Passport Android companion

This is the Android companion app for the ESP32-C3 AI Passport firmware in this repository. The app uses Jetpack Compose and Material 3. Open the `android-app` directory as a project in Android Studio; the project uses AGP 9 built-in Kotlin support and includes a pinned Gradle Wrapper.

Build the debug APK from the command line:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Current behavior

- Four primary destinations: Track, Checklist, QR, and Me. Bluetooth, profile, and device settings live together under Me. Every top bar shows connection/request status, and the top-right Sync button is the only content-push entry point. Edits stay on the phone until the user explicitly syncs the checklist, QR codes, profile, and settings. A standalone clock frame is sent once after each secure connection and again after manual sync. Compact screens use bottom navigation and wider screens use a navigation rail.
- Track requests obtain one phone location and save coordinates locally. The Passport receives a result timestamp only. Track names and coordinates can be edited; place search uses Android's system Geocoder.
- Checklist destinations can be added by text import and reordered. Because the app cannot check in directly, cards do not expose checkboxes and use tonal hierarchy to convey completion. A Passport check-in only marks the current destination complete after a location fix and local database save succeed.
- QR payloads are local by default, can be added/edited/deleted and starred (maximum 3). Destination binding is configured in the QR editor; checklist cards only indicate whether a code is bound. The app sends starred codes first, then codes bound to upcoming itinerary entries, followed by completed entries when capacity remains.
- BLE runs in a user-started foreground service with a persistent notification and reconnect attempts. On first use, the app opens Android system Bluetooth settings, where the user enters the authenticated six-digit passkey shown on Passport; after pairing, the user returns to the app to connect. Later connections restore the encrypted bond. Android can still stop the process or restrict background starts; continuous reachability is not guaranteed.
- Profile settings include device screen timeout (15/30/60/120/300/600 seconds, default 60), saved locally and sent after snapshot sync. Updated firmware persists this setting; older firmware reports a setting failure after the snapshot has been synced.

## Current limits

- The location editor supports coordinate entry and system Geocoder search, but does not yet provide an interactive map picker or a dedicated POI provider.
- BLE traffic is authenticated and encrypted, but QR payloads at rest do not have separate application-layer encryption. Production use still requires device validation and a storage/security review.
- Local unit tests, Lint, and Debug builds are covered. BLE resilience, background recovery, QR scan quality, and end-to-end check-ins still require validation with the target Passport hardware.

See `../docs/protocols/passport-companion-ble.md` for the v3 BLE protocol shared with firmware.
