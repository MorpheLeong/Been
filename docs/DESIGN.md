[简体中文](DESIGN.zh_CN.md) · **English**

---
name: BugChecking Android
description: Normative UI and interaction rules for the BugChecking Android companion app.
colors:
  route-blue: "#4B69A2"
  passport-ink: "#172A3A"
  candy-blue: "#D9E5FF"
  waypoint-mint: "#3A7566"
  candy-mint: "#C5F1DF"
  journey-peach: "#95566B"
  candy-peach: "#FFD9E3"
  candy-lemon: "#FFE8A3"
  mist-surface: "#FFF9F2"
  night-surface: "#202B36"
  night-on-surface: "#F0F4F8"
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

The visual world combines a centred-crop halftone city map with Material 3 Dynamic Soft Candy containers in light mode and Night Fog blue-grey surfaces in dark mode. The map is a quiet geographic memory, not the content itself. Cards and controls sit above it as readable, task-oriented layers. Brand character comes from the journey band, map texture, candy semantic containers, and precise motion—not from ornamental gradients, glows, or novel controls.

**Key characteristics:**

- Recognisable city-map ground with role-based translucent surfaces.
- Bright but calm candy containers; blue-grey Night Fog rather than pure black.
- A restrained Material 3 component vocabulary and Material icon family.
- One dominant action per view. Map and Place library remain persistently visible signature actions on Tracks, with lower emphasis than Add.
- Purposeful state motion, never decorative choreography.
- Dark mode, long content, map zoom, and system insets never cause unreadable, displaced, or geographically incorrect output.

**The Preserve-the-Atlas Rule.** Refinement may adjust opacity, hierarchy, and contrast, but must not remove the map identity, Dynamic Soft Candy, Night Fog, or journey band without explicit product approval.

**The Operate-First Rule.** When expression competes with reading, target access, error recovery, or Android conventions, the task wins.

**The Personal-Tool Rule.** Do not add persistent tutorials, helper paragraphs, or text buttons for hypothetical newcomers. Explicit explanation is reserved for recovery, irreversible/high-cost actions, permission causes, or ambiguity that state alone cannot resolve.

## Colors

### Architecture

Colour follows three layers:

1. **Primitive palette:** the named static colours in the frontmatter and the complete light/dark tonal values in `PassportDesign.kt`.
2. **Semantic roles:** `MaterialTheme.colorScheme` roles, including `primary`, `secondary`, `tertiary`, their containers and `on*` partners, the surface-container ladder, `outline`, `outlineVariant`, and `error`.
3. **Component roles:** primary-page chrome, navigation, card, summary, selection, favourite tiers, and map markers. Component roles must resolve through semantic roles unless a fixed product colour is required and documented.

### Light mode

- Route Blue is the action and journey accent.
- Candy Blue is the high-emphasis summary/primary container.
- Waypoint Mint and Candy Mint support secondary actions and place/favourite meaning.
- Journey Peach and Candy Peach are complementary accents, not generic decoration.
- Candy Lemon is reserved for the highest neutral surface step, not status warning.
- Mist Surface is the static fallback ground.

### Dark mode

- Night Fog uses tinted blue-grey surface steps from lowest through highest; never substitute pure black.
- `night-on-surface` and the defined on-container colours are mandatory for text and icons.
- Page-level content on a transparent scaffold must explicitly inherit `onSurface`; never rely on an ambient default.
- Dynamic dark colour may be softened toward the accepted Night Fog containers, but paired on-colours must remain valid.

### Map and translucency

- The city map is centred and cropped. It may be desaturated and dimmed by theme.
- Translucency is defined by role, not per-screen taste: top/bottom chrome, routine list card, summary/selected card, and form container are separate roles.
- Map attribution, zoom/location controls, and the viewport consume `safeDrawing`/navigation-bar insets. Attribution never hugs the gesture bar, sits under map labels, or clips at an edge.
- Track points and route lines use geographic sources/layers owned by the map renderer so they stay anchored through zoom, rotation, and pan. Do not depend on clearing and rebuilding screen annotations after camera-idle events to maintain position.
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

All four primary pages share the same main-content start and shell geometry. Content is top-aligned within its max width. Primary-page lists reserve space for the bottom bar and the one permitted floating action without relying on unexplained magic padding.

A secondary editor may pin its primary bottom action, but scroll content reserves the measured action-bar height plus navigation/IME insets. At 2.0× font scale and with the keyboard open, the last field, helper, error, and section heading must still scroll fully above the action.

Use the 4/8/12/16/24/32 dp scale for layout rhythm. Add component tokens only for repeated anatomy such as page inset, card padding, avatar size, bottom content reserve, and icon size. Do not tokenise incidental measurements or replace Material component defaults without cause.

### Navigation and flows

- Primary destinations are Tracks, Checklist, QR codes, and Me.
- Primary navigation preserves tab, scroll, and workflow state.
- System back and top-app-bar back must agree; predictive back is required for supported secondary routes.
- Scrolling forms, asynchronous parsing, multi-step work, large lists, destructive administration, and state restoration belong on dedicated pages.
- AlertDialog is limited to a one-field quick add/edit, delete/irreversible confirmation, or a very short choice.
- ModalBottomSheet is for contextual details or short selections that do not need independent restoration.

**The One-Primary-Action Rule.** A screen has one visually primary action. Multiple same-size FABs are not allowed. Tracks keeps Add as its only FAB while Map and Place library remain as two persistent, easy-to-hit, lower-emphasis icon actions on the main screen; do not hide them in overflow or require text labels.

**The Dedicated-State Rule.** If leaving and returning must preserve meaningful work, the flow is a page, not a large dialog or an embedded settings section.

## Elevation & Depth

Depth is primarily tonal. Routine cards are flat or low-tonal surfaces; selected/summary containers use a stronger semantic container; navigation, sheets, dialogs, and a dragged checklist item may rise above them.

Shadows are not the default separation mechanism. They may protect a floating/dragged element from a busy map but must not substitute for the surface-container ladder. Dark mode uses the same hierarchy through Night Fog tonal steps, not grey drop shadows.

Motion communicates state:

- Detail enter: short decelerating slide/fade.
- Detail exit: shorter accelerating slide/fade.
- Checklist completion and reorder: restrained spring with haptic feedback.
- Syncing: a continuous icon rotation only while work is active.
- Respect Android animator-duration settings and avoid ambient or page-load choreography.

**The Tonal-First Rule.** Use container tone to establish resting elevation; reserve shadow/elevation changes for movement, focus, or modal separation.

## Shapes

The shape language is softly rounded and systematic:

- Small (8 dp): compact controls and fields where the Material component does not supply its own shape.
- Medium (12 dp): routine cards and list containers.
- Large (16 dp): prominent cards, avatar frames, and elevated containers.
- Extra large (28 dp): dialogs, sheets, and large expressive containers.
- Full/pill: buttons, chips, progress tracks, and circular nodes only where their component semantics call for it.

Journey-band nodes and connectors are reserved for genuinely ordered track content. Do not reuse them as decoration, badges, or generic list bullets.

**The Ordered-Content Rule.** Numbers, nodes, and connectors imply sequence. They appear only when the data has a real order.

## Components

### App shell

The shell owns the map background, top app bar, bottom bar or rail, inline device status, max-width content boundary, and primary action slot. Top and bottom chrome use their named translucent roles. The current destination is communicated by icon, label, and selected container—not colour alone.

Device status distinguishes disconnected, saved locally, pending, syncing, synced, and retryable failure. Success may be transient. Failure remains visible until resolved or dismissed and exposes its full message.

### Buttons and actions

- Filled Button: one constructive primary action.
- Filled Tonal Button: important secondary action.
- Outlined Button: tertiary or alternative action.
- Text Button: dialog and low-emphasis action.
- Icon Button: familiar compact action with a precise content description; destructive actions are separated from routine actions.
- FAB: at most one primary floating action per screen. On Tracks, Map and Place library use persistent secondary icon controls with a different size, tone, or elevation from Add.

Every action supports default, pressed, focused, disabled, loading when applicable, success/failure feedback, and a minimum 48 × 48 dp target.

### Cards and rows

Routine cards use the card surface role and 12 dp shape. Summary/selected cards use their semantic container roles. A whole-card click has one primary meaning; adjacent inline actions must not compete with it. Track-card place names always use one-line trailing ellipsis, and card height never changes with name length; other long names follow their documented overflow rule.

Track cards align to their journey-band node midpoint. For each date group, the first item connects down only, middle items connect both ways, and the last connects up only. The whole card opens editing; do not add a duplicate edit icon.

### Checklist

Checklist groups are Pending and Completed. Do not show ordinal numbers or visible sort buttons. Long-press reorder stays within its group, persists on release, and retains TalkBack Move up / Move down custom actions. Completion feedback uses a bounded, restrained spring.

### Forms

Forms live inside a contained surface with a maximum width of 720 dp. Labels remain visible while values are entered. Helper/error text sits next to its field and uses semantic colour plus text/icon, never colour alone. Tapping blank space may clear focus without consuming child gestures.

Real names, URLs, and identifiers have an explicit overflow policy. Editable long names preferably grow to two or three lines. If a field must remain single-line, it scrolls horizontally while focused, uses deliberate ellipsis while unfocused, and exposes the full value to accessibility. Silent hard clipping at a field edge is forbidden.

Destructive restore/import shows a review summary before replacement. Long administration flows such as backup management use dedicated pages; the Me page may show a summary link.

### Empty, loading, and error states

Each data view defines loading, day-one empty, filtered empty, error, offline, and overflow behaviour when applicable. For this personal tool, empty states prefer a quiet icon or one short line rather than explanatory paragraphs and labelled CTAs; add an action only when no obvious entry exists or the next step has cost. Errors retain enough diagnostic detail.

### Favourite tiers

Favourite tiers and Place library are signature parts of the Tracks experience, while their data migration, BLE/sync meaning, and compatibility still require separate approval. Timed long press is the explicitly accepted primary favourite interaction: it may remain hidden and novel without a persistent text entry or first-use hint. Once holding begins, tier feedback must be timely, controllable, and must not accidentally open editing. Place library itself remains visibly reachable from Tracks.

## Do's and Don'ts

### Do

- **Do** preserve Dynamic Soft Candy, Night Fog, the city map, and the journey band.
- **Do** use `MaterialTheme.colorScheme`, typography, and shapes as the semantic source of truth.
- **Do** establish explicit `onSurface` content colour over transparent scaffolds.
- **Do** validate final composited colours, not only opaque token pairs.
- **Do** test 1.0× and 2.0× fonts, light/dark themes, the full map zoom range, and Compact/Medium/Expanded widths.
- **Do** keep Map and Place library persistently visible while Add remains the only primary action.
- **Do** default to less explanatory copy; let consistent icons, placement, and feedback teach the tool.
- **Do** keep real check-in coordinates unchanged when selecting a nearby place name; only the displayed name may change.
- **Do** use `BugChecking` for new external UI and documentation; keep legacy Passport naming only where compatibility/history requires it.
- **Do** use non-colour text/icon channels for sync, completion, selection, and error state.

### Don't

- **Don't** restore the superseded pre-acceptance palette or replace Night Fog with pure black.
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
