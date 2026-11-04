#!/usr/bin/env python3
"""Generate Been Android launcher icons from the approved source art."""

from pathlib import Path

from PIL import Image


REPO_ROOT = Path(__file__).resolve().parents[1]
SOURCE = REPO_ROOT / "assets" / "images" / "bugchecking-app-icon-source.png"
RES = REPO_ROOT / "android-app" / "app" / "src" / "main" / "res"
SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# The approved source is a 1254 px square. These two crops intentionally differ:
# legacy launchers display the bitmap directly, while adaptive launchers only
# expose the central safe area after applying their system mask. Keeping a
# wider adaptive crop makes both paths land on the same screen-centred framing.
LEGACY_CROP = (312, 264, 943, 895)
ADAPTIVE_CROP = (163, 129, 1093, 1059)


def save_square(image: Image.Image, path: Path, size: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.resize((size, size), Image.Resampling.LANCZOS).save(path, optimize=True)


def crop_square(image: Image.Image, box: tuple[int, int, int, int]) -> Image.Image:
    left, top, right, bottom = box
    if right - left != bottom - top:
        raise SystemExit(f"Launcher crop must be square: {box}")
    if left < 0 or top < 0 or right > image.width or bottom > image.height:
        raise SystemExit(f"Launcher crop exceeds source bounds: {box}")
    return image.crop(box)


def main() -> None:
    source = Image.open(SOURCE).convert("RGBA")
    if source.width != source.height or source.width < 512:
        raise SystemExit("Launcher source must be square and at least 512 px.")

    legacy = crop_square(source, LEGACY_CROP)
    adaptive = crop_square(source, ADAPTIVE_CROP)

    for density, size in SIZES.items():
        directory = RES / f"mipmap-{density}"
        save_square(legacy, directory / "ic_launcher.png", size)
        save_square(legacy, directory / "ic_launcher_round.png", size)

    save_square(
        adaptive,
        RES / "drawable-nodpi" / "bugchecking_icon_foreground.png",
        432,
    )


if __name__ == "__main__":
    main()
