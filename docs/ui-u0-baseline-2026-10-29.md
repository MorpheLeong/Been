[简体中文](ui-u0-baseline-2026-10-29.zh_CN.md) · **English**

# Been Android UI-U0 baseline and evidence record

Status: `ACCEPTED`; the owner explicitly confirmed “UI-U0 accepted” on 2026-10-29
Recorded: 2026-10-29 01:31 Asia/Shanghai
Stage source: [UI roadmap and acceptance plan](android-ui-adjustment-roadmap-2026-10-28.md#ui-u0--baseline-and-evidence)

## 1. Scope and evidence rules

UI-U0 freezes facts, prepares synthetic fixtures, and records evidence. It does not change UI code, clear/export personal data, or promote a token. After explicit owner approval, `adb install -r` updated to the current-worktree APK while preserving data; no in-App save, delete, or migration was executed.

Evidence order is live device > device capture/recording > current code/resources > owner description. The unlocked device was observed. Raw captures/UI trees contained private places, precise coordinates, QR secrets, or profile imagery, so only SHA-256 evidence and redacted findings were retained.

## 2. Frozen worktree and APK

| Item | Baseline |
| --- | --- |
| Branch/base | `feature/location-passport-app` at `009e387d8645f9a7177b9b76d439e7919b23a8a6` |
| Commit | `feat(passport): add place library and layered backups`, 2026-10-28 22:34:39 +08:00 |
| Worktree | dirty only with the current documentation work; tracked-diff Git blob SHA-1 `cc994bce7da40a4ec8025571cac520461006f997` |
| Application | `com.bugchecking.app`, source and installed `versionCode=1`, `versionName=0.1.0`, installed target SDK 36 |
| Pre-update installed APK | 2026-10-28 04:51:02; 61,029,498 bytes; SHA-256 `717B0019A47F04943ADB8707271600FA88079119A23A1DA1432484F95FAA9060` |
| Post-update installed APK | 2026-10-29 01:19:24; 123,055,609 bytes; SHA-256 `221750AD8548B68C3A354AC20DE777B62F2B60BE303A62D1FFE3760D9C915C9E` |
| Current-worktree build | `assembleDebug` PASS; `Been-debug.apk`, 123,055,609 bytes; SHA-256 `221750AD8548B68C3A354AC20DE777B62F2B60BE303A62D1FFE3760D9C915C9E` |

The installed APK was pulled only to a system temporary folder for hashing and then deleted.

**Artifact match resolved:** after explicit owner approval, `adb install -r` returned `Success`. A fresh pull of the installed APK produced the same SHA-256 as the current-worktree build, so the live observations below are attributable to the frozen artifact.

## 3. Fixed device state

| Dimension | Value |
| --- | --- |
| Device | OnePlus PLZ110 / `OP64DDL1` |
| Android | Android 17, API 37, `PLZ110_17.0.0.106(CN01)` |
| Fingerprint | `OnePlus/PLZ110/OP64DDL1:17/CP2A.260605.016/B.34f780d-ee9c62-ef681b:user/release-keys` |
| Display | 1216×2640 px, 560 dpi, portrait |
| Locale/font | `zh-Hans-CN`, 1.0× |
| Navigation | raw `navigation_mode=2`; gesture bar visually confirmed |
| IME | `com.oplus.keyboard/.service.ImeService` |
| Theme | custom night 23:00–06:00; current computed night = true |
| Dynamic colour | home wallpaper, `TONAL_SPOT`, system/accent palette `#1A8CFF`, overlay enabled |
| Access | owner-unlocked; authentication was not bypassed |
| Artifact match | installed hash equals current-worktree hash |

Dynamic light, static-fallback light/dark, 1.3×/1.5×/2.0×, and three-button navigation remain uncaptured. Any reversible system-setting changes must be logged and restored.

## 4. Synthetic fixture catalogue

Fixtures are definitions only and are not written into the owner's database. They cover: short/long/mixed-script tracks; same-day and cross-day coordinates; favourite tiers 1–3; short/long saved places; short and near-192-byte QR payloads; pending/completed checklist groups; empty states; map-load recovery; and long retryable sync errors. The Chinese record contains the exact fixture names and values.

Map fixtures record the GeoJSON point-feature total. Later acceptance compares `unclustered feature count + sum(cluster point_count)` with that total, never with card or date-group counts.

## 5. Capture matrix and evidence path

Captures belong in [`docs/evidence/ui-u0-2026-10-29/`](evidence/ui-u0-2026-10-29/README.md) and use:

```text
<theme>-<font>-<navigation>-<screen>-<state>.<png|mp4>
```

At dynamic dark, 1.0×, gesture navigation, the observed surfaces are Tracks, Checklist, QR, Me, Track add/edit, QR add/edit/view, Smart import empty, Place library/detail, and static Track map. No save/delete action was executed. Dynamic light, static fallback, non-1.0× font scales, synthetic failure states, three-button navigation, and independent pinch-drift recording remain explicitly `NOT RUN`.

## 6. Map reproduction script

Dynamic point/route drift remains owner-reproduced; this stage independently observed the static map and attribution collision, but did not reproduce the full pinch sequence.

1. Open Track map and wait for style, routes, and markers to settle.
2. Stop at zoom 4/8/12/16/19 and capture each state.
3. Perform ten rapid alternating pinches, then continuous pan, rotation, and refit.
4. After each idle, compare point/cluster totals with the fixture feature total.
5. Confirm ordinary and favourite points use identical coordinates and differ only in style.
6. Record the first failing zoom, exact gesture sequence, duration, and recovery.

Expected: points/routes stay geographically anchored, no stale annotations/clusters remain, attribution avoids system gesture space, and Back/recovery stay available.

## 7. Finding reproduction index

The executable matrix covers A11Y-01, RESP-01, VIS-01, INPUT-01, MAP-01, MAP-02, A11Y-02, UX-02, DS-01, PLAT-01, PLAT-02, FBK-01, DS-02, and TASTE-01. For each it fixes the page/state, action, and expected result; the Chinese record is the authoritative detailed table. Medium/Expanded and predictive Back remain explicitly deferrable, never implicit PASS.

## 8. Evidence log

**SEEN:** all four primary screens and the listed secondary screens on the matching current-worktree APK; dark-title contrast failures; three-FAB list obstruction; long editor value truncation; map attribution collision; three equal-weight QR card actions; device and artifact metadata.

**NOT SEEN:** synthetic empty/extreme/failure boundaries; independent pinch-drift recording; dynamic light, static fallback, non-1.0× font scales, and three-button navigation.

## 9. UI-U0 result

`ACCEPTED`. On 2026-10-29 the owner explicitly confirmed “UI-U0 accepted”. The worktree, pre/post-update APK hashes, device conditions, fixtures, matrix, map script, finding index, and dynamic-dark 1.0× observations are frozen. Dynamic light, static fallback, 1.3×/1.5×/2.0×, three-button navigation, and synthetic failure boundaries remain explicit deferrals and are not PASS. UI-U1 may now start. UI-U0 produced no promotable visual token, so candidate-token states remain unchanged.
