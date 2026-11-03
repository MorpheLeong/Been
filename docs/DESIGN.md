[简体中文](DESIGN.zh_CN.md) · **English**

---
name: BugChecking Android
description: Normative UI and interaction rules for the BugChecking Android companion app.
colors:
  action-neon: "#53FF9A"
  on-action-neon: "#00351D"
  device-disconnected: "#007A49"
  journey-light: "#C3004F"
  journey-dark: "#FF5A86"
  light-canvas: "#FBE8DB"
  light-card: "#F5D9D5"
  dark-canvas: "#121212"
  dark-on-surface: "#E8BDC8"
typography:
  family: Android system sans
  headline: Material 3 headlineSmall semibold
  title: Material 3 titleLarge/titleMedium medium-to-semibold
  body: Material 3 bodyLarge/bodyMedium/bodySmall
  label: Material 3 labelLarge/labelMedium/labelSmall
rounded:
  small: 8dp
  medium: 12dp
  large: 16dp
  extra-large: 28dp
spacing:
  xxs: 4dp
  xs: 8dp
  sm: 12dp
  md: 16dp
  lg: 24dp
  xl: 32dp
components:
  page-horizontal-inset-compact: 16dp
  page-horizontal-inset-medium: 24dp
  minimum-touch-target: 48dp
  form-max-width: 720dp
  list-max-width: 840dp
---

# BugChecking Android design system

This document is normative for new Android UI and for changes to existing screens. It records the accepted product direction and the constraints future work must preserve. The audit report [BugChecking Android UI/UX audit — 2026-10-28](ux-audit-android-2026-10-28.md) records current compliance gaps; a rule appearing here does not claim that every current screen already passes it.

## Overview

**Creative North Star: "A journey atlas softened for everyday use."**

BugChecking is a personal Android companion for preparing trips, managing check-in content, recording real visits, and synchronising identity and selected content to the BugChecking device. Its current audience is the product owner/developer, who knows its hidden gestures and data model; first-use conversion and zero learning cost are not goals. It is an Operate surface with Explore/Play qualities: functional accuracy, visual identity, and interaction delight outrank explanatory copy and explicit labels.

The visual world combines a centred-crop halftone city map, a pale-apricot light canvas, rose-peach surfaces, and charcoal/grey-pink dark surfaces. Neon green and candy red are scarce functional accents rather than component defaults. The map is a quiet geographic memory, not the content itself. Cards and controls sit above it as readable, task-oriented layers. Brand character comes from the journey band, map texture, warm surfaces, and precise motion—not from ornamental gradients, glows, or novel controls.

**Key characteristics:**

- Recognisable city-map ground with role-based translucent surfaces.
- A glare-reduced pale-apricot canvas with warm cards in light mode; charcoal canvas and grey-pink text in dark mode.
- A restrained Material 3 component vocabulary and Material icon family.
- One dominant action per view. Map and Place library remain persistently visible signature actions on Tracks, with lower emphasis than Add.
- Purposeful state motion, never decorative choreography.
- Dark mode, long content, map zoom, and system insets never cause unreadable, displaced, or geographically incorrect output.

**The Preserve-the-Atlas Rule.** Refinement may adjust opacity, hierarchy, and contrast, but must not remove the map identity, warm candy system, charcoal/grey-pink dark system, or journey band without explicit product approval.

**The Operate-First Rule.** When expression competes with reading, target access, error recovery, or Android conventions, the task wins.

**The Personal-Tool Rule.** Do not add persistent tutorials, helper paragraphs, or text buttons for hypothetical newcomers. Explicit explanation is reserved for recovery, irreversible/high-cost actions, permission causes, or ambiguity that state alone cannot resolve.

## Colors

### Architecture

Colour follows three layers:

1. **Primitive palette:** the named static colours in the frontmatter and the complete light/dark tonal values in `PassportDesign.kt`.
2. **Semantic roles:** `MaterialTheme.colorScheme` roles, including `primary`, `secondary`, `tertiary`, their containers and `on*` partners, the surface-container ladder, `outline`, `outlineVariant`, and `error`.
3. **Component roles:** primary-page chrome, navigation, card, summary, selection, favourite tiers, and map markers. Component roles must resolve through semantic roles unless a fixed product colour is required and documented.

### Accepted UI-U1 contracts

| ID | Accepted resolution |
| --- | --- |
| `content/on-image-surface` | Transparent root scaffolds explicitly set `MaterialTheme.colorScheme.onSurface` as content colour. |
| `surface/form-opaque` | Dense editor/form surfaces resolve through opaque `MaterialTheme.colorScheme.surface` or stable surface roles; they do not inherit translucent primary-page card/chrome treatment. |
| `layout/editor-bottom-reserve` | Scroll content reserves the measured fixed-action height plus navigation/IME insets. The action host is transparent and does not add an outer tonal Surface. |
| `layout/safe-drawing-bottom` | Map attribution and edge controls consume horizontal and bottom `safeDrawing`/system-navigation insets. |
| `map/attribution-scrim` | Attribution uses `surface.copy(alpha = 0.88f)` with matching `onSurface`, 8 dp shape, 1 dp tonal elevation, padding, and safe-drawing awareness. |

These BugChecking project contracts were accepted through the UI-U1 PLZ110 device matrix on 2026-10-29. They do not promote the unresolved map-geometry candidates owned by UI-U2.

### Accepted UI-U2 contracts

| ID | Accepted resolution |
| --- | --- |
| `map/route-line-color` | Route lines resolve to `MaterialTheme.colorScheme.primary`. |
| `map/route-line-width` | The renderer `LineLayer` uses 4f style units with round caps and joins. |
| `map/point-size` | Ordinary points use renderer circles with a 9f style-unit radius. |
| `map/point-outline-width` | Ordinary points use a 2.5f style-unit semantic-surface outline; the 36 dp favourite-star bitmap keeps an equivalent 2.5 dp outline. |
| `map/favourite-point-colors` | Favourite tiers 1–3 use the established `trackFavoriteMapColor` semantic colours and a consistent 36 dp five-point star. |
| `motion/track-focus-cue` | After scrolling, the target card shows a 2 dp primary outline for 1.8 seconds and fades it over 450 ms. |
| Renderer-coordinate contract | MapLibre GeoJSON sources/layers own points and routes; UI code does not reimplement projection or rebuild annotations after camera idle. |

These BugChecking project contracts passed the UI-U2 PLZ110 and owner acceptance on 2026-10-30. `map/cluster-radius` and `map/cluster-zoom-thresholds` remain `proposed` because native clustering compatibility is deferred.

### Accepted UI-U3 contracts

| ID | Accepted resolution |
| --- | --- |
| `action/track-primary-fab` | Add Track is the only FAB on Tracks and uses the standard Material 3 primary FAB role. Map and Place library never reuse its complete size, tone, and elevation combination. |
| `action/track-signature-icon` | Map and Place library share one persistent secondary action strip using `secondaryContainer` / `onSecondaryContainer`, 1 dp tonal elevation, and one 48 × 48 dp IconButton target per action. Its accepted UI-U3 16 dp shape is superseded by UI-U4's accepted semantic shape; action hierarchy and target size are unchanged. |
| `layout/track-action-gap` | The signature strip starts 8 dp below the Tracks content edge; it occupies layout space rather than floating over cards. |
| `layout/track-action-safe-reserve` | The Tracks list reserves 104 dp at the bottom so its final card can clear the sole primary FAB; the signature strip requires no overlay reserve because it is outside the scroll viewport. |
| `track-card/title-max-lines` | Track-card place names use exactly one line. |
| `track-card/title-overflow` | Overflow is trailing ellipsis; automatic wrapping and height changes are forbidden. |

These BugChecking project contracts passed the PLZ110 1.0× / 2.0× font matrix and owner acceptance on 2026-10-30. UI-U4 is unlocked; these rules remain project-scoped rather than becoming a cross-project personal preference.

The accepted product intent—Add as the sole FAB, Map/Place library persistently visible, and equal-height one-line cards—remains binding. The earlier in-flow placement of the signature strip is superseded by the scrimless floating-shell contract below, which passed owner acceptance with UI-U4 on 2026-11-04.

### Accepted UI-U3.5 / UI-U4 colour and primary-shell rules

UI-U3.5 uses a fixed brand palette and no longer derives colour from Android wallpaper. The following rules entered the UI-U4 device matrix and received explicit owner acceptance with UI-U4 on 2026-11-04. Their state is BugChecking-project `accepted`; they do not automatically become cross-project personal preferences:

| ID | Current definition |
| --- | --- |
| `color/action-neon` | Neon green `#53FF9A` is a scarce action/status accent. A Button, TonalButton, Slider, or icon does not receive it merely because of component type. |
| `color/device-disconnected` | The floating device action uses deep green `#007A49` when disconnected; connecting, connected, syncing, and sync-ready icons use neon green. Its neutral container never inherits the accent. The device entry outranks the FAB. |
| `color/journey` | Journey lines and checklist progress use candy red: light `#C3004F`, dark `#FF5A86`. UI-U2 geometry remains accepted; UI-U3.5 only replaces the old primary colour mapping with this journey role. |
| `surface/light-canvas` | The light canvas is pale apricot `#FBE8DB`; cards, chrome, and forms use adjacent rose-peach surface steps. Never change only the canvas and leave other surfaces as pale islands. |
| `surface/dark-canvas` | The dark canvas is `#121212` with grey-pink semantic text/borders; dark is not a mechanical inversion of light. |
| `action/editor-helper-neutral` | Track editor Add/Edit helpers—Expand/Find, Map and Link icons, Relocate, and their loading feedback—use `onSurfaceVariant` or neutral surfaces, not neon green. |
| `card/checklist-progress` | The completion card uses the normal card surface; progress uses the journey role and a neutral track. |
| `card/qr-star` | Every QR card uses the normal card surface; only the active star retains neon green. Inactive star, Edit, and Delete remain neutral. |
| `action/profile-backup-neutral` | Change avatar, Save profile, the volume slider, Change, and Back up now remain neutral rather than neon green. |
| `layout/primary-floating-actions` | Primary pages have no solid top app bar or page title. A scrimless overlay aligns 48 dp Profile, signature, and device controls 8 dp below the safe top inset; uncovered regions neither obscure nor intercept content. Initial content clears controls through list content padding, while scrolled content may still pass behind them. |
| `navigation/profile-avatar` | Compact/rail navigation contains Tracks, Checklist, and QR. The circular avatar opens Me as an independent secondary page; Me hides bottom/rail navigation and replaces the avatar with a top-left Back action. |
| `motion/device-capsule` | The neutral 48 dp device control keeps its right edge fixed. Status text expands left, enters in 320 ms, exits in 260 ms, ellipsises before the centred signature strip, and exposes the full message through a polite live region. |

**Operational importance first.** Rank core state/actions, signature states, and ordinary helpers from the real task before assigning colour. A Material component variant defines structure and behaviour; it does not automatically define product emphasis.

**One green per role.** Do not place several near-green hues at the same hierarchy on one screen. If status needs differentiation, name the semantic role and keep a non-colour cue. Current green roles are limited to neon action/status emphasis and disconnected deep green.

### Light mode

- Pale apricot `#FBE8DB` is the canvas; cards, top/bottom chrome, and forms resolve through the rose-peach surface ladder.
- Neon green is reserved for the high-value actions/states explicitly listed in UI-U3.5, never as a default ordinary-control colour.
- Candy red carries the journey line, track points, and checklist progress; it is not the generic error role.
- Any canvas change must recheck the composed map mask, journey line, cards, and chrome. Never replace only the background.

### Dark mode

- Dark mode uses the `#121212` charcoal canvas and adjacent neutral surfaces; text and boundaries use grey-pink semantic roles.
- Neon green and candy red keep the same functional ownership as light mode; semantics are not reassigned by visual brightness.
- Page-level content on a transparent scaffold must explicitly inherit `onSurface`; never rely on an ambient default.
- Each container continues to use its paired `on*` or surface foreground; do not mechanically invert the light theme.

### Map and translucency

- The city map is centred and cropped. It may be desaturated and dimmed by theme.
- Translucency is defined by role, not per-screen taste: top/bottom chrome, routine list card, summary/selected card, and form container are separate roles.
- Map attribution, zoom/location controls, and the viewport consume `safeDrawing`/navigation-bar insets. Attribution never hugs the gesture bar, sits under map labels, or clips at an edge.
- Track points and route lines use geographic sources/layers owned by the map renderer so they stay anchored through zoom, rotation, and pan. Do not depend on clearing and rebuilding screen annotations after camera-idle events to maintain position.
- Ordinary track points use circles. Favourite places always use five-point stars on the map, coloured by the existing favourite-tier semantics. The star is a renderer `SymbolLayer` icon bound to the same geographic point feature; never reintroduce a screen-space annotation solely to change its shape.
- Tapping an ordinary point or favourite star opens a map-detail sheet containing only the place name, check-in date/time, and one primary action: “Locate in Tracks”. That action returns to Tracks and scrolls to the matching card. The sheet does not repeat accuracy or expose Edit/external-map actions.
- After the scroll completes, the target card shows a 2 dp primary-colour outline for 1.8 seconds, then fades it over 450 ms. The cue must not change card size, surface colour, favourite tier, or click behaviour, and it adds no toast or explanatory copy.
- Dense forms use a more opaque container or local scrim than sparse lists.
- Final composited text/background pairs must meet 4.5:1 for normal text and 3:1 for large text and UI boundaries. Checking only the uncomposited hex pair is insufficient.
- Do not lower normal-size supporting text opacity unless the final composite remains at least 4.5:1.

**The Paired-Colour Rule.** A container uses its matching `on*` colour. Arbitrary cross-pairing and alpha-faded substitute ink are prohibited.

**The Map-Is-Memory Rule.** The map may be noticed after the content, never before it. If a field, label, or status competes with the map, strengthen its local surface rather than removing the map globally.

## Typography

Use the Android system sans and Material 3 type scale. Do not introduce a display font into product UI. The default system typeface is intentional and supports Chinese, Latin, numbers, accessibility scaling, and platform familiarity.

- `headlineSmall`: major in-page headings when a top app bar is not the only title.
- `titleLarge`: secondary page and sheet titles.
- `titleMedium`: section headers, card titles, and date-group labels.
- `titleSmall`: compact subsection labels inside settings/forms.
- `bodyLarge`: editable values and primary row content.
- `bodyMedium`: normal explanatory content.
- `bodySmall`: supporting facts and helper text, only when contrast remains AA.
- `labelLarge`: buttons and prominent compact values.
- `labelMedium`/`labelSmall`: status, compact metadata, and secondary captions.

Use weight before colour/opacity to strengthen hierarchy. Avoid per-call `FontWeight` overrides when the intended role can be represented in `PassportTypography`.

Text must reflow at 2.0× without clipping, overlapping, or hiding controls. Keep atomic value/unit content together; if a supporting row cannot fit, recompose it into two deliberate lines instead of leaving an orphaned unit. Titles and labels may wrap unless a measured, user-safe ellipsis rule exists.

Track-card place names are an explicit exception: keep one line with trailing ellipsis and never wrap in a way that changes card height. If a marquee is offered, start it only on focus, hold, or explicit selection; never run passive continuous marquee, and respect reduced-motion settings.

**The Role-Before-Size Rule.** Select the semantic type role first. One-off font sizes are not permitted in page code.

**The Two-Lines-Are-Better-Than-Damage Rule.** On large fonts, a deliberate second line is preferable to clipped labels, overlapping actions, or unreadable ellipsis.

## Layout

### Window classes

- **Compact (<600 dp):** bottom NavigationBar, single pane, 16 dp horizontal page inset.
- **Medium (600–839 dp):** NavigationRail, single constrained pane, 24 dp inset. Forms remain at or below 720 dp; lists remain at or below 840 dp.
- **Expanded (>=840 dp):** NavigationRail plus list-detail or supporting-pane layout where context benefits. Do not merely centre a phone-width page when a second pane improves the task.
- Foldables must avoid placing primary interaction across a hinge. Expanded support is not considered complete without hinge-aware verification.

Primary routes share one shell geometry. Initial content clears the floating top-action row through the scrolling container's own content padding; after scrolling, content remains visible in every uncovered transparent area and may pass behind the buttons. The shell must not create solid top/bottom bands with whole-viewport padding. Compact uses a 64 dp floating bottom group: a three-destination custom compact navigation container takes the remaining width, a 64 × 64 dp Add control sits to its right, and the gap is 8 dp. Top floating entries, the bottom floating group, and independent-page corner actions share a fixed 16 dp content-edge inset in addition to system `safeDrawing`, aligning avatar/back with date-heading/navigation edges and device status with track-card/Add edges. On PLZ110 the physical container edge sits about 56 px from the screen; outward shadow is excluded from alignment. The bottom group sits 12 dp above navigation insets. The navigation shell has 4 dp inner padding; each of its three equal-width items is 56 dp high, uses a 24 dp icon and 2 dp icon-label gap, and wraps icon plus one-line label in the selected container. Lists reserve only enough trailing space for the actual floating group, never a full-width bottom bar.

Independent pages have no title or solid app bar. A transparent four-corner layer assigns Back at top-start, contextual action at top-end, confirm/next at bottom-start, and destructive action at bottom-end; a corner is absent when it has no action. Controls are 48 dp circles and bottom controls clear navigation/IME insets. The page canvas always fills the window; initial clearance belongs to scrolling content padding, and scrolled content may enter every transparent gap and pass behind the controls, while only the control surfaces obscure and intercept input. At 2.0× font scale and with the keyboard open, the last field, helper, error, and section heading still scroll above them.

The app window is edge-to-edge with transparent status/navigation bars and without Android's forced navigation-bar contrast scrim. Page colour continues into the gesture region, and system icon/gesture-handle appearance follows the active light/dark theme.

Use the 4/8/12/16/24/32 dp scale for layout rhythm. Add component tokens only for repeated anatomy such as page inset, card padding, avatar size, bottom content reserve, and icon size. Do not tokenise incidental measurements or replace Material component defaults without cause.

### Navigation and flows

- Bottom/rail destinations are Tracks, Checklist, and QR codes. The top-left avatar opens Me and replaces its former bottom/rail destination.
- Primary navigation preserves tab, scroll, and workflow state.
- System back and top-app-bar back must agree; predictive back is required for supported secondary routes.
- Scrolling forms, asynchronous parsing, multi-step work, large lists, destructive administration, and state restoration belong on dedicated pages.
- AlertDialog is limited to a one-field quick add/edit, delete/irreversible confirmation, or a very short choice.
- ModalBottomSheet is for contextual details or short selections that do not need independent restoration.

**The One-Primary-Action Rule.** A screen has one visually primary action. Multiple same-size FABs are not allowed. Tracks keeps Add as its only FAB while Map and Place library remain as two persistent, easy-to-hit, lower-emphasis icon actions on the main screen; do not hide them in overflow or require text labels.

**The Dedicated-State Rule.** If leaving and returning must preserve meaningful work, the flow is a page, not a large dialog or an embedded settings section.

## Elevation & Depth

Depth is primarily tonal. Routine cards are flat or low-tonal surfaces; state differences prefer content, icons, and explicit semantic accents rather than tinting a whole card by default; navigation, sheets, dialogs, and a dragged checklist item may rise above them.

Shadows are not the default separation mechanism. They may protect a floating/dragged element from a busy map but must not substitute for the surface-container ladder. Top/corner floating controls and floating navigation use 4 dp shadow elevation; the FAB uses 6 dp at rest and 8 dp when pressed/focused. In dark mode these floating controls share a 1 dp light rim derived from low-alpha `onSurface`, so they remain legible while moving over map content. Light mode suppresses that rim and relies on container-tone separation plus shadow for cleaner, more translucent edges. Floating navigation may own a distinct navigation-container tone, but that tone must not spread to routine cards. Routine cards receive no extra shadow, and dark mode still relies primarily on the charcoal surface ladder.

Motion communicates state:

- Detail enter: short decelerating slide/fade.
- Detail exit: shorter accelerating slide/fade.
- Checklist completion and reorder: restrained spring with haptic feedback.
- Syncing: a continuous icon rotation only while work is active.
- Respect Android animator-duration settings and avoid ambient or page-load choreography.

**The Tonal-First Rule.** Use container tone to establish resting elevation; reserve shadow/elevation changes for movement, focus, or modal separation.

## Shapes

Shape uses a semantic hierarchy anchored by the device screen radius:

- Android 12+ uses the smallest non-zero current-window corner radius converted to dp; a square display uses 0 dp and older Android falls back to 16 dp.
- `cardShape` uses the runtime screen radius for cards, dialogs, and sheets. `fieldShape` is fixed at 16 dp for text/read-only fields and selectors, and `buttonShape` is fixed at 24 dp for ordinary text buttons. Floating navigation uses the concentric radius `clamp(screen radius - 16 dp, 16 dp, height / 2)`. The 64 dp Add control, avatar/back, and corner actions are explicitly circular; device status, the Map/Place-library group, and navigation selection are half-height capsules.
- `MaterialTheme.shapes` maps small roles to fields, medium to buttons, and large roles to card shells. Page code must call the matching semantic token/shared component and may not construct local rounded shapes or bypass tokens through a generic theme shape.
- Standard card content uses 24 dp vertical padding and horizontal padding `max(24 dp, screen radius / 2)`; compact list cards use the same horizontal safety inset with 16 dp vertical padding.
- Circular avatars, corner actions, and journey nodes remain `CircleShape`; chips and progress tracks use the semantic full token.
- `PassportPalette` may be replaced at runtime for future colour customisation. Shape, border, and component dimensions are not user-colour settings.

This UI-U4 shape hierarchy and its code guard received explicit owner acceptance on 2026-11-04 and are `accepted`. Evidence includes PLZ110 light/dark, gesture/three-button navigation, IME clearance, 16 dp (56 px) edge alignment, the 29.7 dp concentric navigation radius, circular Add, plus passing token guard, unit tests, lint, APK assembly, and `git diff --check`.

Journey-band nodes and connectors are reserved for genuinely ordered track content. Do not reuse them as decoration, badges, or generic list bullets.

**The Ordered-Content Rule.** Numbers, nodes, and connectors imply sequence. They appear only when the data has a real order.

## Components

### App shell

The shell owns the map background, scrimless floating action overlay, floating bottom bar or rail, device status, max-width content boundary, and primary action slot. Neither primary nor independent pages use full-width top chrome or page titles; independent routes use the four-corner action layer. The current bottom/rail destination is communicated by icon, label, and selected container—not colour alone.

Device status distinguishes disconnected, saved locally, pending, syncing, synced, and retryable failure. Success may be transient. Failure remains visible until resolved or dismissed and exposes its full message.

### Buttons and actions

- Filled Button: one constructive primary action.
- Filled Tonal Button: important secondary action.
- Outlined Button: tertiary or alternative action.
- Text Button: dialog and low-emphasis action.
- Icon Button: familiar compact action with a precise content description; destructive actions are separated from routine actions.
- FAB: at most one primary floating action per screen. On Tracks, Map and Place library use persistent secondary icon controls with a different size, tone, or elevation from Add.
- A fixed bottom Save/Add action uses a transparent inset-owning host. Do not wrap it in a full-width tonal tray; the button itself provides the action emphasis while navigation-bar and IME clearance remain intact.

Every action supports default, pressed, focused, disabled, loading when applicable, success/failure feedback, and a minimum 48 × 48 dp target.

### Cards and rows

Routine cards use the card surface role and device screen radius. Summary/selected cards use their semantic container roles. Card content observes the shared curve-safe inset rather than legacy 8/16 dp padding. A whole-card click has one primary meaning; adjacent inline actions must not compete with it. Track-card place names always use one-line trailing ellipsis, and card height never changes with name length; stars and text move inward together while journey-node alignment remains unchanged. Other long names follow their documented overflow rule.

Track cards align to their journey-band node midpoint. For each date group, the first item connects down only, middle items connect both ways, and the last connects up only. The whole card opens editing; do not add a duplicate edit icon.

### Checklist

Checklist groups are Pending and Completed. Do not show ordinal numbers or visible sort buttons. Long-press reorder stays within its group, persists on release, and retains TalkBack Move up / Move down custom actions. Completion feedback uses a bounded, restrained spring.

### Forms

Forms live inside a contained surface with a maximum width of 720 dp. Labels remain visible while values are entered. Helper/error text sits next to its field and uses semantic colour plus text/icon, never colour alone. Tapping blank space may clear focus without consuming child gestures.

Track add/edit app bars contain the page title only. Record mode, date/time, and precision are not pseudo-subtitles; precision is shown beside the coordinate group and may also remain on track cards.

Real names, URLs, and identifiers have an explicit overflow policy. Editable long names preferably grow to two or three lines. If a field must remain single-line, it scrolls horizontally while focused, uses deliberate ellipsis while unfocused, and exposes the full value to accessibility. Silent hard clipping at a field edge is forbidden.

Destructive restore/import shows a review summary before replacement. Long administration flows such as backup management use dedicated pages; the Me page may show a summary link.

### Empty, loading, and error states

Each data view defines loading, day-one empty, filtered empty, error, offline, and overflow behaviour when applicable. For this personal tool, empty states prefer a quiet icon or one short line rather than explanatory paragraphs and labelled CTAs; add an action only when no obvious entry exists or the next step has cost. Errors retain enough diagnostic detail.

### Favourite tiers

Favourite tiers and Place library are signature parts of the Tracks experience, while their data migration, BLE/sync meaning, and compatibility still require separate approval. Timed long press is the explicitly accepted primary favourite interaction: it may remain hidden and novel without a persistent text entry or first-use hint. Once holding begins, tier feedback must be timely, controllable, and must not accidentally open editing. Tiers 1–3 use one candy-red progression—peach blush, candy pink, raspberry—in track cards, pulse fills/outlines, Place-library filters/cards/stars, and map stars; interaction neon green is not a favourite-tier colour. When a track first creates or joins a saved place, all unlinked historical tracks that match that place without ambiguity are linked immediately and inherit its tier. Place library itself remains visibly reachable from Tracks.

## Do's and Don'ts

### Do

- **Do** preserve the warm-candy/charcoal-grey-pink system, city map, and journey band; keep UI-U3.5 explicitly `proposed` until acceptance.
- **Do** use `MaterialTheme.colorScheme`, typography, and shapes as the semantic source of truth.
- **Do** establish explicit `onSurface` content colour over transparent scaffolds.
- **Do** keep system bars edge-to-edge and theme-adaptive, and keep fixed bottom action hosts transparent while preserving navigation/IME insets.
- **Do** validate final composited colours, not only opaque token pairs.
- **Do** test 1.0× and 2.0× fonts, light/dark themes, the full map zoom range, and Compact/Medium/Expanded widths.
- **Do** keep Map and Place library persistently visible while Add remains the only primary action.
- **Do** default to less explanatory copy; let consistent icons, placement, and feedback teach the tool.
- **Do** keep real check-in coordinates unchanged when selecting a nearby place name; only the displayed name may change.
- **Do** use `BugChecking` for new external UI and documentation; keep legacy Passport naming only where compatibility/history requires it.
- **Do** use non-colour text/icon channels for sync, completion, selection, and error state.

### Don't

- **Don't** add a full-width tonal Surface behind bottom Save/Add buttons merely to own insets.
- **Don't** use an app-bar subtitle for status facts already represented by editable fields or nearby metadata.

- **Don't** restore a superseded palette or let routine components inherit neon green merely through default primary/secondary roles.
- **Don't** remove or redraw the city map as a generic gradient, glass wallpaper, or decorative illustration.
- **Don't** stack multiple equal-weight FABs or hide Map and Place library in overflow.
- **Don't** wrap track-card place names or let name length change card height.
- **Don't** add tutorials, helper paragraphs, and repeated button labels for hypothetical newcomers.
- **Don't** use opacity to create hierarchy when it causes text contrast to fail.
- **Don't** stretch phone layouts across tablets or place content across a fold hinge.
- **Don't** put stateful forms, backup administration, or multi-step import work into long dialogs.
- **Don't** add visible checklist sort controls or a duplicate track-card edit icon.
- **Don't** split Geocoder names by suffix without diagnostic evidence and targeted tests.
- **Don't** introduce new favourite/BLE/data-migration semantics through a visual-only change.

## Acceptance gates

A UI change is not complete until the relevant gates pass:

1. **Visual:** captures at Compact light/dark and 1.0×/2.0×; Medium/Expanded when layout is touched.
2. **Contrast:** normal text >=4.5:1, large text and UI boundaries >=3:1, measured or computed from the final composite.
3. **Operability:** 48 dp targets, state feedback, and existing TalkBack semantics do not regress without cause; first-use discoverability is not a gate for the hidden favourite gesture.
4. **State:** loading, empty, error, offline, overflow, disabled, and destructive confirmation as applicable.
5. **Navigation:** system back, top back, predictive back, state restoration, and tab preservation.
6. **Android validation:** `testDebugUnitTest`, `lintDebug` with zero errors, and `assembleDebug`.
7. **Device:** physical-device review on the target phone for any visual, gesture, inset, dynamic-colour, BLE-status, or keyboard change.
8. **Map geometry:** at minimum, middle, and maximum zoom—and after continuous zoom, pan, and rotation—every point and route line remains anchored to its geographic coordinate.
