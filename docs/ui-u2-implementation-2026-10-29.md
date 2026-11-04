[简体中文](ui-u2-implementation-2026-10-29.zh_CN.md) · **English**

# Been Android UI-U2 Implementation Checkpoint

Date: 2026-10-29
Updated: 2026-10-30
Status: `ACCEPTED`
Prerequisite: UI-U1 explicitly accepted by the owner

## 1. Boundary

UI-U2 replaces screen-space annotation rebuilding with renderer-owned geographic geometry. It preserves the existing OpenFreeMap styles, route/favourite colours, and refit action, while distilling point detail into the Tracks-card location flow. It does not change Track-home action hierarchy.

## 2. Implemented candidate

- Build route geometry from raw longitude/latitude values as a GeoJSON source and render it with a MapLibre `LineLayer`.
- Build point geometry from the same raw coordinates as a second GeoJSON source. Ordinary points use a `CircleLayer`; favourite tiers 1–3 restore the established five-point-star semantics and tier colours through `SymbolLayer`s.
- Register density-aware 36 dp star bitmaps in the MapLibre style and disable symbol collision hiding. Shape changes never mutate feature coordinates or restore screen-space annotations.
- Keep point IDs in feature properties so a rendered point opens the existing detail sheet; the query has a 12 dp hit allowance.
- Distil the map-detail sheet to place name, check-in date/time, and one “Locate in Tracks” button. Remove accuracy, Edit, and external-map actions. The button reuses the existing Tracks focus path, returns to the main page, and scrolls to the matching card, including repeated navigation to the same track.
- After scrolling completes, show a 2 dp primary-colour focus outline for 1.8 seconds and fade it over 450 ms. The cue does not alter card height, favourite surface colour, or interaction semantics.
- Keep route vertices time-ordered within each local date and preserve every point feature, including coincident coordinates.
- Update source data only when Track rows change. Camera callbacks no longer clear or rebuild annotations.
- Keep the project on MapLibre Android 11.11.0 after a diagnostic 13.6.1 upgrade did not resolve native clustering on the PLZ110.

## 3. Native-clustering blocker

Native clustering is not enabled in the installed candidate. On the Android 17 PLZ110, a clustered runtime `GeoJsonSource` produced zero source features and no rendered features while the same data rendered correctly with clustering disabled.

The following diagnostics all reproduced the empty clustered source:

- MapLibre Android 11.11.0 and 13.6.1;
- `FeatureCollection`, raw-JSON string, and app-private file URI inputs;
- default asynchronous updates and `withSynchronousUpdate(true)`;
- source data supplied during construction and after source attachment;
- normal cluster filters and an unfiltered circle layer;
- the full 27-point fixture and a minimal two-valid-distinct-point fixture.

Fixture validation found 27 valid point features, 26 unique coordinates, one coincident pair, and no non-finite or out-of-range coordinate. The non-clustered source renders all points, so the failure is isolated to the native clustering path rather than coordinate generation or layer styling. The temporary 13.6.1 diagnostic path was removed, including all logging and cache-file code.

Relevant upstream references: [official clustering example](https://maplibre.org/maplibre-native/android/examples/styling/circle-layer/), [synchronous-update disappearing-layer issue](https://github.com/maplibre/maplibre-native/issues/4035), and [GeoJSON runtime rendering issue](https://github.com/maplibre/maplibre-native/issues/4471).

## 4. Verification to date

| Gate | Result | Notes |
|---|---|---|
| GeoJSON helper unit tests | PASS | Every point preserves coordinate, ID, and favourite tier; route vertices remain time-ordered |
| `testDebugUnitTest` | PASS | Rerun on the stable non-clustered candidate |
| `assembleDebug` | PASS | Data-preserving install on the connected PLZ110 |
| `lintDebug` | PASS | Final stable candidate; no blocking lint finding |
| Point detail | PASS | A visible point opens the distilled sheet with place name, check-in date/time, and one Tracks-card location action |
| Favourite stars | PASS | Two favourite stars are visible on the PLZ110 in light mode; an unobstructed star opens the correct place detail |
| Favourite/ordinary coordinate source | PASS | Circles and stars query the same point source; favourite state changes renderer-layer presentation only |
| Map detail to Tracks card | PASS | PLZ110 dark-mode check confirms no accuracy, one action, return to Tracks, and scroll to the correct card |
| Target-card location cue | PASS | Outline remains clearly visible after scroll, holds for 1.8 seconds, fades over 450 ms, and leaves no layout or visual residue |
| Five double-tap zooms, pan, refit | PASS for crash/recovery | Refit restores the complete point/route view |
| Ten rapid two-finger pinches and rotation | NOT RUN | ADB cannot substitute for the owner gesture |
| Projection error at idle, <= 2 px | NOT RUN | Requires the final clustered architecture and measurement harness |
| Cluster count conservation | FAIL / BLOCKED | Native clustered source is empty on this device |
| Offline cleared-cache failure and retry | NOT RUN | Required before owner acceptance |

Screenshots contain private place data and remain temporary local evidence outside the repository.

The final automated gate completed `BUILD SUCCESSFUL` (`testDebugUnitTest lintDebug assembleDebug`, 51 tasks: 8 executed, 43 up-to-date). Repository checks and `git diff --check` also pass. The 2026-10-30 installed final candidate with the extended location cue has APK SHA-256 `7F8E0948B81608E5742E57B7F97C109C53DA920F3CD366777F01FE36B8BF3E64`.

## 5. Gate state

- On 2026-10-30 the owner explicitly confirmed “UI-U2 acceptance complete”; UI-U3 is unlocked.
- `map/route-line-color`, `map/route-line-width`, `map/point-size`, `map/point-outline-width`, `map/favourite-point-colors`, `motion/track-focus-cue`, and the renderer-owned-coordinate contract are promoted to `accepted`.
- Native clustering, count conservation, automated projection measurement, and offline gates retain their recorded states. `map/cluster-radius` and `map/cluster-zoom-thresholds` remain `proposed` and were not incorrectly promoted by this acceptance.

## 6. Next decision

UI-U2 is closed. Work may proceed to UI-U3 Tracks signature hierarchy. Native-clustering compatibility remains an independent deferred item; it does not block the current personal-data scale and does not leak into UI-U3 scope.
