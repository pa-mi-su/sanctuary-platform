#!/bin/bash
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
device="${1:?Pass a booted iOS simulator UDID}"
qa_dir="$(mktemp -d /tmp/sanctuary-keyboard-qa.XXXXXX)"
bundle=com.pamisu.Sanctuary.KeyboardQA
app="$qa_dir/KeyboardQA.app"
mkdir -p "$app"
xcrun --sdk iphonesimulator swiftc -parse-as-library -sdk "$(xcrun --sdk iphonesimulator --show-sdk-path)" \
  -target "$(uname -m)-apple-ios16.6-simulator" -module-cache-path "$qa_dir/module-cache" \
  "$root/apps/ios/Sanctuary/UI/KeyboardAwareScrollView.swift" \
  "$root/apps/ios/Sanctuary/UI/KeyboardNavigationPolicy.swift" \
  "$root/apps/ios/Sanctuary/UI/KeyboardOcclusionObserver.swift" \
  "$root/apps/ios/Scripts/KeyboardNavigationHarness.swift" -o "$app/KeyboardQA"
python3 - "$app/Info.plist" <<'PY'
import plistlib,sys
with open(sys.argv[1],'wb') as f:
 plistlib.dump({'CFBundleIdentifier':'com.pamisu.Sanctuary.KeyboardQA','CFBundleExecutable':'KeyboardQA',
 'CFBundleName':'KeyboardQA','CFBundlePackageType':'APPL','CFBundleVersion':'1','CFBundleShortVersionString':'1.0',
 'MinimumOSVersion':'16.6','UIDeviceFamily':[1,2],'UILaunchScreen':{},
 'UIApplicationSceneManifest':{'UIApplicationSupportsMultipleScenes':False,'UISceneConfigurations':{}}},f)
PY
codesign --force --sign - "$app" > /dev/null
xcrun simctl install "$device" "$app"
trap 'xcrun simctl terminate "$device" "$bundle" >/dev/null 2>&1 || true; xcrun simctl uninstall "$device" "$bundle" >/dev/null 2>&1 || true' EXIT
xcrun simctl launch "$device" "$bundle"
container="$(xcrun simctl get_app_container "$device" "$bundle" data)"
for ((attempt=0;attempt<120;attempt++)); do
  if [[ -f "$container/Documents/keyboard-results.json" ]]; then break; fi
  sleep 0.25
done
python3 - "$container/Documents/keyboard-results.json" <<'PY'
import json,sys
with open(sys.argv[1]) as f: result=json.load(f)
print(json.dumps(result,indent=2))
if not result['passed']:sys.exit(1)
PY
