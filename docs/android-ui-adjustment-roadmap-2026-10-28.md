[简体中文](android-ui-adjustment-roadmap-2026-10-28.zh_CN.md) · **English**

# Been Android UI adjustment roadmap and acceptance plan

Status: pre-implementation draft 0.1
Baseline: 2026-10-28
Inputs: [Design system](DESIGN.md) · [UI/UX audit](ux-audit-android-2026-10-28.md)

## 1. Goal and sequencing

This is targeted hardening, not a redesign. It preserves Dynamic Soft Candy/Night Fog, the city map, journey band, hidden favourite hold, and personal-tool density while fixing rendering correctness, legibility, action hierarchy, and token drift.

Priority order is geographic/data correctness, observed unreadable or clipped output, and then system polish. This iteration uses `UI-U0` through `UI-U5` so it cannot be confused with the historical Android Phases 0–3. Execution is strictly `UI-U0 → UI-U1 → UI-U2 → UI-U3 → UI-U4 → UI-U5`. Exploration may start early, but no next-stage merge, token promotion, or completion claim occurs before the owner explicitly says “UI-Ux accepted”.

## 2. Phase overview

| Phase | Goal | Main scope | Estimate | Dependency |
| --- | --- | --- | ---: | --- |
| UI-U0 | Freeze baseline and matrix | Captures, device state, data fixtures, candidate tokens | 0.5–1 day | None |
| UI-U1 | Fix visible legibility/insets | Dark content colour, 2.0× reflow, editor reserve, attribution | 1–2 days | UI-U0 accepted |
| UI-U2 | Stabilise map geometry | GeoJSON sources/layers, clustering, full zoom range | 2–4 days | UI-U1 accepted |
| UI-U3 | Rebalance Tracks actions | Add-only FAB; visible Map/Place library; equal-height cards | 1–2 days | UI-U2 accepted |
| UI-U4 | Converge the design system | Surface roles, favourite ink, status, QR actions, tokens | 2–3 days | UI-U3 accepted |
| UI-U5 | Platform and final regression | Back, window classes, full device matrix, personal standard | 1–3 days | UI-U4 accepted |

Estimated total: 7.5–15 effective development days. Medium/Expanded work may be explicitly deferred if this release targets only the current phone.

### Scope and deferral

| Class | Items | Rule |
| --- | --- | --- |
| Must pass | UI-U1 legibility/insets, UI-U2 map geometry, UI-U3 Tracks hierarchy, touched-screen consistency in UI-U4, PLZ110 regression in UI-U5 | Any FAIL blocks completion |
| Explicitly deferrable | Medium/Expanded, landscape, predictive back when support is not claimed, untouched-screen token cleanup | Record reason, risk, and follow-up; never report PASS |
| Out of scope | Favourite migration, BLE/sync semantics, Geocoder cleanup, destructive restore execution, avatar-crop redesign | Requires separate approval |

Conditional acceptance does not advance by default. The owner must explicitly say that the next stage may begin while naming the retained conditions; those rules remain `verified`, never `accepted` or memory-eligible.

### Fixed device matrix

UI-U0 records and freezes OS/API/build, IME, dynamic-colour wallpaper source, and APK commit.

| Dimension | Required |
| --- | --- |
| Device | PLZ110, 1216×2640, 560 dpi, portrait, `zh-CN` |
| Theme | Fixed-wallpaper dynamic light/dark; static-fallback light/dark |
| Font | 1.0× and 2.0× on every touched page; 1.3×/1.5× on Tracks, Me, Track edit, and QR edit |
| Navigation | Gesture on every touched page; three-button on Map and bottom-action editors |
| IME | The same IME recorded in UI-U0; Track edit, QR edit, Smart import |
| Map | zoom 4/8/12/16/19; ten rapid pinches; continuous pan, rotate, refit; load failure and recovery |

Landscape, tablet/foldable, and complete TalkBack acceptance are not claimed in this iteration; existing semantics must not regress without cause.

### Token and contract governance

Each candidate records `ID`, `rule-class(E | P | B | C)`, `kind(token | component-contract | architecture-contract)`, scope, proposed value/behaviour, status, owner, call sites, evidence, and `supersedes`. Stable accepted tokens move into `DESIGN.md`.

- Tokens are reusable visual/dimensional values.
- Component contracts include behaviour such as `maxLines = 1` and are not mislabelled as numeric tokens.
- Architecture contracts include renderer ownership of geographic coordinates.
- Canonical state machine: `proposed` before implementation; `verified` after automated and fixed-device gates; `accepted` only after explicit owner acceptance; memory only afterward. Replaced items remain traceable as `superseded`.
- Iteration candidates default to B (a project-bounded contract); engineering floors use E. This UI gate never promotes an item directly to cross-project P; C/P promotion belongs to Gate M of the personal standard.

### Candidate register for this iteration

This is the single implementation index. Phase sections explain the intent; `DESIGN.md` receives only accepted entries. Values remain undecided until their phase gate rather than turning temporary implementation values into standards.

| Kind | Candidate IDs/contracts | Rule class | Phase | Current state |
| --- | --- | --- | --- | --- |
| UI-U1 content/surface | `content/on-image-surface`, `surface/form-opaque` | B | UI-U1 | `accepted` |
| UI-U4 content/surface | `surface/chrome`, `surface/list-card`, `surface/summary`, `surface/selected`, `surface/favourite-tier-*`, `ink/on-favourite-tier-*` | B | UI-U4 | `accepted` |
| UI-U1 layout | `layout/editor-bottom-reserve`, `layout/safe-drawing-bottom` | B | UI-U1 | `accepted` |
| UI-U3 layout | `layout/track-action-gap`, `layout/track-action-safe-reserve` | B | UI-U3 | `accepted` |
| UI-U4 primary shell | `layout/primary-floating-actions`, `navigation/profile-avatar`, `motion/device-capsule` | B | UI-U4 | `accepted` |
| UI-U1 map attribution | `map/attribution-scrim` | B | UI-U1 | `accepted` |
| Map geometry | `map/route-line-color`, `map/route-line-width`, `map/point-size`, `map/point-outline-width`, `map/favourite-point-colors` | B | UI-U2 | `accepted` |
| Map clustering | `map/cluster-radius`, `map/cluster-zoom-thresholds` | B | post-UI-U2 compatibility work | `proposed` |
| UI-U2 motion | `motion/track-focus-cue` | B | UI-U2 | `accepted` |
| UI-U3 action tokens | `action/track-primary-fab`, `action/track-signature-icon` | B | UI-U3 | `accepted` |
| UI-U4 action/motion tokens | `action/destructive-icon`, `motion/completion-spring`, `motion/favourite-hold-stage` | B | UI-U4 | `accepted` |
| Component contracts | `track-card/title-max-lines = 1`, `track-card/title-overflow = ellipsis` | B | UI-U3 | `accepted` |
| Architecture contract | Geographic coordinates belong to renderer sources/layers; UI code does not reimplement projection | E | UI-U2 | `accepted` |

## 3. Phase gates

### UI-U0 — Baseline and evidence

Execution record: [2026-10-29 UI-U0 baseline and evidence](ui-u0-baseline-2026-10-29.md).

Stage status: `ACCEPTED`. The owner explicitly confirmed “UI-U0 accepted” on 2026-10-29; the UI-U1 prerequisite is satisfied.

- Record APK commit/worktree (`git status --short --branch`), PLZ110 OS/build/resolution/dpi, locale, orientation, theme/wallpaper, font scale, navigation mode, IME, and exact short/long-name, favourite, date-group, QR, empty, and failure fixtures.
- Capture all primary pages plus track/QR add/edit, Place library/detail, and Track map.
- Prepare zoom 4/8/12/16/19, rapid pinch, pan, rotate, and refit scenarios.
- Acceptance: every finding has a reproducible state/action/expected result; map drift has reproducible steps or remains explicitly owner-reproduced.
- Memory: record only the accepted stage gate, frozen evidence, explicit deferrals, and the rule that NOT RUN is never PASS. UI-U0 promotes no visual token; all candidate-token states remain `proposed`.

### UI-U1 — Legibility, reflow, and safe areas

Execution record: [2026-10-29 UI-U1 implementation and acceptance checkpoint](ui-u1-implementation-2026-10-29.md).

Stage status: `ACCEPTED`. Dynamic and static-fallback light/dark, gesture and three-button navigation, 1.0×/2.0×, the named 1.3×/1.5× pages, and the remaining IME combinations pass; the automated gates were rerun on the final code. The owner explicitly confirmed “UI-U1 accepted” on 2026-10-29, so its five candidates are now `accepted` and UI-U2 is unlocked.

- Establish shared `onSurface`; fix 2.0× clipping and value/unit splits; reserve measured bottom-action + navigation/IME inset through a transparent action host with no outer tonal Surface; use edge-to-edge adaptive system bars; make map attribution and viewport respect safe drawing. Long editor values scroll while focused and use trailing ellipsis while unfocused; track cards never wrap.
- Acceptance: no unreadable headings, overlap, hard clipping, or hidden final fields at 1.0×/2.0× on every touched page and at 1.3×/1.5× on the four pages named by the fixed matrix; gesture and three-button navigation both pass; Android unit/lint/assemble gates pass.
- Accepted tokens: `content/on-image-surface`, `surface/form-opaque`, `layout/editor-bottom-reserve`, `layout/safe-drawing-bottom`, `map/attribution-scrim`. UI-U2 was subsequently accepted on 2026-10-30.

### UI-U2 — Map geometry correctness

Execution record: [2026-10-29 UI-U2 implementation checkpoint](ui-u2-implementation-2026-10-29.md).

Stage status: `ACCEPTED`. On 2026-10-30 the owner explicitly confirmed “UI-U2 acceptance complete”. Renderer-owned GeoJSON geometry, ordinary circles, favourite stars, the distilled map-detail sheet, Tracks-card navigation, and its 1.8-second location cue pass; UI-U3 is unlocked. Native clustered-source compatibility on the Android 17 PLZ110 remains separately deferred, so cluster tokens are outside this acceptance and stay `proposed`.

- Replace clear/rebuild annotations with MapLibre GeoJSON source + Line/Circle/Symbol layers. Coordinate data belongs to renderer sources; visual style and clustering never mutate coordinates.
- Keep refit and point selection; distil point detail to name, check-in time, and one Tracks-card location action.
- Accepted evidence combines the owner's full-range zoom observation with PLZ110 light/dark checks of renderer-owned points/routes, favourite stars, point selection, map-to-card navigation, and the timed focus cue. Native clustering, cluster conservation, the automated 2 px projection harness, and cleared-cache offline recovery remain accurately deferred rather than being re-labelled PASS.
- Accepted tokens/contracts: `map/route-line-color`, `map/route-line-width`, `map/point-size`, `map/point-outline-width`, `map/favourite-point-colors`, `motion/track-focus-cue` (2 dp primary outline, 1.8-second hold after scrolling, 450 ms fade), and the renderer-owned geographic-coordinate architecture contract. `map/cluster-radius` and `map/cluster-zoom-thresholds` remain `proposed` because native clustering is incompatible on the PLZ110 and must not be reported as passed.

### UI-U3 — Tracks signature hierarchy

Execution record: [2026-10-30 UI-U3 implementation and acceptance checkpoint](ui-u3-implementation-2026-10-30.md).

Stage status: `ACCEPTED`. On 2026-10-30 the owner explicitly confirmed “UI-U3 acceptance passed”. Add is the sole FAB; Map and Place library remain persistently visible in a compact secondary action strip that occupies layout space and never covers cards. UI-U4 is unlocked.

- Add uses the standard Material 3 primary FAB role. Map and Place library share a `secondaryContainer` / `onSecondaryContainer` action strip with 16 dp shape, 1 dp tonal elevation, and separate 48 × 48 dp targets.
- The action strip uses an 8 dp top gap. The scrolling list reserves 104 dp at the bottom for the sole FAB; short, long, and mixed-script card names remain one-line trailing ellipsis with identical measured heights.
- PLZ110 navigation checks passed Map, Place library, and card-to-editor routes. At 2.0× font scale, three visible fixture cards each measured 385 px high; font scale was restored to 1.0× after verification. The full Android unit/lint/assemble gate passed on the accepted APK.
- Accepted tokens/contracts: `action/track-primary-fab`, `action/track-signature-icon`, `layout/track-action-gap = 8 dp`, `layout/track-action-safe-reserve = 104 dp`, `track-card/title-max-lines = 1`, and `track-card/title-overflow = ellipsis`.

### UI-U4 — Cross-page consistency and tokens

Stage status: `ACCEPTED`. On 2026-11-04 the owner explicitly confirmed “UI-U4 accepted”. Token guard, unit tests, lint, APK assembly, and `git diff --check` passed. PLZ110 evidence covers light/dark, gesture/three-button navigation, IME, 16 dp (56 px) floating-edge alignment, the 29.7 dp concentric navigation radius, circular Add, and independent-page corner actions. UI-U5 is now unlocked but has not started or been accepted.

Before UI-U4, the owner approved a separate primary-shell increment. Its implementation removes the solid top app bar and primary-page titles, moves Me from bottom/rail navigation to a circular avatar, keeps Map/Place library centred on Tracks, and places the device action in a neutral right-anchored expanding capsule. The control row clears the safe top inset; initial Tracks, Checklist, and QR content clears it through scroll-content padding, while scrolled content may pass behind uncovered regions. Me opens as an independent secondary page with no bottom/rail navigation and a top-left Back action. Narrow layouts ellipsise status text rather than moving or hiding the centred signature actions. Floating controls/navigation use explicit shadow elevation without adding a solid chrome band. This increment was accepted with UI-U4 on 2026-11-04.

- Define chrome/list/summary/selected/favourite/form surface roles; validate final-composite favourite legibility; preserve diagnostic sync errors; reduce QR action competition without adding labels; consolidate only repeated stable values. The current worktree implements runtime palette/shape/border/component tokens, a 64 dp Compact floating navigation + 64 dp Add group, and titleless four-corner independent pages. Android 12+ reads the window radius (PLZ110: 160 px, about 45.7 dp); pre-31 falls back to 16 dp. Large shells retain that radius, while fields use 16 dp and ordinary text buttons use 24 dp. Card content uses a curve-safe horizontal inset of `max(24 dp, screen radius / 2)`. `verifyPassportUiTokens` is wired into Gradle `check` and rejects local rounded shapes or generic theme-shape bypasses. Automation, the PLZ110 matrix, and explicit owner acceptance make this implementation `accepted` as of 2026-11-04.
- Acceptance: roles resolve to the same tokens everywhere; action hierarchy is consistent; final composites are measured on the lightest/darkest map samples and every surface role (normal text >=4.5:1, large/UI boundaries >=3:1); destructive QR action is separated without requiring labels; inventory names owners and call sites. Cards and dialogs/sheets retain the runtime screen radius; PLZ110 floating navigation uses the concentric `45.7 - 16 = 29.7 dp` radius, Add is explicitly circular, and status/signature/selection controls are capsules. Fields stay 16 dp and text buttons stay 24 dp. Top, bottom, and independent-page corner controls align to the 16 dp content edge and clear gesture/three-button navigation and IME at 1.0×/2.0×.
- Candidate tokens: `surface/chrome`, `surface/list-card`, `surface/summary`, `surface/selected`, `surface/favourite-tier-*`, `ink/on-favourite-tier-*`, `action/destructive-icon`, `motion/completion-spring`, and `motion/favourite-hold-stage`.

### UI-U5 — Platform, final device review, and personal standard

- Verify system/top Back and state restoration; predictive Back is required only if this iteration claims support, otherwise record it as deferred. Test Medium/Expanded only if claimed. Run the complete light/dark, 1.0×/2.0×, IME, inset, map-dynamic, and cross-page matrix. Reconcile the [personal application standard](personal-app-development-standard.md).
- Follow the personal standard's dependency order and complete Gates A–G, I, K, and L before H. When permissions/data/migration are touched, J preflight occurs before E and is rechecked after device work; otherwise record why J is not applicable. Gate M is outside this iteration. Validate only UI colour/surface, layout/overflow, control hierarchy, motion, and external visual-resource rules. Data sync, firmware, and Tianse-beta candidates are not completion criteria.
- Acceptance: current-worktree APK and all automated gates PASS; every UI-U1–UI-U4 device item PASS; no regression to access for Tracks/Checklist/QR/Me (three navigation items plus avatar), journey band, the current palette, hidden favourite, visible Map/Place library, or equal-height cards. Completion requires explicit “UI-U5 accepted”.
- Every E/P/B rule applicable to this iteration traces to iteration evidence or an explicit owner statement; unvalidated preferences remain C candidates.

## 4. Acceptance record

```text
Phase (UI-Ux):
Build: PASS / FAIL / NOT RUN
Unit tests: PASS / FAIL / NOT RUN
Lint: PASS / FAIL / NOT RUN
Device: PASS / FAIL / NOT RUN
Device, theme, font, navigation mode:
Pages and states passed:
Failures and reproduction:
Deferred items and reason:
Owner result: accepted / conditionally accepted (does not advance by default) / rejected
Rules/tokens promoted:
Rules superseded:
```

## 5. Memory protocol

Implementation decisions start as `proposed`; automated and device gates make them `verified`; only explicit “UI-Ux accepted” makes them `accepted`. Then add one small note under `C:\Users\MPHNUC\.codex\memories\extensions\ad_hoc\notes\` containing `scope`, `applies_to`, evidence, accepted tokens/contracts, superseded rules, and non-generalisation boundaries. Been-specific decisions remain project-scoped. A preference becomes cross-project only after Been and Tianse beta both validate it, or the owner explicitly promotes it. The future personal development skill is created only after those rules are stable and must include triggers, counterexamples, a decision tree, acceptance templates, and product exceptions.

This plan itself is not written to memory before implementation and acceptance.
