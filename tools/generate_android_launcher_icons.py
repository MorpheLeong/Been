#!/usr/bin/env python3
"""Generate BugChecking Android launcher icons from the approved source art."""

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


def save_square(image: Image.Image, path: Path, size: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.resize((size, size), Image.Resampling.LANCZOS).save(path, optimize=True)


def main() -> None:
    source = Image.open(SOURCE).convert("RGBA")
    if source.width != source.height or source.width < 512:
        raise SystemExit("Launcher source must be square and at least 512 px.")

    for density, size in SIZES.items():
        directory = RES / f"mipmap-{density}"
        save_square(source, directory / "ic_launcher.png", size)
        save_square(source, directory / "ic_launcher_round.png", size)

    # Android adaptive icons use a 108 dp canvas. The supplied composition has
    # generous safe margins, so retaining the complete artwork avoids altering it.
    save_square(source, RES / "drawable-nodpi" / "bugchecking_icon_foreground.png", 432)


if __name__ == "__main__":
    main()
