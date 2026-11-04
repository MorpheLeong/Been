<p align="right"><a href="README.zh_CN.md">简体中文</a> · <strong>English</strong></p>

# Been Android companion

This is the Been Android companion app for the ESP32-C3 device firmware in this repository. The app uses Jetpack Compose and Material 3. Open the `android-app` directory as a project in Android Studio; the project uses AGP 9 built-in Kotlin support and includes a pinned Gradle Wrapper.

Build the debug APK from the command line:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Current behavior

- Four primary destinations: Track, Checklist, QR, and Me. Bluetooth, personal identity, and device settings live together under Me. The top app bar distinguishes disconnected, locally saved, pending, syncing, synced, and retryable failure states without duplicating a second global status strip. Edits stay on the phone until the user explicitly syncs them. Compact screens use bottom navigation and wider screens use a navigation rail.
- Track requests obtain one phone location and save coordinates locally. The root page exposes Map and Add as bottom-right floating actions. Real check-ins keep their time and coordinates read-only; app-created manual visits support second-precise time and coordinate editing, and coordinate edits are explicitly shown as manual. Both sources share the dated journey band and map.
- Checklist destinations show spring-animated overall progress and are grouped into Upcoming and Completed. Reorderable-powered long-press dragging follows the finger inside the current group, while TalkBack retains explicit move-up/move-down actions. The bottom-right Add action unifies one-place entry, pasted lists, tables, and files behind a candidate preview. Passport check-ins still advance the list only after both location and local persistence succeed.
- QR payloads are local by default and use the bottom-right Add action for creation; they can be edited, deleted, and starred (maximum 3). Tapping a card opens a high-contrast, full-screen in-app presentation; starring is available directly on the card and stays synchronized with the editor. Destination binding is configured in the QR editor; sync prioritizes starred and itinerary-bound codes.
- BLE runs in a user-started foreground service with a persistent notification and reconnect attempts. On first use, the app opens Android system Bluetooth settings, where the user enters the authenticated six-digit passkey shown on Passport; after pairing, the user returns to the app to connect. Later connections restore the encrypted bond. Android can still stop the process or restrict background starts; continuous reachability is not guaranteed.
- Profile settings include device screen timeout (15/30/60/120/300/600 seconds, default 60), saved locally and sent after snapshot sync. Updated firmware persists this setting; older firmware reports a setting failure after the snapshot has been synced.

## Current limits

- Place-name suggestions use Android's system Geocoder and no dedicated POI provider yet. Automatic check-in coordinates remain read-only; only app-created manual visits allow time and coordinate changes.
- BLE traffic is authenticated and encrypted, but QR payloads at rest do not have separate application-layer encryption. Production use still requires device validation and a storage/security review.
- Local unit tests, Lint, and Debug builds are covered. BLE resilience, background recovery, QR scan quality, and end-to-end check-ins still require validation with the target Passport hardware.

See `../docs/protocols/passport-companion-ble.md` for the v3 BLE protocol shared with firmware.
