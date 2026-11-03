[简体中文](README.zh_CN.md) · **English**

# UI-U0 device evidence

This folder contains the redacted evidence manifest for the 2026-10-29 UI-U0 fixed matrix. Do not store databases, backups, exported private precise locations, device secrets, or unredacted logs here.

Naming: `<theme>-<font>-<navigation>-<screen>-<state>.<png|mp4>`.

Status: PLZ110 observation completed. Raw screenshots/UI trees contained private places, precise coordinates, QR secrets, or profile imagery. They were reviewed only in temporary storage, hashed before cleanup, never committed, and deleted from both local and device temporary directories.

Fixed conditions: current-worktree APK SHA-256 `221750AD8548B68C3A354AC20DE777B62F2B60BE303A62D1FFE3760D9C915C9E`; dynamic dark; 1.0× font; gesture navigation; 1216×2640.

| Surface | Screenshot SHA-256 | Redacted observation |
| --- | --- | --- |
| Tracks | `8328A4DB5E331C6DB13F894FD75AF8C9BF8EE4B4033C4DB50097042B4B3439C8` | Low-contrast date; three FABs obstruct cards; long names stay single-line ellipsized and equal-height |
| Checklist | `91F9CF8AE432A43D0B1AD43CC8E3C43A340C891B82E6DE1B972D369E955F18F1` | Low-contrast pending-section heading |
| Smart import empty | `6D72A8373FC5142AF089C3306CDD8A1E0744F3990D4BC71213F397E1ABC7A6A4` | Empty page reachable; parse/failure/preview not run |
| QR | `891B5C8929E81E0E3FC540C1B22118FF85DCEB4F34ED71BC8B688C51182C4797` | Three equal-weight inline actions per card |
| QR add/edit/view | `D9704346B0A3DEB970A7608A01144D9E6D733592D017ADECEBBF14F57D828E99` / `87F73ECE4F3C7F7958E55F40DEF6F9D4FACA89312CA79A90E9E3B0E414A71212` / `768A200025C1FE6AB407D1C08D14AF499D088B31C227CE2416C75B6C32AA0932` | Reachable; no save; real payload not retained |
| Me | `93E9AEC86D2A32752FE59A7136BC6F337BB6D19CC0AA9D366105C26A4C0337AD` | Low-contrast identity heading; profile data not retained |
| Place library/detail | `1DDD10FDA365CFB3AF3B2920CF802EAF97849B43E5629292D9048469D25A6429` / `1E2A15C94A6336C3A04E3C1CFEB388DEA6CE91ADB05422A26006E152311B4264` | Readable cards/detail; fixed bottom action; no save |
| Track add/edit | `C3B460643C2E6A6610287C3DBB4359C4404FEBC1430A3D8870A2394546D7D9B1` / `4F66041B0C6DC5847B0F5630DDAC3FB907664403C2454AB4CEC27F18B86CA3EC` | Long place value is horizontally truncated; no save; coordinates not retained |
| Track map static | `44AFCDFC8AF853CC8680E108FEE655F9DE2A063DAADB9230B41135D672D226DD` | Points/routes visible; attribution overlaps/clips into gesture space |

UI trees were used only to confirm hierarchy and were not retained because they contained sensitive text. Pinch-drift remains owner-reproduced; U0 freezes the deterministic script without claiming an independent recording.
