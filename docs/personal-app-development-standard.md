[简体中文](personal-app-development-standard.zh_CN.md) · **English**

# Personal application development standard

Status: candidate standard 0.2; BugChecking UI-U0 through UI-U3 are accepted, the fixed-colour and scrimless primary-shell increments await device acceptance, and UI-U4 is not accepted; cross-project preferences await Tianse beta
Audience: the product owner and collaborating developers/AI
Current evidence: BugChecking Android; not every rule is cross-project yet

## 1. Purpose and rule levels

This document defines both what makes an application feel authored by the owner and how requirements, design, implementation, and physical-device acceptance proceed. Personal-tool status permits learning cost and hidden interaction; it does not excuse incorrect function, unsafe data handling, unreadable output, or unstable platform behaviour.

Rules use four classes. A normative rule intended for `DESIGN`, memory, or a skill must enter the register below with class, scope, state, evidence, exceptions, promotion condition, and supersession. Other prose explains registered rules and does not silently become a separate standard:

- **E — Engineering floor:** mandatory everywhere and never waived by personal-tool status.
- **P — Confirmed personal preference:** explicitly promoted by the owner or stable in at least two projects, with its applicable mode stated.
- **B — Project-bounded contract:** applies only to one named project. Current B entries belong to BugChecking; Tianse may create its own B entries. B never generalises by default.
- **C — Candidate personal preference:** a cross-project hypothesis awaiting Tianse beta evidence or explicit owner promotion; failure may leave it as C or become a B rule for a named project.

Class and state are orthogonal. A C rule may be `accepted` for one experiment and remain only a candidate; `verified` means agreed automated/device evidence passed, while `accepted` additionally requires explicit owner confirmation. Conditional acceptance remains `verified`; `superseded` is terminal but traceable. C-to-P promotion is a separate Gate M decision, not part of single-project acceptance.

Current P rules include minimal explanatory copy, icon-first low-cost actions, recoverable hidden gestures, keeping signature secondary actions visible, and assigning colour emphasis by real operational importance rather than component names or frontend nesting. Exact colours and the BugChecking visual world remain B; bright-soft colour, chromatic dark surfaces, and system-font preference remain C.

**Non-waivable rule.** Personal-tool mode may change guidance density, expression, and interaction visibility. It never lowers data correctness, recoverability, permission/privacy security, platform stability, hardware safety, or necessary operability.

### 1.1 Current normative register

| ID | Rule summary | Class/state | Scope and evidence | Promotion/exception |
| --- | --- | --- | --- | --- |
| E-01 | Personal-tool mode never waives correctness, recovery, safety, stability, or necessary feedback | E / `accepted` | Universal; explicit owner statements and rework review | No product-mode exception |
| E-02 | Coordinates, order, units, time, and state remain truthful | E / `accepted` | Universal; map and Geocoder review | Presentation may simplify; facts may not change |
| E-03 | Code/screenshots do not replace dynamic device and traceable-artifact evidence | E / `accepted` | Universal; map miss and prior device rework | None |
| E-04 | Irreversible changes, migration, protocol, and compatibility require layered separate approval | E / `accepted` | Universal; favourite scope and rename reviews | UI acceptance never approves another layer |
| E-05 | Record and preserve user worktree changes before implementation | E / `accepted` | Universal collaboration floor | Stop and report an unavoidable conflict |
| E-06 | External dependencies/resources verify licence, maintenance, glyphs, attribution, and failure strategy | E / `accepted` | Universal; font, map, and resource choice | Apply fields relevant to the resource type |
| E-07 | Gate blocking, risk owner, artifact traceability, and rollback are delivery contracts | E / `proposed` | This process; awaiting current BugChecking validation | Owner decides acceptance after UI-U5 |
| P-01 | Personal tools use minimal copy and icon-first UI; explain risk, recovery, and genuine ambiguity | P / `accepted` | Personal tools only; explicit owner statement | Reclassify for public products |
| P-02 | A cheap, recoverable, non-core novelty may use a hidden gesture as its only entry | P / `accepted` | Personal tools; favourite-hold statement | Core/high-cost capabilities need a visible fallback |
| P-03 | A few signature secondary actions may remain visible below the primary action | P / `accepted` | Personal tools; Map/Place library statement | Count and first-viewport space set by brief |
| P-04 | Accent colour follows real action/state importance, not component type or interface nesting | P / `accepted` | Personal tools and task surfaces; explicit owner statement | Platform semantic and safety states keep consistent meaning |
| C-01 | Prefer bright-soft colour that avoids muddy greys | C / `proposed` | Cross-project candidate; BugChecking evidence only | Tianse beta or explicit owner promotion |
| C-02 | Prefer chromatic dark surfaces | C / `proposed` | Cross-project candidate; BugChecking evidence only | Tianse beta or explicit owner promotion |
| C-03 | Prefer system type with complete Chinese glyph coverage | C / `proposed` | Cross-project candidate | Licensed, glyph-complete, performant brand type may override |
| C-04 | Whether minimal copy/icon-first UI extends to public products | C / `proposed` | Product-mode expansion candidate | Tianse/public-product evidence or explicit owner promotion |
| C-05 | Whether visible signature secondary actions generalise across projects | C / `proposed` | Cross-project candidate | Tianse beta or explicit owner promotion |
| C-06 | Whether hidden gestures may accelerate a frequent core capability | C / `proposed` | Cross-project candidate; visible fallback remains mandatory | Tianse beta or explicit owner promotion |
| B-BC-01 | City map, journey band, and candy contrast form the BugChecking identity; UI-U3.5 replaces the old Dynamic Soft Candy/Night Fog values with a fixed palette | B / identity `accepted`, palette replacement `proposed` | BugChecking; historical Phase 3 and current UI-U3.5 | Never copy automatically to Tianse beta; update replacement state after acceptance |
| B-BC-02 | Equal-height one-line track cards, Add-only FAB, visible Map/Place library | B / `accepted` product contract | BugChecking; explicit owner statement | Implementation tokens still pass the [roadmap](android-ui-adjustment-roadmap-2026-10-28.md) |
| B-BC-03 | UI-U1 legibility, safe-area, and bottom-action contract: explicit `onSurface`, opaque forms, transparent bottom-action host, edge-to-edge system bars, safe map attribution | B / `accepted` | BugChecking; PLZ110 UI-U1 owner/device acceptance, 2026-10-29 | Project-specific; does not promote UI-U2 map geometry or a Tianse preference |
| B-BC-04 | UI-U2 map contract: renderer-owned point/route coordinates, ordinary circles/favourite stars, distilled point sheet, Tracks-card navigation, and timed outline feedback | B / `accepted` | BugChecking; PLZ110 UI-U2 owner/device acceptance, 2026-10-30 | Cluster tokens remain deferred; never generalise as Tianse or cross-project visual rules |
| B-BC-05 | UI-U3.5 fixed palette and emphasis allocation: apricot/rose and charcoal/grey-pink surfaces, scarce neon green, candy-red journey role, neutral helpers/cards/profile/backup controls | B / `proposed` | BugChecking; current implementation | Device matrix and explicit owner acceptance |
| B-BC-06 | Primary pages use a scrimless floating action row with explicit transparent breathing space; bottom/rail navigation has three items and the avatar opens Me as an independent Back-navigated page; device status expands left without moving signature actions | B / `proposed` | BugChecking; current implementation | Device matrix and explicit owner acceptance; do not generalise before Tianse evidence |

## 2. Product and design preferences

### Product mode

- Declare personal tool, expert tool, or public product before design. Do not share onboarding/copy density across modes.
- Personal tools may use icon-first UI, learned gestures, and novelty where state feedback is clear and mistakes are cheap and recoverable. Forgotten discoverability does not require persistent hints.
- A small number of signature secondary features may remain persistently visible for play value.
- Do not add tutorials, helper paragraphs, or repeated button labels for hypothetical newcomers. Explain recovery, permissions, irreversible/high-cost actions, diagnostics, and genuine ambiguity.

### Colour and visual world

- **C candidate:** prefer bright-but-calm colour, avoiding muddy grey, pure-black expanses, and default template blue.
- Dynamic colour is not a goal by itself. When it weakens identity or task hierarchy, prefer a fixed brand palette verified on device in both themes.
- **C candidate:** prefer an authored dark-surface relationship over simple black inversion; BugChecking's current charcoal/grey-pink system is project-specific B.
- Accent scarcity follows the real task: list core state/actions, signature states, and ordinary helpers before assigning colour; avoid several near-identical hues for the same hierarchy on one screen.
- A recognisable environmental ground is welcome, but content sits on quiet readable local surfaces.
- Translucency is role-based: expressive summaries/selection, steadier opaque forms. Avoid gradients, glows, decorative cards, and filler illustration as substitutes for hierarchy.

### Layout, type, and overflow

- Touch task screens normally establish one visual primary action. Dashboards, symmetric choices, and multi-pane tools may document exceptions in the brief. Signature secondary actions may remain visible with lower emphasis.
- Prefer stable list rhythm and predictable card height. Cards use one-line trailing ellipsis when a fixed/equal-height component contract requires it.
- Shared content starts, insets, card padding, and bottom reserves remain consistent.
- On mobile, stateful, recoverable, scrolling, multistep, or asynchronous work normally uses dedicated pages; dialogs and sheets stay short. Desktop/multi-pane products follow their platform mode.
- **C candidate:** prefer platform system fonts with verified Chinese/Latin/number coverage. A brand-led project may override this with a licensed, performant, glyph-complete font.
- Build hierarchy with role, size, weight, spacing, and surface before opacity. Keep value/unit pairs atomic.
- A marquee requires explicit focus/hold/selection and reduced-motion support.

Sparse pages may use focus-building whitespace; dense forms do not stretch tasks with decorative whitespace. Ellipsised full values remain reachable in edit/detail or by explicit interaction. Form labels remain visible after input, and errors use text/icon plus semantic colour.

### Controls and selectors

- Prefer native platform and the established project design system; Android projects prefer Material 3. Novelty belongs in interaction details, not unfamiliar basic controls.
- Icon-first actions are acceptable for familiar, low-cost personal workflows. Public, irreversible, or diagnostic workflows may need labels.
- Separate primary, signature secondary, routine secondary, and destructive actions. On Android/touch, every target is at least 48×48dp; other platforms use their own target standard.
- Android may use Filled/FAB, tonal, and outlined/text to express structure and interaction type, but a component variant does not automatically receive the accent. Rank real task importance first, then map component-level colours/tokens; separate destructive actions by position, tone, or container.
- Hidden gestures are allowed when feedback begins promptly, state is visible, normal taps remain unambiguous, and failure is recoverable.
- Selectors show the minimum needed fields while retaining enough disambiguation to choose correctly; simplification must not cause selection of the wrong place/object.

### Motion and sound

- Motion explains state: short restrained springs, continuous animation only during active work, and system duration/reduced-motion respected.
- Completion/favourite tiers may feel playful but never hide the final state. Sound/haptics confirm meaningful state and do not add noise to routine browsing.

### External resources and dependencies

- New dependencies document purpose, licence, size, maintenance, and alternatives.
- Coordinate content uses renderer-native sources/layers or projection APIs. Do not reimplement projection maths in UI code; renderer projection APIs remain valid for hit testing and acceptance.
- Icons stay within one family; Android/Material 3 projects prefer Material icons. Images/fonts/maps record source, licence, attribution, and failure/offline behaviour.
- Do not download decorative filler. External resources must carry content, identity, or functional value.
- Verify Chinese glyph coverage before style; successful compilation does not prove device rendering.

## 3. Universal engineering floors

- Product statements define desired behaviour; device measurements define actual behaviour. Implementation facts rank device measurement > current code/resources > history. Intent ranks current explicit owner decision > authoritative current design/protocol docs > historical preference. A mismatch is a defect or decision, not a same-level tie-break.
- Do not disguise taste as a defect or impose consumer-product defaults on a personal tool.
- Migration, sync/BLE semantics, compatibility, and irreversible data changes require separate approval and never arrive as a UI side effect.
- Coordinates, order, units, times, and states remain truthful even when visual simplification is desirable.
- Compilation is not visual, gesture, font, map, or hardware acceptance.
- Preserve existing user changes and record worktree state before implementation.
- Every candidate standard has a state: `proposed`, `verified`, `accepted`, or `superseded`.

## 4. BugChecking-specific contract (B)

- Preserve the centred-crop city map, journey band, and candy contrast. UI-U3.5 is replacing the exact old Dynamic Soft Candy/Night Fog palette with fixed apricot/rose, charcoal/grey-pink, neon-green/candy-red roles; that replacement remains `proposed` until acceptance and never copies mechanically into Tianse beta.
- Primary access is Tracks, Checklist, and QR in the bottom bar/rail plus the top-left avatar for Me. Whole track-card tap edits; place names remain one-line ellipsised and equal-height.
- Map and Place library remain visible signature secondary actions; Add is the only primary FAB.
- Timed hold is the primary favourite interaction and may remain hidden. Its three stages must use one shared, visibly distinct candy-red tier palette across pulse, card, Place library, and map star; interaction green is reserved for action emphasis. A newly saved place immediately links unambiguous matching historical tracks, while bulk migration and BLE/sync semantics still require separate approval.
- Checklist has no ordinals or visible sort buttons; hold reorder stays within group and persists.
- New external UI/docs, APK/firmware artifacts and releases, and BLE advertising use `BugChecking`. Legacy BLE recognition, database/protocol fields, classes, and `passport_*` internals remain until a separate compatibility migration.
- Selecting a nearby place changes only the displayed name and preserves the real check-in coordinate.
- Transparent root scaffolds explicitly use `onSurface`; dense form surfaces stay stable and opaque. Track add/edit app bars use title only, with precision beside coordinates rather than in a repeated status subtitle.
- Fixed bottom Save/Add actions use transparent hosts with measured navigation/IME clearance; they do not add full-width tonal trays. System bars remain edge-to-edge and theme-adaptive.
- Map attribution uses a readable semantic scrim and consumes safe-drawing insets. Renderer sources/layers own point/route geometry; ordinary points are circles and favourites are stars. UI-U2 is accepted, while native-clustering tokens remain deferred.
- In the current pre-UI-U4 shell, primary pages have no solid top bar or page title. The device entry is the highest-value status/action: its container stays neutral, disconnected uses a deep-green icon, and all other connection/sync states use a neon-green icon. Routine editor helpers, completion/QR card surfaces, profile, and backup controls stay neutral; journey and checklist progress use candy red.

## 5. Rework and reversals to learn from

### 5.1 Route Blue to Dynamic Soft Candy/Night Fog

Device comparison came too late, so a documented palette was superseded. Future colour direction requires grouped target-device light/dark and dynamic/static samples before baseline status, and the replacement records `supersedes`.

### 5.2 Favourite scope coupling

UI, gesture, database, BLE, migration, and sync were treated as one feature and all blocked. Future cross-end work gets per-layer approval; UI acceptance never implies protocol acceptance.

### 5.3 Mainstream-audience audit assumption

Initial recommendations added discoverability and copy that conflict with a personal tool. Every design task begins with audience, publication, failure cost, and explicit non-goals.

### 5.4 One-FAB advice versus signature features

Primary/secondary was too coarse. Future inventories distinguish primary, signature secondary, routine secondary, and destructive.

### 5.5 Generic wrapping versus equal-height cards

Overflow contracts are specified per component before implementation: max lines, overflow, full-value access, extreme samples, and font-scale behaviour.

### 5.6 Static map review missed dynamic failure

Custom maps/charts/canvas/gesture rendering require zoom/pan/rotate/rebuild/race matrices; screenshots cannot sign functional correctness.

### 5.7 Geocoder suffix heuristics before evidence

Splitting a concatenated name on apparent hotel or garden-compound suffixes looked expedient but could corrupt a real merchant name, so implementation was deferred. Data cleaning begins by recording raw component fields and multiple same-coordinate `Address` results, then establishes sample distribution, precedence, counterexamples, and tests before any rule is promoted.

### 5.8 Brand rename versus compatibility

External UI/docs, public build artifacts/releases, BLE advertising, protocol, database, and historical naming are separate layers. The first three use BugChecking; legacy recognition, protocol/database, and internal names remain only for compatibility.

### 5.9 Bottom action Surface versus system navigation

The first diagnosis treated the tonal Surface around fixed Save/Add buttons as the visual split. The owner clarified that Android's forced black/white navigation-bar plate was a separate and more important defect; after edge-to-edge adaptation was fixed, the owner separately chose to remove the app-layer Surface. Future reviews isolate four layers—button, app container, system-bar background, and inset ownership—then change and capture one variable at a time in light and dark modes. A later preference refinement does not retroactively validate an earlier diagnosis.

### 5.10 Canvas-only adjustment became a negative optimisation

To reduce glare, the first pass only greyed the white canvas. Cards, chrome, and editor-tool surfaces then lost their rose warmth and looked like pale islands; the journey line and map mask no longer belonged to the same composition. A large-surface change now requires grouped adjustment and full-screen captures of canvas, mask, chrome, cards, boundaries, text, and journey line. Acceptance asks whether glare falls, content remains distinct, warmth survives, and route/boundary contrast stays clear.

### 5.11 Component defaults created false importance

After primary and secondary were unified as neon green, Filled, TonalButton, Slider, and default icons inherited the same accent. Routine editor helpers, completion and QR cards, and profile/backup actions competed with the device sync entry; near-identical greens also looked like defects. Colour work now starts with an operational-importance map. Component variants define structure and behaviour, while explicit component-level semantic colours decide emphasis.

## 6. Development gates

Gates are not an unconditional alphabetic pipeline. Normal dependency is `A → B → C → D → J preflight (when risk/data is touched) → E → F → G → I → J scope recheck → K → L → H`; when no risk/data scope is touched, mark J not applicable with a reason. M runs only for cross-project promotion. Any required FAIL blocks H. NOT RUN must name a reason and risk owner and receive explicit owner deferral, otherwise it also blocks H. J/K findings must be closed or explicitly deferred with an owner, not merely marked “checked”.

### Gate A — Brief
Record audience, publication, core task, signature experience, failure cost, non-goals, and target devices. Missing fields block option selection.

### Gate B — Evidence
Current authoritative docs, worktree, and running product/device evidence; unseen states remain hypotheses.

### Gate C — Boundaries
UI, interaction, data, sync/protocol, migration, and hardware each say change/no-change/separate approval.

### Gate D — Options and acceptance first
For judgment calls, first map operational importance and assign semantic ownership for canvas, chrome, cards, text, boundaries, states, and key actions. Then use 2–3 constrained options and define extreme samples and acceptance before implementation.

### Gate E — Small vertical slice
Validate default, long content, failure, dark, and target font scale on one complete slice before propagation.

### Gate F — Automated gates
Run relevant tests during iteration; unit, lint, build, and repository docs before delivery. Automatable rules enter tools/CI.

### Gate G — Device gate
Build the current worktree for target hardware; record visual, gesture, inset, font, dynamic-map, and hardware states as PASS/FAIL/NOT RUN. Colour changes cover both themes and main/form/state screens, judging full compositions rather than isolated swatches.

### Gate H — Freeze and memory
Only after G, I, applicable J, K, and L pass or receive explicit risk deferral may owner acceptance update authoritative docs, mark rules `accepted`, record `supersedes`, and permit a memory extension. Cross-project preference requires a second project or explicit promotion.

### Gate I — Rule classification
Record E/P/B/C, platform/product mode, status, evidence, exceptions, promotion conditions, and `supersedes`; B always names its project. Unclassified normative rules cannot enter DESIGN, memory, or a skill.

### Gate J — Risk and data
Before implementation, review least privilege, secret/log redaction, retention/deletion, overwrite, backup/restore, offline, duplicate submit, conflict, timeout, partial success, migration failure, and rollback; after device work, recheck whether implementation expanded scope. Personal tools do not skip applicable items.

### Gate K — Operability and compatibility
Within the owner-approved Gate A scope, check contrast, font scale, non-colour state, focus/semantics, reduced motion, supported OS/device forms, language, dynamic/static colour, and abnormal networks; a narrower scope records reason and risk. Semantic labels, trigger feedback, and recovery are never waived. A hidden gesture may be the only entry only for cheap, recoverable, non-core novelty; core, high-cost, or irreversible capability needs a visible fallback.

### Gate L — Release evidence
Record the base commit. If the worktree is dirty, also record `git status` and a dirty-diff/patch fingerprint, plus build configuration, APK/firmware hash, device/OS, test versions, known unverified items, and rollback. A commit is not a precondition, but the artifact must map uniquely to “base commit + worktree diff”.

### Gate M — Candidate promotion
Before Tianse beta uses a candidate, state hypothesis, counterexamples, pass condition, and fallback classification. Promote to P after Tianse-beta evidence or an explicit owner decision; otherwise keep it C or a B rule for a named project.

## 7. Decision record

```text
Decision / scope / audience and mode / problem / non-goals
Options / selected reason / rejected reasons
Layers: UI / interaction / data / protocol / migration / hardware
Acceptance matrix
State: proposed / verified / accepted / superseded
Supersedes / superseded by
Cross-project status: no / candidate / confirmed
Rule class: E / P / B / C
Scope and platform
Evidence and exceptions
Promotion condition
```

## 8. Path to a personal development skill

Accept BugChecking rules phase by phase; apply the same process—but not the same visual world—to Tianse beta; compare both projects; explicitly promote genuine cross-project preferences; then create a skill containing triggers, brief questions, selection tendencies, anti-patterns, layered approvals, device matrices, memory promotion, and examples. Replay it against both projects: it must not copy BugChecking's map/candy identity into Tianse beta or leak Tianse beta's identity back into BugChecking.

## 9. Candidate preferences awaiting Tianse beta

- C-04: whether icon-first minimal copy applies beyond personal tools.
- C-01/C-02: whether bright-soft colour and chromatic dark surfaces are cross-project preferences.
- C-05: whether persistently visible signature secondary actions generalise.
- C-03: whether system typography remains preferred under stronger brand needs.
- C-06: whether hidden gestures may become optional accelerators for frequent core flows; a visible fallback remains an engineering floor.

These are not cross-project mandates until evidence or explicit owner promotion settles them.
