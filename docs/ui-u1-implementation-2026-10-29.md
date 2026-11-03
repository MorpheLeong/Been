[简体中文](ui-u1-implementation-2026-10-29.zh_CN.md) · **English**

# BugChecking Android UI-U1 Implementation and Acceptance Checkpoint

Date: 2026-10-29
Status: `ACCEPTED`
Prerequisite: UI-U0 explicitly accepted by the owner

Owner decision: the owner explicitly confirmed “UI-U1 accepted” on 2026-10-29.

## 1. Boundary

This checkpoint addresses legibility, text-scale reflow, editor bottom safe areas, and map-attribution safe areas. It does not change the accepted Dynamic Soft Candy/Night Fog direction, city-map background, four destinations, journey band, favourite hold, or business flows. Map projection, route geometry, clustering, and zoom stability remain UI-U2 work.

## 2. Implemented

- Give the transparent root Scaffold an explicit `onSurface` content colour.
- Keep Track add/edit app bars to the page title only. Remove the pseudo-subtitle that duplicated record mode, date/time, and precision; retain precision on track cards and place it directly beside the coordinate group, with copy reserved for location failures.
- Use a transparent host for bottom primary actions instead of a tonal-elevation `Surface`. Navigation-bar and IME insets remain applied, and scroll content still reserves the measured bar height.
- Use transparent edge-to-edge status/navigation bars and disable Android's forced navigation-bar contrast scrim. The page background now reaches the gesture area and the gesture handle follows light/dark appearance.
- Keep track names single-line: trailing ellipsis while unfocused, full horizontally scrolling value while focused.
- Keep track metadata single-line and bind numeric accuracy to its unit with a non-breaking space.
- Disable duplicate MapLibre branding and render one legal attribution on a semantic scrim with safe-drawing insets.
- Allow a debuggable APK to force static fallback colours for one launch through the ADB intent extra `com.bugchecking.app.extra.FORCE_STATIC_THEME=true`. Release builds ignore it; no product setting or persistent preference is added.
- Make the initial Smart Import input area scrollable with an IME-height reserve at the end. Shared blank-space focus clearing now fires only on an unconsumed pointer-up, so scrolling at 2.0× no longer dismisses the keyboard.

## 3. Automated gates

| Gate | Result | Notes |
|---|---|---|
| `testDebugUnitTest` | PASS | Long-name ellipsis, font-scale budget, and accuracy-unit coverage |
| `lintDebug` | PASS | No blocking lint finding |
| `assembleDebug` | PASS | Debug APK builds and was installed with `adb install -r` |

The final full gate was rerun after the last code change and finished `BUILD SUCCESSFUL` (51 tasks: 19 executed, 32 up-to-date). Final APK SHA-256: `C0D47003D911FB1A57DC57990F38E16527B1462D4AA3C4ADE6644CCD869505FB`.

## 4. PLZ110 device results

The device first used gesture navigation, then the owner enabled three-button navigation for the supplementary matrix; both modes passed. Testing restored `font_scale=1.0` and the original `custom_schedule` night mode, and the final device state retains gesture navigation. Screenshots contain private places, avatar data, and QR content and therefore remain temporary evidence outside the repository.

| Combination | Pages/states | Result |
|---|---|---|
| Dynamic dark, 1.0× | Tracks, Checklist, QR, Me, map, track editor, focused long name + IME | PASS |
| Dynamic dark, 2.0× | Four primary pages, map, track editor, QR editor | PASS |
| Dynamic dark, 1.3×/1.5× | Tracks, Me, track editor, QR editor | PASS |
| Dynamic light, 1.0× | Tracks, Checklist, QR, Me, map, track editor, QR editor | PASS |
| Dynamic light, 2.0× | Tracks, Checklist, Me, map, track editor, QR editor | PASS |
| Static fallback light/dark, 1.0×/2.0× | Four primary pages, map, Track add, and bottom action area | PASS |
| Three-button navigation | Four primary pages, map, Track add/edit, QR editor, Smart Import, and bottom actions | PASS |
| Remaining IME combinations | Track add, QR editor, Smart Import; 1.0×/2.0× | PASS |
| Bottom primary action and gesture navigation | Dynamic light/dark Track editor; dark theme with IME open | PASS — no outer Surface around Save, page colour reaches the screen edge, handle appearance flips automatically, and the button clears navigation/IME insets |

No fixed-matrix combination showed unreadable headings, hard clipping, bottom actions hidden by system navigation/IME, or duplicate attribution. At 2.0× the map attribution reflows to three lines; it is larger but complete and clear of the three-button navigation area. Coordinate/route drift while zooming remains reproducible and intentionally stays in UI-U2; it is not counted as a UI-U1 pass.

Follow-up correction: the redundant status subtitle was removed from Track add/edit and precision was restored beside the coordinate group. The final code passes its automated gates and was installed data-preservingly. After unlocking, the corrected variant was reviewed on-device with gesture navigation: dynamic light at 1.0× and 2.0× covered both Track add and device-track edit; dynamic dark at 1.0× covered both app bars and confirmed precision after coordinates on device-track edit. Both app bars contain only the page title, date/time is not duplicated, precision stays adjacent to coordinates, and the bottom primary action clears the gesture area. The dark Track-add capture was still locating, so its precision treatment is covered by the shared component path on device-track edit plus the light Track-add checks at 1.0× and 2.0×.

## 5. Candidate-contract state

The owner acceptance gate promotes these fixed-matrix candidates to `accepted`: `content/on-image-surface`, `surface/form-opaque`, `layout/editor-bottom-reserve`, `layout/safe-drawing-bottom`, and `map/attribution-scrim`. Their accepted definitions are now part of `DESIGN.md`, the personal development standard, and project memory.

## 6. Next steps

1. UI-U2 is unlocked.
2. Reproduce and fix map point/route drift across the full zoom matrix. It remains unresolved and is not covered by this acceptance.
