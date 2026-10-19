<p align="right">
  <a href="README.zh_CN.md">简体中文</a> · <strong>English</strong>
</p>

# Assets

This directory stores reusable fonts, images, music, and sound effects, organized by asset type.

Keep each asset in the matching subdirectory and document its destination, naming, integration method, and source/license. Do not mix binary assets with Markdown documentation.

## Fonts

Store reusable font files and generated font sources in `fonts/`.

- Use descriptive names that include the family, weight, size, and format when relevant.
- Document the source, license, character range, conversion command, and expected destination.
- Check Flash and internal-RAM impact before adding a font; the ESP32-C3 has no PSRAM.
- Do not commit fonts whose license does not permit redistribution.

| File | Use and source/license |
| --- | --- |
| [`fonts/passport_gb2312_14.c`](fonts/passport_gb2312_14.c), [`fonts/passport_gb2312_20.c`](fonts/passport_gb2312_20.c) | Simplified Chinese monospaced UI fonts at 14px and 20px, Medium (500), 4 bpp, for the application's locked LVGL 9.6.0~1. Generated from Noto Sans Mono CJK SC with `fonttools` 4.60.1 and `lv_font_conv` 1.5.3, without compression or kerning. Covers GB2312 Chinese, half-width ASCII, and common punctuation. Noto Sans CJK is licensed under OFL 1.1; see [`fonts/OFL.txt`](fonts/OFL.txt). |
| [`fonts/passport_gb2312_symbols.txt`](fonts/passport_gb2312_symbols.txt) | The 6,763 Chinese characters encoded by GB2312, used as the reproducible converter symbol list. |

Regenerate with [`tools/generate_passport_fonts.py`](../tools/generate_passport_fonts.py), which records the exact punctuation ranges, pins the variable font to weight 500, and applies the LVGL 9 static-bitmap adaptation. Pass `--font` with the official `NotoSansMonoCJKsc-VF.otf` source and `--converter` with `lv_font_conv` 1.5.3's JavaScript entry point. The source SHA-256 is `ABB74B8CDDF47C8267FD4948EB2B9F275218206F6CEA8490D9305BB1B085BC7F`.

[`tools/check_passport_fonts.py`](../tools/check_passport_fonts.py) verifies the generated cmap tables, GB2312 coverage, fixed text and glyph bounds. The UI aligns the Latin/Chinese baselines and reserves the 17px/24px line heights. Characters outside this set can use the built-in Source Han SC fallback; arbitrary rare characters and emoji are not guaranteed.

## Images

Store reusable source images and generated display assets in `images/`.

| File | Dimensions and format | Use and source |
| --- | --- | --- |
| [`images/home.jpg`](images/home.jpg) | 3840 × 2160, JPEG | Product hero image embedded in both project README files to foreground AI Passport and its open, maker-oriented identity. |
| [`images/readme-hardware-specs.png`](images/readme-hardware-specs.png) | 2172 × 724, PNG RGBA | Optional technical infographic retained as a reference asset; it is no longer used as the homepage hero. Generated for this repository with the built-in image generation tool on 2026-09-17; the six labels and values were checked against the documented hardware contract. |
| [`images/logo-wordmark.png`](images/logo-wordmark.png) | 1648 × 336, PNG RGBA | Transparent black wordmark extracted from the repository's original `images/logo.png`; embedded in both project README files for light backgrounds. |
| [`images/logo-wordmark-dark.png`](images/logo-wordmark-dark.png) | 1648 × 336, PNG RGBA | White version of the extracted wordmark, used by the README `<picture>` element when GitHub is in dark mode. |
| [`images/bugchecking-app-icon-source.png`](images/bugchecking-app-icon-source.png) | 1254 × 1254, PNG RGBA | Canonical BugChecking app-icon artwork updated and approved by the project owner on 2026-10-20. Launcher renditions are generated without redrawing by [`tools/generate_android_launcher_icons.py`](../tools/generate_android_launcher_icons.py); legacy and adaptive outputs use separate deterministic crops so launcher masks stay centred on the white screen frame. |
| [`images/bugchecking-city-map-halftone.png`](images/bugchecking-city-map-halftone.png) | 1294 × 1216, PNG RGB | Canonical blue-gray halftone city-map background supplied and approved by the project owner on 2026-10-19. Integrated without redrawing into the Android primary-page scaffold as `drawable-nodpi/bugchecking_city_map_halftone.png`; center crop preserves the artwork's center across window sizes. |

- Use descriptive names and document dimensions, pixel format, conversion steps, and destination.
- Prefer formats suitable for the 240 × 320 RGB565 display and account for Flash and internal RAM.
- Preserve editable sources where licensing permits, and record the source and license.
- Never commit device QR secrets, credentials, or personal data in images.

## Music and sound effects

Store reusable music and sound-effect sources in `music/`.

- Document the source, license, sample rate, bit depth, channels, conversion command, and destination.
- Prefer 16 kHz, 16-bit mono PCM when it matches the current BSP audio path.
- Check Flash and internal-RAM cost before embedding audio; stream or chunk long recordings.
- Do not commit media without redistribution permission.
