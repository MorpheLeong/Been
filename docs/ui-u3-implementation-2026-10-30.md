[简体中文](ui-u3-implementation-2026-10-30.zh_CN.md) · **English**

# BugChecking Android UI-U3 Implementation and Acceptance Checkpoint

Date: 2026-10-30
Status: `ACCEPTED`
Prerequisite: UI-U2 explicitly accepted by the owner

## 1. Boundary

UI-U3 rebalances only the Tracks home action hierarchy and card-title overflow. It preserves the four primary destinations, Dynamic Soft Candy / Night Fog identity, journey band, hidden favourite hold, card tap-to-edit, map implementation, and all accepted UI-U0–UI-U2 contracts.

## 2. Implemented result

- Keep Add Track as the only floating action button, using the standard Material 3 primary FAB role.
- Replace the two additional same-weight FABs with one fixed secondary action strip above the scroll viewport. Map and Place library remain visible on the first viewport and are never moved to overflow.
- Resolve the signature strip through `secondaryContainer` / `onSecondaryContainer`, the 16 dp large shape, and 1 dp tonal elevation. Each action has its own 48 × 48 dp IconButton target and precise content description.
- Give the signature strip an 8 dp top gap. Because it occupies layout space, it never covers a track card.
- Reserve 104 dp at the list bottom for the sole primary FAB so the final card can scroll clear of it.
- Keep every place name to one line with trailing ellipsis. Short, long Chinese, and mixed-script names never wrap or change card height.
- Leave the hidden timed favourite hold and whole-card edit click unchanged.

## 3. Verification

| Gate | Result | Evidence |
| --- | --- | --- |
| First-viewport hierarchy | PASS | Add, Map, and Place library are simultaneously visible; Add is the only FAB |
| Signature targets | PASS | UI hierarchy reports two separate 168 × 168 px targets on the PLZ110, equivalent to 48 × 48 dp |
| Card obstruction | PASS | Signature strip occupies layout space above the scroll viewport and does not overlay cards |
| 1.0× font | PASS | Short, long, and mixed-script fixture titles remain single-line trailing ellipsis with equal card anatomy |
| 2.0× font | PASS | Three visible fixture cards each measured 385 px high; no action or bottom-navigation collision |
| Navigation | PASS | Map, Place library, and whole-card edit routes all opened the expected destination |
| Favourite interaction | PASS | Card semantics remain long-clickable; existing hold-stage unit coverage passes and the implementation path was not changed |
| Android gate | PASS | `testDebugUnitTest`, `lintDebug`, and `assembleDebug` completed successfully on the final tokenised code (51 tasks: 15 executed, 36 up-to-date) |
| Repository check | PASS | `git diff --check` reports no whitespace error; unrelated dirty-worktree changes were preserved |

The final installed debug APK is `BugChecking-debug.apk`, SHA-256 `583DDD1E181973835396DBEE9B04BE5D07F40F4D65E3B0A355E86B4A4A51624C`. The temporary 2.0× font setting was restored to 1.0× after verification. Screenshots contain private place data and were removed after inspection.

## 4. Accepted tokens and contracts

| ID | Accepted value / behaviour |
| --- | --- |
| `action/track-primary-fab` | Add Track is the only FAB and uses the standard Material 3 primary FAB role |
| `action/track-signature-icon` | Shared secondary action strip; `secondaryContainer` / `onSecondaryContainer`; 16 dp shape; 1 dp tonal elevation; 48 × 48 dp per action |
| `layout/track-action-gap` | 8 dp |
| `layout/track-action-safe-reserve` | 104 dp list-bottom reserve for the sole FAB; signature actions remain outside the scroll viewport |
| `track-card/title-max-lines` | 1 |
| `track-card/title-overflow` | trailing ellipsis |

These are BugChecking project contracts. They do not automatically become Tianse beta rules or cross-project personal preferences.

## 5. Gate state

On 2026-10-30 the owner explicitly confirmed UI-U3 acceptance. UI-U3 is closed and UI-U4 cross-page consistency / token convergence is unlocked.
