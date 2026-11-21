"""Test precise-alarm onboarding with no account or saved reminders.

Requires reminder-upgrade-fixture.py on localhost:18084 and DEV APKs built
against http://10.0.2.2:18084. Supply --baseline once per old DEV APK (e.g.
1.0.39 and 1.0.40), --candidate, --tests, --adb, --aapt, --device emulator-N.

Only an isolated API 35 emulator is accepted. Fresh-case preparation uninstalls
the DEV package on that emulator. Upgrade cases preserve data between the old
APK and candidate with install -r. No production package or physical device is
accepted, and no app data is cleared during the upgrade being tested.
"""

import argparse
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import xml.etree.ElementTree as ET


PACKAGE = "com.pamisu.sanctuary.dev"
RUNNER = PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", type=Path, action="append", default=[])
    parser.add_argument("--candidate", type=Path, required=True)
    parser.add_argument("--tests", type=Path, required=True)
    parser.add_argument("--device", required=True)
    parser.add_argument("--adb", default=shutil.which("adb"))
    parser.add_argument("--aapt", required=True)
    parser.add_argument("--output", type=Path, default=Path(tempfile.mkdtemp(prefix="sanctuary-reminder-onboarding-")))
    args = parser.parse_args()
    if not args.device.startswith("emulator-") or not args.adb:
        parser.error("An isolated emulator and adb are required")
    versions = {}
    for apk in [*args.baseline, args.candidate, args.tests]:
        badging = subprocess.check_output([args.aapt, "dump", "badging", str(apk)], text=True)
        match = re.search(r"package: name='([^']+)' versionCode='[^']*' versionName='([^']*)'", badging)
        expected = PACKAGE + ".test" if apk == args.tests else PACKAGE
        if not match or match[1] != expected:
            parser.error(f"Unexpected package in {apk}")
        versions[apk] = match[2]
    args.output.mkdir(parents=True, exist_ok=True)

    def adb(*command):
        return subprocess.check_output([args.adb, "-s", args.device, *command], text=True, stderr=subprocess.STDOUT)

    if adb("shell", "getprop", "ro.build.version.sdk").strip() != "35":
        parser.error("This repository's Espresso version requires the API 35 test emulator")
    size = adb("shell", "wm", "size")
    density = adb("shell", "wm", "density")
    scale = adb("shell", "settings", "get", "system", "font_scale").strip()

    def instrument(name, scenario, log):
        output = adb("shell", "am", "instrument", "-w", "-e", "scenario", scenario,
                     "-e", "class", "app.sanctuary.android." + name, RUNNER)
        log.write(output)
        log.flush()
        if "OK (1 test)" not in output or "FAILURES!!!" in output:
            raise RuntimeError(f"{name} failed: {log.name}")

    cases = [("fresh-grant", None, 411, "1.0"), ("fresh-later", None, 411, "1.0"),
             ("fresh-grant-large", None, 320, "1.5"), ("decline", None, 411, "1.0"),
             ("allowed", None, 411, "1.0")]
    for baseline in args.baseline:
        cases.extend([("upgrade", baseline, 411, "1.0"), ("allowed", baseline, 411, "1.0")])
    try:
        for scenario, baseline, width, font_scale in cases:
            label = f"{versions[baseline] if baseline else 'fresh'}-{scenario}"
            with (args.output / (label + ".log")).open("w") as log:
                # Independent test installations; never applied to a real user's phone.
                adb("shell", "am", "force-stop", PACKAGE)
                subprocess.run([args.adb, "-s", args.device, "uninstall", PACKAGE], capture_output=True)
                adb("shell", "wm", "size", f"{width}x{max(800, width * 2)}")
                adb("shell", "wm", "density", "160")
                adb("shell", "settings", "put", "system", "font_scale", font_scale)
                log.write(adb("install", "-r", str(baseline or args.candidate)))
                log.write(adb("install", "-r", str(args.tests)))
                if not scenario.startswith("fresh"):
                    adb("shell", "pm", "grant", PACKAGE, "android.permission.POST_NOTIFICATIONS")
                    adb("shell", "appops", "set", "--uid", PACKAGE, "SCHEDULE_EXACT_ALARM", "allow" if scenario == "allowed" else "deny")
                    instrument("ReminderEmptyInstallFixtureTest", scenario, log)
                if baseline:
                    log.write(adb("shell", "am", "start", "-W", "-n", PACKAGE + "/app.sanctuary.android.MainActivity"))
                    adb("shell", "uiautomator", "dump", "/sdcard/reminder-baseline-empty.xml")
                    xml = adb("shell", "cat", "/sdcard/reminder-baseline-empty.xml")
                    nodes = ET.fromstring(xml).iter("node")
                    if any("Allow precise reminders" in node.get("text", "") for node in nodes):
                        raise RuntimeError("Baseline unexpectedly offered empty-cache precise-alarm setup")
                    log.write("Reproduced old build: general alerts enabled, zero reminders, no precise-alarm setup.\n")
                    log.write(adb("install", "-r", str(args.candidate)))
                    log.write(adb("shell", "am", "wait-for-broadcast-idle"))
                instrument("ReminderEmptyInstallUiTest", scenario, log)
                for suffix in ["", "-prompt"]:
                    if suffix and scenario == "allowed":
                        continue
                    adb("pull", f"/sdcard/Android/data/{PACKAGE}/files/reminder-empty-{scenario}{suffix}.png",
                        str(args.output / (label + suffix + ".png")))
            print(f"PASS {label}", flush=True)
    finally:
        adb("shell", "am", "force-stop", PACKAGE)
        old_size = re.search(r"Override size: (\S+)", size)
        old_density = re.search(r"Override density: (\S+)", density)
        adb("shell", "wm", "size", old_size[1] if old_size else "reset")
        adb("shell", "wm", "density", old_density[1] if old_density else "reset")
        if scale == "null":
            adb("shell", "settings", "delete", "system", "font_scale")
        else:
            adb("shell", "settings", "put", "system", "font_scale", scale)
    print(f"Results: {args.output}", flush=True)


if __name__ == "__main__":
    main()
