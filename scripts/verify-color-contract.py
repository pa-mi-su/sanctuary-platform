#!/usr/bin/env python3
"""Check shared palette adapters, accessible role pairs, CSS drift and reader parity."""
import json
import re
import subprocess
import sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
subprocess.run([sys.executable, str(ROOT/'scripts/sync-color-contract.py'), '--check'], check=True)
colors=json.loads((ROOT/'design/tokens/colors.json').read_text())['colors']
def lum(value):
    rgb=[int(value[i:i+2],16)/255 for i in (1,3,5)]
    linear=[x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in rgb]
    return sum(a*b for a,b in zip(linear,[.2126,.7152,.0722]))
def contrast(a,b):
    lo,hi=sorted([lum(colors[a]),lum(colors[b])]);return (hi+.05)/(lo+.05)
pairs=[(a,b) for a in ['text','text-muted','primary','error','warning','gold'] for b in ['surface','surface-raised','surface-card']]
pairs += [('on-accent',b) for b in ['primary','primary-strong','hero','error']]
pairs += [('error','error-surface'),('warning','warning-surface')]
for a,b in pairs:
    ratio=contrast(a,b)
    assert ratio>=4.5, f'{a} on {b}: {ratio:.2f}:1 (requires 4.5:1)'
print(f'{len(pairs)} semantic text/background pairs pass WCAG AA normal-text contrast.')
paths=list((ROOT/'apps/web/src').rglob('*.scss'))
for p in paths:
    if p.name=='_colors.scss':continue
    assert not re.search(r'#[0-9a-fA-F]{3,8}\b|rgba?\(',p.read_text()),f'Raw UI color outside contract: {p}'
inline_paths=[]
for p in (ROOT/'apps/web/src').rglob('*.ts'):
    inline=re.findall(r'styles:\s*(?:\[\s*)?`([\s\S]*?)`',p.read_text())
    for css in inline:
        assert not re.search(r'#[0-9a-fA-F]{3,8}\b|rgba?\(',css),f'Raw inline UI color: {p}'
        inline_paths.append(p)
all_css='\n'.join(p.read_text() for p in paths+inline_paths)
defined=set(re.findall(r'(--[\w-]+)\s*:',all_css)); used=set(re.findall(r'var\((--[\w-]+)',all_css))
assert not used-defined, f'Undefined CSS roles: {used-defined}'
reader_a=(ROOT/'apps/ios/Sanctuary/Resources/readings-theme.js').read_text()
reader_b=(ROOT/'apps/android/app/src/main/assets/readings-theme.js').read_text()
assert reader_a==reader_b,'iOS and Android reading themes differ'
print(f'{len(paths)} web/admin stylesheets and {len(inline_paths)} inline style blocks use defined semantic colors.')
print('SwiftUI, Compose, web/admin and Android XML adapters match all 27 contract colors.')
