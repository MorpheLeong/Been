[简体中文](ux-audit-android-2026-10-28.zh_CN.md) · **English**

# Been Android UI/UX audit — 2026-10-28

## 1. Scope and evidence

This is a full-product audit of the native Android companion app, with emphasis on the four primary destinations, their shared shell, the track/favourite/map flows, editors, backup settings, design tokens, Android adaptation, and Material 3 compliance. The current audience is the sole product owner/developer: they know the hidden gestures, accept learning cost, and value novelty and play. First-use conversion, public-facing explanation cost, and accessibility discoverability of the favourite gesture are not grading goals at this stage; functional correctness, visual identity, map geometry, and data recovery retain a higher bar.

Evidence used:

- Real-device captures from a PLZ110 device at 1216 × 2640, 560 dpi, package `com.bugchecking.app` version `0.1.0`, updated on 2026-10-28. The four primary pages were observed in light mode; Tracks and Me were also observed in dark mode.
- Real-device visual review of secondary flows: Place library, Place detail, Track map, Add track, Edit track, Add to checklist, Add QR, and Edit QR. The review only navigated and captured screens; it did not save, delete, or alter product data.
- Real-device font-scale samples at 1.0× and 2.0×; the QR editor was also sampled at 2.0×. The device was restored to 1.0× and its original scheduled theme mode after sampling.
- Accessibility node dumps for the primary and listed secondary pages.
- Source review of `MainActivity.kt`, `PassportDesign.kt`, `TrackFavoriteUi.kt`, `TrackMapScreen.kt`, `BackupSettings.kt`, the manifest, resources, and Gradle dependencies.
- Computed contrast checks on the static fallback palette. Runtime contrast over translucent, image-backed surfaces is not treated as measured contrast.
- Five product-owner constraints declared on 2026-10-28: both signature entries remain visible; track-card height stays fixed; map zoom has point/line displacement; helper copy stays minimal; hidden long-press favourite is intentional.

Not tested: first-install permission sequencing, TalkBack spoken order on-device, keyboard/switch access, tablet/foldable rendering, map-provider failure paths, offline transitions, destructive backup restore, avatar crop (which requires selecting a local image), or editor save-success, validation-error, and IME-open states. Point/route disappearance and displacement across zoom levels was reproduced and reported by the product owner but not independently recorded in this pass. The installed APK was visually consistent with the inspected source features, but this audit does not prove it was assembled from the exact current dirty worktree.

### Real-device visual coverage matrix

| Flow | Page/state | Coverage |
| --- | --- | --- |
| Primary | Tracks, Checklist, QR codes, Me | Light 1.0×; Tracks/Me also dark and 2.0× |
| Tracks FABs | Place library, Place detail, Track map, Add track | Light 1.0× |
| Track edit | Existing device check-in editor | Light 1.0× with a long place name |
| Checklist FAB | Add to checklist / Smart import initial state | Light 1.0× |
| QR FAB/edit | New QR and existing QR editor | Light 1.0×; editor also 2.0× |
| Not visually covered | Avatar crop, destructive restore, map failure, multi-zoom point/line stability, all validation/IME states | Zoom failure is owner-reproduced; the rest is code-only or untested; none is a visual pass |

## 2. Executive summary

The app is a recognisable personal tool rather than a generic Compose template: the city-map ground, journey band, soft-candy containers, Night Fog surfaces, and hidden long-press favourite form its character. Map and Place library are not primary tasks, but they are signature features that must remain persistently visible; the right adjustment is to lower their emphasis relative to Add, not hide them. The most important newly recorded functional defect is map zoom stability: the owner has reproduced disappearing or displaced points/lines, and the code clears and rebuilds annotations after camera-idle zoom changes. The dark-theme content-colour leak remains a directly observed high-priority defect. Fixed-height track cards are intentional, and the existing one-line trailing ellipsis is the correct direction; automatic wrapping would violate the product constraint. First-use discoverability, explanatory-copy volume, and the hidden favourite gesture are no longer findings. The right direction is rendering and hierarchy hardening, not turning a personal tool into an instructional consumer app.

## 3. What must be preserved

- The accepted Dynamic Soft Candy light palette and Night Fog blue-grey dark surfaces.
- The centred-crop city-map background as a product signature.
- The journey-band relationship between date groups and track cards.
- Four primary destinations: Tracks, Checklist, QR codes, and Me.
- Dedicated pages for stateful editing/import/crop flows; short confirmation dialogs and contextual bottom sheets.
- Checklist grouping, group-bounded long-press reorder, and TalkBack Move up / Move down actions.
- Dynamic colour on Android 12+ with a deliberate static fallback.
- Persistently visible Map and Place-library entries on Tracks, with Add as the only primary action.
- Uniform track-card height with one-line trailing ellipsis for place names.
- Hidden, tiered, feedback-rich long-press favourite interaction.
- Minimal helper copy and icon-led personal-tool density.

## 4. Quick wins

1. Set an explicit `onSurface` content colour for the transparent primary-page scaffold and add dark-theme screenshot tests.
2. Keep Add as the only FAB; retain Map and Place library as persistent lower-emphasis icon actions, never hidden in overflow.
3. Keep number and unit together in track metadata and test all primary pages at 2.0× font scale.
4. Stop fading supporting text inside favourite cards until every dynamic/light/dark combination clears 4.5:1.
5. Move map points and route lines to MapLibre geographic sources/layers and test the full zoom, pan, and rotation range.
6. Reserve dynamic safe space beneath scrollable editor content; keep track-card names on one line with trailing ellipsis.

## 5. Findings, sorted by severity

### High

#### A11Y-01 · Dark-theme page headings become unreadable

| Field | Detail |
| --- | --- |
| Category | Normative defect · accessibility · usability |
| Confidence | Observed (real-device render) + observed (code) |
| Location | Tracks date headings; Me section headings; shared primary content under `PassportRootScaffold`; `MainActivity.kt:872-949`, `MainActivity.kt:1281-1287`, `MainActivity.kt:987-1032` |
| Evidence | In the dark Tracks capture, date headings render black against a dark blue-grey/image surface. In the dark Me capture, the Personal identity section heading is effectively absent while card content remains light. The root scaffold uses `Color.Transparent` and does not establish an explicit content colour for its content slot. |
| Why it matters | Users who prefer dark mode lose the grouping labels that explain the list and the settings hierarchy. This is a core-content contrast failure, not a cosmetic mismatch. |
| Recommendation | Establish `MaterialTheme.colorScheme.onSurface` at the shared root content boundary, then keep component-specific `onX` pairings inside coloured containers. Add dark screenshot regression coverage for all four primary pages and secondary page titles. |
| Expected impact | Restores readable hierarchy across dark mode with one shared fix and prevents per-page colour patches. |
| Effort | S |

#### RESP-01 · 2.0× font scale causes clipping and broken information groups

| Field | Detail |
| --- | --- |
| Category | Normative defect · Android accessibility · usability |
| Confidence | Observed (real-device render) |
| Location | Me page section start; Tracks cards and bottom navigation; QR editor; `MainActivity.kt:987-1027`, `MainActivity.kt:1276-1337`, `MainActivity.kt:1923-1993` |
| Evidence | At 2.0×, the Me section heading is clipped below the top app bar. On Tracks, `15 m` splits with an orphaned `m`. In the QR editor, the Bind checklist item heading and control are pushed below the fixed bottom action, making the content/action boundary unclear. |
| Why it matters | Large-font users cannot reliably read section structure or compact supporting facts. The layout technically scrolls, but comprehension and target access degrade before scrolling can help. |
| Recommendation | Treat 2.0× as a required layout state: avoid fixed assumptions at the top of scroll content, allow section headers and cards to grow, and keep value/unit tokens together. Editors must scroll and reserve the measured bottom-action height plus navigation/IME insets. Test 1.0×, 1.3×, 1.5×, and 2.0× in light and dark modes. |
| Expected impact | Makes the app usable for large-font users and reduces future clipping regressions across all form and list screens. |
| Effort | M |

### Medium

#### VIS-01 · Three equal-weight FABs obscure track content

| Field | Detail |
| --- | --- |
| Category | Usability issue · visual hierarchy |
| Confidence | Observed (real-device render + code) |
| Location | Tracks page lower-right action stack; `MainActivity.kt:515-532` |
| Evidence | Place library, Map, and Add are three same-size floating buttons stacked over the list. They cover portions of two track cards at 1.0× and proportionally more at 2.0×. Only icons explain the actions. |
| Why it matters | Three actions all look primary and permanently cover records. They fail to express the real hierarchy: Add is primary; Map and Place library are signature secondary actions. Icon discoverability is not a current product problem. |
| Recommendation | Keep Add track as the only primary FAB. Retain Map and Place library as persistent, easy-to-hit, lower-emphasis icon actions on Tracks—for example top-bar actions or a compact signature tool strip. Do not hide them in overflow or require text labels. Preserve routes and behaviour. |
| Expected impact | Clarifies the main action, exposes more content, and improves one-handed use without redesigning the page. |
| Effort | M |

#### INPUT-01 · Long editor values lack an explicit overflow cue

| Field | Detail |
| --- | --- |
| Category | Usability polish · content adaptation |
| Confidence | Observed (real-device render) |
| Location | Place name field in an existing track editor; `MainActivity.kt:2375-2640` |
| Evidence | A long device-generated place name exceeds the single-line field at 1.0× font scale and simply disappears at the trailing edge. There is no ellipsis, field growth, or visible horizontal-position cue. |
| Why it matters | The owner may occasionally need to verify the full value while editing. This is a low-cost inconvenience in a personal tool and does not affect track-card scanning. Track cards already keep uniform height with `maxLines = 1` and `TextOverflow.Ellipsis`. |
| Recommendation | Do not change track cards: retain one-line trailing ellipsis and never wrap. The editor may scroll horizontally while focused and show deliberate trailing ellipsis while unfocused; it need not grow vertically. An optional marquee starts only on hold/selection. |
| Expected impact | Makes real long content verifiable and reduces accidental renaming without changing data or navigation. |
| Effort | S |

#### MAP-01 · Map attribution collides with the bottom system gesture area

| Field | Detail |
| --- | --- |
| Category | Normative defect · Android insets · usability |
| Confidence | Observed (real-device render + code) |
| Location | Bottom of Track map; `TrackMapScreen.kt:197-235` |
| Evidence | `© OpenStreetMap contributors · OpenFreeMap` hugs the display edge and overlaps the gesture bar/map labels; cropped map text is also visible at the left edge. |
| Why it matters | Required attribution is difficult to read and the lower edge looks like a rendering fault. Other navigation modes and landscape can amplify the collision. |
| Recommendation | Put attribution in a dedicated scrim/control layer that consumes `navigationBars` or `safeDrawing` insets, with readable contrast and minimum padding. Make the map viewport avoid the same area instead of moving only the text. |
| Expected impact | Stabilises attribution and bottom-edge finish across gesture and three-button navigation. |
| Effort | S |

#### MAP-02 · Custom map points and route lines can disappear or drift while zooming

| Field | Detail |
| --- | --- |
| Category | Functional defect · data-display correctness |
| Confidence | Reproduced and reported by the product owner; implementation mechanism observed (code); runtime failure not independently reproduced in this pass |
| Location | Track-map zoom and pan; `TrackMapScreen.kt:121-205` |
| Evidence | The owner reports that coordinates disappear or shift at some zoom ratios, while equivalent map-service-native drawing does not. Code updates zoom from `addOnCameraIdleListener`, then `LaunchedEffect(map, rows, zoom, ...)` calls `current.clear()` and recreates `PolylineOptions`, clusters, and markers. |
| Why it matters | Map is one of the app's few signature features. A point or line detached from its real coordinate makes the map untrustworthy even when the stored track data is intact. Clear-and-rebuild legacy annotations also create opportunities for flicker, races, and cluster jumps during zoom. |
| Recommendation | Use MapLibre style GeoJSON sources with LineLayer/CircleLayer/SymbolLayer so the renderer projects geographic coordinates directly. Camera changes should adjust clustering/visibility parameters rather than clear all annotations. Verify zoom 4/8/12/16/19 plus rapid pinch, continuous pan, and rotation on-device. |
| Expected impact | Stable geographic anchoring at every scale, with less flicker and no change to the current visual or route behaviour. |
| Effort | M |

#### A11Y-02 · Favourite-card text has no robust contrast margin

| Field | Detail |
| --- | --- |
| Category | Normative defect · visual legibility |
| Confidence | Observed (code) + computed (static palette); runtime result varies with translucency |
| Location | Favourite tier colours and supporting text; `PassportDesign.kt:94-124`, `MainActivity.kt:1332-1336`, `TrackFavoriteUi.kt:347-356` |
| Evidence | The darkest static favourite pair `#DDF8F3` on `#397A71` computes to 4.48:1 before the card is made translucent. Supporting text is then reduced to 0.76–0.78 alpha. |
| Why it matters | Normal-size supporting text can fall below AA depending on theme, wallpaper-derived colours, and the map pixels behind the card. The highest favourite tier is exactly where the strongest colour treatment is used. |
| Recommendation | Define explicit `onFavouriteTierN` semantic tokens validated against the final composited surface. Do not use alpha to demote normal-size text unless the composited pair still clears 4.5:1. Use weight or type role before opacity. |
| Expected impact | Stabilises legibility across dynamic colour, dark mode, and the image background while retaining the mint favourite identity. |
| Effort | S–M |

#### UX-02 · QR cards combine navigation and three adjacent icon actions

| Field | Detail |
| --- | --- |
| Category | Usability issue · action hierarchy |
| Confidence | Observed (real-device render + code) |
| Location | QR list cards; `MainActivity.kt:1568-1613` |
| Evidence | The whole card opens presentation while Star, Edit, and Delete sit adjacent on the same row. Delete is protected by confirmation, but its visual weight and proximity match routine actions. |
| Why it matters | Three equal-weight icons visually compete with the whole-card Present action, and Delete sits close to routine actions despite its confirmation. The issue is hierarchy and target separation, not whether a newcomer understands the icons. |
| Recommendation | Preserve icon-first, low-copy UI. Star may remain inline; Edit/Delete can move to an unlabelled overflow menu, or Delete can be separated through spacing, tone, and position. |
| Expected impact | Reduces target competition and accidental destructive taps while preserving all functions. |
| Effort | S–M |

#### DS-01 · Busy imagery is allowed to remain visible through dense forms

| Field | Detail |
| --- | --- |
| Category | Normative defect · design-system consistency |
| Confidence | Observed (real-device render + code) |
| Location | All primary pages, most noticeable on Me fields; `PrimaryPageBackdrop` and translucent surface helpers in `PassportDesign.kt:68-92`, `MainActivity.kt:816-835` |
| Evidence | The city-map pattern remains visible through bars, cards, and text fields. It is attractive on sparse list surfaces but competes with field outlines, labels, and long values on Me. |
| Why it matters | Contrast and visual noise vary by the map pixels behind each component. The same alpha recipe does not serve both sparse cards and high-density editable forms. |
| Recommendation | Preserve the centred map identity, but define surface opacity by role: chrome, list card, summary card, and form container. Dense forms need a more opaque container or a local scrim; avoid per-screen arbitrary alpha. |
| Expected impact | Retains product recognition while improving reading speed and making contrast testable. |
| Effort | S–M |

#### PLAT-01 · Adaptive navigation stops at Compact versus not-Compact

| Field | Detail |
| --- | --- |
| Category | Normative defect · Material 3 / Android adaptation |
| Confidence | Observed (code); tablet render not tested |
| Location | Window-size handling and root shell; `MainActivity.kt:258-262`, `MainActivity.kt:467`, `MainActivity.kt:841-951` |
| Evidence | Compact uses a NavigationBar and every other width uses the same NavigationRail/content column. Medium and Expanded do not receive distinct content structures or list-detail use, although 720/840 dp max-width tokens exist. |
| Why it matters | Tablets and foldables get a widened phone composition rather than a layout that uses available space or preserves context between list and detail. |
| Recommendation | Keep the current Compact shell. Define Medium as rail + constrained single pane, and Expanded as rail + list/detail or supporting pane where the flow benefits. Add hinge-aware tests before claiming foldable support. |
| Expected impact | Better tablet/foldable productivity and clearer compliance with current Compose-first adaptive guidance. |
| Effort | L |

#### PLAT-02 · Predictive back is explicitly disabled

| Field | Detail |
| --- | --- |
| Category | Normative defect · Android platform behaviour |
| Confidence | Observed (manifest + navigation code) |
| Location | `AndroidManifest.xml:14`; all secondary routes |
| Evidence | The app targets SDK 36 but sets `android:enableOnBackInvokedCallback="false"`. Secondary routes already use directional Compose transitions and BackHandler in specific screens. |
| Why it matters | System back behaviour and previews do not match modern Android expectations, especially on gesture navigation. Users lose a platform-level orientation cue. |
| Recommendation | Remove the opt-out, test predictive back on every secondary route, and keep custom BackHandler only where it protects an active operation. |
| Expected impact | Restores platform-consistent navigation without changing the route model. |
| Effort | M |

#### FBK-01 · Sync errors are transient and can be truncated

| Field | Detail |
| --- | --- |
| Category | Usability issue · system status and recovery |
| Confidence | Observed (code); failure render not triggered |
| Location | Top-app-bar status; `MainActivity.kt:450-465`, `MainActivity.kt:883-921` |
| Evidence | Status text disappears after five seconds, is restricted to 132 dp on compact screens, and uses one line with ellipsis. The icon remains retryable, but the reason can vanish before it is read. |
| Why it matters | A user knows sync failed but may not know why or what will happen on retry. Error recovery should persist until resolved or dismissed. |
| Recommendation | Keep short success/status messages transient. Keep failure state persistent and expose the full message through a sheet/snackbar action or a dedicated status row; do not rely on a truncated live-region label. |
| Expected impact | Improves recovery and trust without adding permanent chrome during healthy operation. |
| Effort | S–M |

### Low

#### DS-02 · Token use is good but not yet closed over spacing and component roles

| Field | Detail |
| --- | --- |
| Category | Normative defect · design-token drift |
| Confidence | Observed (code) |
| Location | `PassportSpacing` plus hard-coded 1, 2, 6, 8, 12, 16, 20, 24, 32, 36, 56, 64, 96, 104, 132, 220, 420, and 520 dp values across UI files |
| Evidence | A central scale and max-width tokens exist, but repeated component padding, content reserves, icon sizes, star gaps, and overlay offsets remain magic values. Some are legitimate component dimensions; their roles are undocumented. |
| Why it matters | Future pages can copy values without knowing whether they are global spacing, component anatomy, or a one-off fit, causing slow visual drift. |
| Recommendation | Keep the base spacing scale and add only durable semantic/component tokens such as page horizontal inset, card padding, bottom content reserve, avatar size, and supporting-icon size. Do not tokenise every number. |
| Expected impact | Makes consistency easier to maintain without burying Compose defaults under excessive abstraction. |
| Effort | S |

#### TASTE-01 · The glass treatment is too uniform across content types

| Field | Detail |
| --- | --- |
| Category | Subjective aesthetic recommendation |
| Confidence | Observed (real-device render) |
| Location | Four primary pages |
| Evidence | Top bar, bottom navigation, summary cards, list cards, and form containers all use closely related translucent materials. The result is coherent, but major and minor surfaces sometimes have similar visual emphasis. |
| Why it matters | The app risks looking like one continuous frosted layer rather than a hierarchy of content, controls, and summaries. This is polish, not a usability failure by itself. |
| Recommendation | Keep the signature material but reserve the strongest candy container for summaries/selection, use quieter surfaces for routine rows, and make dense form containers more opaque. Avoid new gradients, glows, or decorative cards. |
| Expected impact | Adds authored hierarchy without changing the product's accepted visual identity. |
| Effort | S–M |

## 6. Material 3 compliance snapshot

These category snapshots describe the current implementation against Material 3/Compose guidance and are not combined into a general UX score. Accessibility is not a release goal for this personal-use stage, but defects that directly affect legibility remain recorded.

| Category | Score | Status | Note |
| --- | ---: | --- | --- |
| Colour tokens | 7/10 | Pass | Strong `ColorScheme`; favourite/translucent composites need contracts. |
| Typography | 6/10 | Warn | Correct roles, but large-font reflow fails. |
| Shape | 9/10 | Pass | Coherent 8/12/16/28 dp scale and Material shapes. |
| Elevation | 8/10 | Pass | Tonal surfaces dominate; drag/dialog elevation is purposeful. |
| Components | 8/10 | Pass | Material 3 primitives are used consistently. |
| Layout | 6/10 | Warn | Compact is solid; medium/expanded structure is incomplete. |
| Navigation | 7/10 | Pass | Clear four-destination shell; predictive back is disabled. |
| Motion | 8/10 | Pass | Short transitions and restrained springs support state. |
| Accessibility | 5/10 | Not a gate | Good semantics effort; hidden-favourite discoverability is not penalised, while dark legibility and 2.0× clipping remain real display defects. |
| Theming | 8/10 | Pass | Dynamic colour + fallback + Night Fog are well established. |

## 7. Prioritised backlog

| # | ID | Action | Severity | Effort |
| ---: | --- | --- | --- | --- |
| 1 | A11Y-01 | Fix shared dark-mode content colour and add regression captures | High | S |
| 2 | RESP-01 | Harden primary pages and editors at 2.0× font scale | High | M |
| 3 | MAP-02 | Move points/routes to geographic sources/layers and verify the full zoom range | Medium | M |
| 4 | VIS-01 | Keep only Add as a FAB while Map/Place library remain visible secondary icons | Medium | M |
| 5 | MAP-01 | Make attribution and viewport avoid system insets | Medium | S |
| 6 | A11Y-02 | Define and verify final-composite favourite legibility | Medium | S–M |
| 7 | FBK-01 | Keep sync failures persistent with enough diagnostic detail | Medium | S–M |
| 8 | DS-01 | Create role-based opacity/scrim rules | Medium | S–M |
| 9 | UX-02 | Reduce QR-card action competition without adding text labels | Medium | S–M |
| 10 | INPUT-01 | Add one-line editor overflow feedback; keep track-card height fixed | Medium | S |
| 11 | PLAT-02 | Re-enable and test predictive back | Medium | M |
| 12 | PLAT-01 | Define Medium and Expanded content structures | Medium | L |
| 13 | DS-02 | Consolidate durable component tokens | Low | S |
| 14 | TASTE-01 | Refine surface hierarchy without changing the visual world | Low | S–M |

## 8. What is working, and what was deliberately left alone

What is working: the app uses a real Material 3 theme, dynamic and static schemes, consistent shapes, a four-destination navigation model, clear dedicated editor routes, non-colour sync labels, delete confirmations, TalkBack reorder actions, and sensible maximum content widths. The journey band, candy containers, map ground, centred avatar, and QR presentation mode all feel specific to Been.

Left alone: the accepted Dynamic Soft Candy/Night Fog direction, city-map background, lack of visible checklist sort controls, card-wide track edit target, fixed one-line track-card height, hidden long-press favourite, combined personal/backup page, restrained empty states, and system typography. These are explicit personal-tool tradeoffs and are not defects on first-use discoverability or mainstream comprehension grounds.

## 9. Open questions and remaining verification

- Does the current APK exactly match the dirty workspace? Rebuild/install before accepting screenshot fixes.
- Capture Compact, Medium, and Expanded screenshots at 1.0× and 2.0×.
- Independently reproduce and record point/line disappearance or displacement at zoom 4/8/12/16/19 and after rapid pinch, pan, and rotation; until then MAP-02 remains owner-reproduced.
- Exercise offline, permission-denied, BLE failure, backup failure, and import-restore states.
- Favourite and Place-library UI/interaction are confirmed product signatures; their migration and BLE/sync semantics still require separate approval when protocol work is in scope.
