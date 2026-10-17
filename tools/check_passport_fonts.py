"""Check actual generated LVGL cmap coverage and fixed-text glyph bounds."""

import json
from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[1]


def array_body(text, name):
    match = re.search(r"\b" + re.escape(name) + r"\[\]\s*=\s*\{(.*?)\};", text, re.S)
    if not match:
        raise AssertionError(f"missing array {name}")
    return match.group(1)


def numbers(text, name):
    body = re.sub(r"/\*.*?\*/", "", array_body(text, name), flags=re.S)
    return [int(value, 0) for value in re.findall(r"0x[\da-fA-F]+|\d+", body)]


def font_map(path):
    text = path.read_text(encoding="utf-8")
    glyphs = []
    for block in re.findall(r"\{([^{}]*)\}", array_body(text, "glyph_dsc")):
        glyphs.append({key: int(value, 0) for key, value in re.findall(
            r"\.(\w+)\s*=\s*(-?0x[\da-fA-F]+|-?\d+)", block)})
    mapping = {}
    for block in re.findall(r"\{([^{}]*)\}", array_body(text, "cmaps")):
        fields = dict(re.findall(r"\.(\w+)\s*=\s*(\w+)", block))
        start = int(fields["range_start"], 0)
        offsets = (numbers(text, fields["unicode_list"])
                   if fields["unicode_list"] != "NULL"
                   else list(range(int(fields["range_length"], 0))))
        ids = (numbers(text, fields["glyph_id_ofs_list"])
               if fields["glyph_id_ofs_list"] != "NULL" else list(range(len(offsets))))
        for offset, glyph_offset in zip(offsets, ids):
            glyph_id = int(fields["glyph_id_start"], 0) + glyph_offset
            assert 0 < glyph_id < len(glyphs), (path.name, glyph_id)
            mapping[start + offset] = glyphs[glyph_id]
    metrics = dict(re.findall(r"\.(line_height|base_line)\s*=\s*(\d+)", text))
    return mapping, int(metrics["line_height"]), int(metrics["base_line"])


def main():
    source = (ROOT / "main" / "main.c").read_text(encoding="utf-8")
    # Tokenize strings separately from comments so comment prose is not UI input.
    required = set()
    tokens = re.finditer(r'(?P<string>"(?:\\.|[^"\\])*")|/\*.*?\*/|//[^\n]*', source, re.S)
    for token in tokens:
        if token.group("string"):
            required.update(ord(char) for char in json.loads(token.group("string"))
                            if ord(char) >= 32)
    common = set(map(ord, (ROOT / "assets" / "fonts" / "passport_gb2312_symbols.txt")
                     .read_text(encoding="utf-8")))
    assert len(common) == 6763
    for size in (14, 20):
        mapping, height, baseline = font_map(
            ROOT / "assets" / "fonts" / f"passport_gb2312_{size}.c")
        missing = (required | common) - mapping.keys()
        assert not missing, f"{size}px missing: " + ", ".join(f"U+{cp:04X}" for cp in sorted(missing))
        assert 0x1F9A6 not in mapping, "known unsupported emoji unexpectedly covered"
        ascii_widths = {mapping[cp]["adv_w"] for cp in range(0x21, 0x7F)}
        cjk_widths = {mapping[cp]["adv_w"] for cp in common}
        assert len(ascii_widths) == 1, f"{size}px ASCII is not monospaced"
        assert len(cjk_widths) == 1, f"{size}px GB2312 is not monospaced"
        assert next(iter(ascii_widths)) * 2 == next(iter(cjk_widths)), \
            f"{size}px ASCII/CJK cell ratio is not 1:2"
        for cp in required:
            glyph = mapping[cp]
            if glyph["box_h"]:
                top = height - baseline - glyph["box_h"] - glyph["ofs_y"]
                assert 0 <= top and top + glyph["box_h"] <= height, (size, cp, top)
        print(f"{size}px: monospaced fixed text and 6,763 GB2312 glyphs covered; line height {height}px")


if __name__ == "__main__":
    main()
