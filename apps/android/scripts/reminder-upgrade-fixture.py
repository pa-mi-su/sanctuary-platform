"""Local-only fixture for ReminderUpgradeFixtureTest and ReminderUpgradeUiTest.

Start this server, build both DEV APKs with SANCTUARY_ANDROID_DEV_API_BASE_URL
set to http://10.0.2.2:18084, and use a disposable emulator (API 35 supports
this repository's Espresso version). Install the 1.0.38 DEV APK, grant
POST_NOTIFICATIONS, and run ReminderUpgradeFixtureTest. Deny precise alarms
with `adb shell appops set --uid com.pamisu.sanctuary.dev SCHEDULE_EXACT_ALARM deny`.
Install the candidate with `adb install -r` WITHOUT uninstalling/clearing data,
install its test APK, and run ReminderUpgradeUiTest. Repeat at 320 and 411 dp
and font_scale 1.5. The reminders endpoint always fails while auth and content
remain available. All credentials and tokens here are local test values.
"""
from pathlib import Path
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse
import argparse
import base64
import json
import time


PROFILE = {'userId': '00000000-0000-0000-0000-000000000010',
 'email': 'qa@example.test',
 'firstName': 'QA',
 'lastName': 'Account',
 'displayName': 'QA Account',
 'preferredLanguage': 'en',
 'timeZoneId': 'America/New_York',
 'novenaRemindersEnabled': False,
 'feastRemindersEnabled': False,
 'emailUpdatesEnabled': False,
 'onboardingCompleted': True,
 'favoriteSaintCount': 0,
 'favoriteNovenaCount': 0,
 'favoritePrayerCount': 0,
 'activeNovenaCount': 0,
 'completedNovenaCount': 0}

REMINDERS = [
    dict(kind="prayers", slug="qa-prayer", title="Devotion to the Seven Sorrows of Mary", hour=15, minute=0),
    dict(kind="novenas", slug="qa-novena", title="A novena with a deliberately long title for narrow screen layout", hour=15, minute=5),
]


def encode(value):
    return base64.urlsafe_b64encode(json.dumps(value).encode()).decode().rstrip("=")


class UpgradeHandler(BaseHTTPRequestHandler):
    def send_json(self, value, status=200):
        self.send_bytes(json.dumps(value).encode(), "application/json", status)

    def send_bytes(self, body, content_type, status=200):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        path = urlparse(self.path).path
        if path == "/me/reminders":
            return self.send_json({"message": "Local fixture: reminder API unavailable"}, 503)
        if path == "/me":
            time.sleep(getattr(self.server, "auth_delay_ms", 0) / 1000)
            if getattr(self.server, "auth_status", 200) != 200:
                return self.send_json({"message": "Local fixture authentication outage"}, self.server.auth_status)
            return self.send_json(PROFILE)
        if path == "/me/announcements":
            return self.send_json(dict(readIds=[], clearedIds=[], clearedBefore=0))
        if path == "/announcements":
            return self.send_json(dict(items=[], hasMore=False))
        if path == "/fixture.png":
            asset = Path(__file__).resolve().parents[2] / "web/public/brand-logo.png"
            return self.send_bytes(asset.read_bytes(), "image/png")
        if path.startswith(("/content/prayers/", "/content/novenas/")):
            record = next((r for r in REMINDERS if r["slug"] == path.split("/")[-1]), None)
            if record is None:
                return self.send_json({"message": "Unknown fixture content"}, 404)
            image_url = f"http://10.0.2.2:{self.server.server_port}/fixture.png"
            return self.send_json(dict(record, id=record["slug"], imageUrl=image_url,
                                       body="Local fixture prayer", category="devotion",
                                       description="Local fixture novena", durationDays=9, days=[]))
        return self.send_json([])

    def do_POST(self):
        body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
        if urlparse(self.path).path == "/fixture/reminder-mutations":
            status = json.loads(body)["status"]
            if status not in (200, 503):
                return self.send_json({"message": "Unsupported fixture status"}, 400)
            self.server.reminder_mutation_status = status
            return self.send_json({"status": status})
        if urlparse(self.path).path == "/fixture/auth":
            status = json.loads(body)["status"]
            if status not in (200, 401, 503):
                return self.send_json({"message": "Unsupported fixture status"}, 400)
            self.server.auth_status = status
            self.server.refresh_status = json.loads(body).get("refreshStatus", status)
            self.server.auth_delay_ms = json.loads(body).get("delayMs", 0)
            return self.send_json({"status": status})
        if urlparse(self.path).path in ("/auth/login", "/auth/refresh"):
            status = getattr(self.server, "refresh_status" if urlparse(self.path).path == "/auth/refresh" else "auth_status", 200)
            if status != 200:
                return self.send_json({"message": "Local fixture authentication outage"}, status)
            # Deliberately unsigned token, accepted only by this local fixture.
            token = encode({"alg": "none"}) + "." + encode(dict(
                sub=PROFILE["userId"], exp=int(time.time()) + 86400, email=PROFILE["email"]
            )) + ".fixture"
            return self.send_json(dict(accessToken=token, idToken=token,
                                       refreshToken="local-fixture-refresh", tokenType="Bearer",
                                       expiresIn=86400, email=PROFILE["email"], displayName=PROFILE["displayName"]))
        return self.send_json(dict(readIds=[], clearedIds=[], clearedBefore=0))

    def do_PUT(self):
        self.rfile.read(int(self.headers.get("Content-Length", 0)))
        if urlparse(self.path).path == "/me/reminders":
            return self.reminder_mutation()
        self.send_json({"message": "Local fixture rejects registration"}, 503)

    def do_DELETE(self):
        if urlparse(self.path).path.startswith("/me/reminders"):
            return self.reminder_mutation()
        self.send_json({"message": "Unknown fixture endpoint"}, 404)

    def reminder_mutation(self):
        if getattr(self.server, "reminder_mutation_status", 200) == 503:
            return self.send_json({"message": "Local fixture rejects reminder write"}, 503)
        return self.send_bytes(b"", "application/json", 204)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=18084)
    args = parser.parse_args()
    ThreadingHTTPServer(("127.0.0.1", args.port), UpgradeHandler).serve_forever()
