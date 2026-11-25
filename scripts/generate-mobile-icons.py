#!/usr/bin/env python3
"""Resize the approved icon into native iOS/Android assets using macOS sips."""
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs/design/sanctuary-app-icon-source.png"


def resize(target: Path, pixels: int) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["sips", "--resampleHeightWidth", str(pixels), str(pixels),
                    str(SOURCE), "--out", str(target)], check=True, stdout=subprocess.DEVNULL)


iconset = ROOT / "apps/ios/Sanctuary/Assets.xcassets/AppIcon.appiconset"
for entry in json.loads((iconset / "Contents.json").read_text())["images"]:
    pixels = round(float(entry["size"].split("x")[0]) * float(entry["scale"].rstrip("x")))
    resize(iconset / entry["filename"], pixels)
resize(ROOT / "apps/ios/Sanctuary/Assets.xcassets/BrandLogo.imageset/brand-logo.png", 1024)
resources = ROOT / "apps/android/app/src/main/res"
resize(resources / "drawable/brand_logo.png", 1024)
resize(resources / "drawable/ic_launcher_foreground.png", 1024)
for density, pixels in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
    for name in ["ic_launcher", "ic_launcher_round"]:
        resize(resources / f"mipmap-{density}/{name}.png", pixels)
print("Generated iOS app icons, Android launcher assets, and matching in-app branding.")
