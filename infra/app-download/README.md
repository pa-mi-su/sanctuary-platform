# Sanctuary permanent app-download link

Printed URL: **https://mydailysanctuary.com/app**. Keep this endpoint for the lifetime of the books, cards, and garments that contain it.

## Behavior

CloudFront viewer-request function `sanctuary-app-download`, JavaScript runtime 2.0:

- Exact `/app` and `/app/` requests only.
- iPhone, iPad, and iPod user agents: HTTP 302 to the owner-supplied Apple listing.
- Android user agents: HTTP 302 to the owner-supplied Google Play listing.
- Unknown devices and desktop-style iPad user agents: accessible HTML containing both store buttons; no guessing.
- Other paths: return the original request unchanged.
- GET and HEAD supported; other methods at the app endpoint receive 405.
- No-store headers. Targets are fixed in source; query parameters cannot supply a redirect URL.
- No advertising identifiers, additional analytics, cookies, QR subscriptions, or dependencies on an origin-page deployment.

The redirect is served before the website origin. Known phones receive no intermediate HTML page. Phone/browser security UI is outside Sanctuary's control. Neither this service nor a QR code can guarantee detection of every masked user agent, an available app store in every country, or successful installation on an unsupported device.

## Status and validation

Published and attached to production after owner approval on October 20, 2026. CloudFront reports Deployed. Live Apple/Android redirects and desktop fallback are HTTP-verified. Local behavioral tests pass, and AWS-native tests pass for Apple, Android, desktop fallback, and unrelated-path pass-through. QR artwork and rendered PDF samples were independently decoded. Physical paper/fabric and real-phone camera scans still require testing before print production.

## Production control

The repository's `docs/architecture/deployment-and-pipelines.md`, under Release Safety Rules, says: **“production deploys require approval.”** The owner approved the initial activation on October 20, 2026. Obtain approval for future production changes.

Run from the repository checkout, using an existing authorized AWS session:

```
node infra/app-download/test-edge.cjs
python3 infra/app-download/manage.py plan --backup-dir /absolute/path/to/new-backup-directory
```

After approval:

```
python3 infra/app-download/manage.py activate-production --backup-dir /absolute/path/to/new-activation-backup-directory
```

The script confirms account and production aliases, refuses to replace an existing unrelated viewer-request function or Lambda, checks AWS-native routing tests, backs up the current config, publishes the function, and associates it using the distribution's ETag. It changes only the default behavior's function association. No S3 content, Namecheap DNS, databases, or app binaries are modified. A future ordered cache behavior requires review before use of this script. Do not reuse an existing backup directory.

Poll CloudFront until the distribution is Deployed. Fetch `/app` with Apple, Android, and desktop user agents, and confirm the two fixed store URLs and fallback buttons. Test the homepage and an existing content page. Scan the actual-size paper proof with real phones before removing activation notes from the book or ordering merchandise.

Rollback (fresh backup directory):

```
python3 infra/app-download/manage.py rollback-production --backup-dir /absolute/path/to/new-rollback-backup-directory
```

Rollback removes only this function's association from the current configuration; it does not restore stale unrelated fields. It intentionally leaves the function resource for inspection. To change store URLs later, update `edge.js`, rerun tests, update the development function, and approve/publish the new version. The printed QR remains unchanged.
