#!/usr/bin/env python3
"""Check actual palette contrast and native launcher-asset contracts (no dependencies)."""
import json
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ios = (ROOT / 'apps/ios/Sanctuary/UI/AppTheme.swift').read_text()
android = (ROOT / 'apps/android/app/src/main/java/app/sanctuary/android/ui/theme/Color.kt').read_text()

def token(text, name, pattern):
    match = re.search(pattern.format(name=re.escape(name)), text)
    assert match, name
    return match[1]

pairs = [('homeInk', 'SanctuaryHomeInk'), ('homeMuted', 'SanctuaryHomeMuted'),
         ('homeSurface', 'SanctuaryHomeSurface'), ('homeSurfaceRaised', 'SanctuaryHomeSurfaceRaised'),
         ('onAccent', 'SanctuaryOnAccent'), ('errorText', 'SanctuaryError')]
colors = {}
for swift_name, kotlin_name in pairs:
    a = token(ios, swift_name, r'static let {name} = Color\(hex: "#([A-Fa-f0-9]{{6}})"\)')
    b = token(android, kotlin_name, r'val {name} = Color\(0xFF([A-Fa-f0-9]{{6}})\)')
    assert a.upper() == b.upper(), (swift_name, a, b)
    colors[swift_name] = a

def luminance(hex_value):
    rgb = [int(hex_value[i:i+2], 16) / 255 for i in (0, 2, 4)]
    linear = [x/12.92 if x <= .04045 else ((x+.055)/1.055)**2.4 for x in rgb]
    return sum(x*w for x, w in zip(linear, [.2126, .7152, .0722]))

for fg, bg in [('homeInk','homeSurface'), ('homeMuted','homeSurface'),
               ('homeInk','homeSurfaceRaised'), ('homeMuted','homeSurfaceRaised'), ('errorText','homeSurface')]:
    a, b = sorted([luminance(colors[fg]), luminance(colors[bg])])
    ratio = (b+.05)/(a+.05)
    assert ratio >= 4.5, (fg, bg, ratio)
    print(f'{fg} on {bg}: {ratio:.2f}:1')

def png(path):
    data = path.read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', path
    width, height = struct.unpack('>II', data[16:24])
    return width, height, data[25]

iconset = ROOT / 'apps/ios/Sanctuary/Assets.xcassets/AppIcon.appiconset'
for entry in json.loads((iconset/'Contents.json').read_text())['images']:
    size = round(float(entry['size'].split('x')[0]) * float(entry['scale'].rstrip('x')))
    width, height, color_type = png(iconset/entry['filename'])
    assert (width, height) == (size, size), entry
    if entry['idiom'] == 'ios-marketing': assert color_type in (0, 2), 'App Store icon must have no alpha channel'
res = ROOT / 'apps/android/app/src/main/res'
for density, size in {'mdpi':48, 'hdpi':72, 'xhdpi':96, 'xxhdpi':144, 'xxxhdpi':192}.items():
    for name in ['ic_launcher','ic_launcher_round']:
        assert png(res/f'mipmap-{density}/{name}.png')[:2] == (size,size)
for name in ['ic_launcher','ic_launcher_round']:
    assert 'ic_launcher_monochrome' in (res/f'mipmap-anydpi-v26/{name}.xml').read_text()
print('Native icon dimensions, App Store opacity, and Android themed-icon references passed.')
