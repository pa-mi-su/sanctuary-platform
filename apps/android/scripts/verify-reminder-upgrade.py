"""Verify retained-data upgrades against a running reminder-upgrade-fixture.py.

Build the DEV baseline(s), candidate, and candidate instrumentation APK with
SANCTUARY_ANDROID_DEV_API_BASE_URL=http://10.0.2.2:18084. Use an isolated API 35
emulator; the runner intentionally reseeds the DEV app's account/reminders.
Pass --baseline once per old APK, --candidate, --tests, and --device emulator-N.
The real Android settings screen grants alarm access. Production packages and
physical devices are rejected. No app uninstall or clear-data operation is used.
"""

import argparse
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import urllib.request


PACKAGE = "com.pamisu.sanctuary.dev"
RUNNER = PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--baseline", type=Path, action="append", required=True)
    parser.add_argument("--candidate", type=Path, required=True)
    parser.add_argument("--tests", type=Path, required=True)
    parser.add_argument("--device", required=True)
    parser.add_argument("--adb", default=shutil.which("adb"))
    parser.add_argument("--aapt", required=True)
    parser.add_argument("--output", type=Path, default=Path(tempfile.mkdtemp(prefix="sanctuary-reminder-upgrade-")))
    args = parser.parse_args()
    if not args.device.startswith("emulator-") or not args.adb:
        parser.error("Use an isolated emulator and provide --adb when adb is not on PATH")
    args.output.mkdir(parents=True, exist_ok=True)
    versions = {}
    for apk in [*args.baseline, args.candidate, args.tests]:
        badging = subprocess.check_output([args.aapt, "dump", "badging", str(apk)], text=True)
        match = re.search(r"package: name='([^']+)' versionCode='[^']*' versionName='([^']*)'", badging)
        expected = PACKAGE + ".test" if apk == args.tests else PACKAGE
        if not match or match[1] != expected:
            parser.error(f"Refusing unexpected package in {apk}")
        versions[apk] = match[2]

    def adb(*command):
        return subprocess.check_output([args.adb, "-s", args.device, *command], text=True, stderr=subprocess.STDOUT)

    if adb("shell", "getprop", "ro.build.version.sdk").strip() != "35":
        parser.error("Use API 35 for this repository's Espresso version")
    old_size = adb("shell", "wm", "size")
    old_density = adb("shell", "wm", "density")
    old_scale = adb("shell", "settings", "get", "system", "font_scale").strip()

    def auth(status):
        request = urllib.request.Request("http://127.0.0.1:18084/fixture/auth",
                                         data=f'{{"status":{status}}}'.encode(),
                                         headers={"Content-Type": "application/json"})
        with urllib.request.urlopen(request, timeout=5) as response:
            assert response.status == 200

    def instrument(class_name, log, permission_allowed=False):
        output = adb("shell", "am", "instrument", "-w", "-e", "permissionAllowed", str(permission_allowed).lower(), "-e", "class",
                     "app.sanctuary.android." + class_name, RUNNER)
        log.write(output)
        log.flush()
        if "OK (1 test)" not in output or "FAILURES!!!" in output:
            raise RuntimeError(f"{class_name} failed; see {log.name}")

    try:
        for baseline in args.baseline:
            version = versions[baseline]
            for width, scale, mode in [(320, "1.0", "settings"), (411, "1.0", "settings"),
                                       (320, "1.5", "settings"), (411, "1.0", "offline"),
                                       (411, "1.0", "allowed")]:
                label = f"{version}-{width}-{scale}-{mode}"
                with (args.output / (label + ".log")).open("w") as log:
                    auth(200)
                    adb("shell", "am", "force-stop", PACKAGE)
                    adb("shell", "wm", "size", f"{width}x{max(800, width * 2)}")
                    adb("shell", "wm", "density", "160")
                    adb("shell", "settings", "put", "system", "font_scale", scale)
                    log.write(adb("install", "-r", "-d", str(baseline)))
                    log.write(adb("install", "-r", str(args.tests)))
                    adb("shell", "pm", "grant", PACKAGE, "android.permission.POST_NOTIFICATIONS")
                    # Model a previously used app, not an app explicitly force-stopped
                    # by the runner. Android suppresses broadcasts to stopped apps.
                    log.write(adb("shell", "am", "start", "-W", "-n", PACKAGE + "/app.sanctuary.android.MainActivity"))
                    adb("shell", "appops", "set", "--uid", PACKAGE, "SCHEDULE_EXACT_ALARM", "allow" if mode == "allowed" else "deny")
                    instrument("ReminderUpgradeFixtureTest", log, permission_allowed=mode == "allowed")
                    # This is the upgrade under test: same package, retained data.
                    log.write(adb("install", "-r", str(args.candidate)))
                    # Do not let starting instrumentation kill an in-flight upgrade receiver.
                    log.write(adb("shell", "am", "wait-for-broadcast-idle"))
                    if mode != "allowed":
                        dump = adb("shell", "dumpsys", "alarm")
                        pending = [block for block in re.split(r"\n(?=    (?:RTC|ELAPSED))", dump)
                                   if PACKAGE in block.splitlines()[0] and "ACTION_SHOW_REMINDER" in block]
                        if pending:
                            raise RuntimeError("Denied upgrade retained a native reminder alarm")
                    auth(503 if mode == "offline" else 200)
                    target_test = {"offline": "ReminderUpgradeOfflineUiTest", "allowed": "ReminderUpgradeAllowedTest"}.get(mode, "ReminderUpgradeUiTest")
                    instrument(target_test, log)
                    if mode == "settings":
                        adb("pull", f"/sdcard/Android/data/{PACKAGE}/files/reminder-layout.png",
                            str(args.output / (label + ".png")))
                print(f"PASS {label}", flush=True)
    finally:
        auth(200)
        adb("shell", "am", "force-stop", PACKAGE)
        size = re.search(r"Override size: (\S+)", old_size)
        density = re.search(r"Override density: (\S+)", old_density)
        adb("shell", "wm", "size", size[1] if size else "reset")
        adb("shell", "wm", "density", density[1] if density else "reset")
        if old_scale == "null":
            adb("shell", "settings", "delete", "system", "font_scale")
        else:
            adb("shell", "settings", "put", "system", "font_scale", old_scale)
    print(f"Results: {args.output}", flush=True)


if __name__ == "__main__":
    main()
