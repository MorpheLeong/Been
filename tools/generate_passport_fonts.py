"""Generate the application's licensed, uncompressed LVGL Chinese fonts."""

import argparse
import hashlib
from pathlib import Path
import subprocess


RANGES = (
    "0x20-0x7E,0x00A0,0x00B7,0x2013-0x2014,0x2018-0x2019,"
    "0x201C-0x201D,0x2026,0x3000-0x3002,0x3008-0x3011,"
    "0xFF01,0xFF08-0xFF09,0xFF0C,0xFF1A-0xFF1B,0xFF1F"
)
SOURCE_SHA256 = "a2b93e6c2db05d6bbbf6f27d413ec73269735b7b679019c8a5aa9670ff0ffbf2"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--font", required=True, type=Path)
    parser.add_argument("--converter", required=True, type=Path,
                        help="lv_font_conv 1.5.3's lv_font_conv.js")
    parser.add_argument("--node", default="node")
    args = parser.parse_args()
    if hashlib.sha256(args.font.read_bytes()).hexdigest() != SOURCE_SHA256:
        parser.error("source font SHA-256 differs from the recorded Noto Sans SC source")
    assets = Path(__file__).resolve().parents[1] / "assets" / "fonts"
    symbols = (assets / "passport_gb2312_symbols.txt").read_text(encoding="utf-8")
    version = subprocess.check_output(
        [args.node, str(args.converter), "--version"], text=True).strip()
    if version != "1.5.3":
        parser.error(f"expected lv_font_conv 1.5.3, got {version}")
    for size in (14, 20):
        output = assets / f"passport_gb2312_{size}.c"
        subprocess.run([
            args.node, str(args.converter), "--font", str(args.font),
            "--range", RANGES, "--symbols", symbols, "--size", str(size),
            "--bpp", "4", "--format", "lvgl", "--no-compress", "--no-kerning",
            "--lv-font-name", f"passport_font_{size}", "--lv-include", "lvgl.h",
            "--output", str(output),
        ], check=True)
        # npm 1.5.3 predates LVGL 9's static-bitmap marker. Keep the generated
        # uncompressed bytes in Flash, without a per-glyph RAM copy.
        text = output.read_text(encoding="utf-8")
        text = text.replace(
            "    .dsc = &font_dsc,",
            "#if LV_VERSION_CHECK(9, 3, 0)\n    .static_bitmap = 1,\n#endif\n"
            "    .dsc = &font_dsc,")
        text = text.replace('in lv_conf.h")', 'in lv_conf.h"')
        output.write_text(
            "/* Derived from Noto Sans SC; distributed under assets/fonts/OFL.txt. */\n"
            + text, encoding="utf-8")
        print(f"Generated {output.name}: {output.stat().st_size} source bytes")


if __name__ == "__main__":
    main()
