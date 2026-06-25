# Android Play Console Setup

This is the exact setup needed to take the Android app on `codex/android-dev-distribution`
from "builds locally and in GitHub" to "uploads to Google Play tracks".

## Current Android release model

The Android project now uses one package name across all flavors:

- `com.pamisu.sanctuary`

Track mapping in GitHub Actions:

- `dev` branch push -> Play **internal** track
- `uat` branch push -> Play **closed testing** track (`beta`)
- `main` branch push -> Play **production draft**

Workflow file:

- [`/Users/pms/repos/sanctuary-platform/.github/workflows/android-pipeline.yml`](/Users/pms/repos/sanctuary-platform/.github/workflows/android-pipeline.yml)

## What already works

The project already builds:

- `assembleDevDebug`
- `assembleUatDebug`
- `bundleDevRelease`
- `bundleUatRelease`
- `bundleProdRelease`

So the remaining work is credentials and Play Console access, not Android code structure.

## Step 1: Create the Android app in Play Console

In Google Play Console:

1. Create one app named `Sanctuary`
2. Use package name:
   - `com.pamisu.sanctuary`
3. Complete the minimum app creation fields
4. Make sure these tracks exist:
   - Internal testing
   - Closed testing
   - Production

## Step 2: Create a Google service account for Play uploads

1. Open [Google Play Console](https://play.google.com/console)
2. Open:
   - `Setup`
   - `API access`
3. Link a Google Cloud project if one is not linked yet
4. Create a **service account**
5. Grant it Play Console access to this app

Recommended permission scope:

- release management / app releases access sufficient to upload builds to:
  - internal
  - closed testing
  - production draft

After that:

6. Create a JSON key for the service account
7. Keep the raw JSON contents for GitHub secret setup

## Step 3: Create the Android upload keystore

Run this locally:

```bash
keytool -genkeypair \
  -v \
  -keystore sanctuary-upload-key.jks \
  -alias sanctuary-upload \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Choose and record:

- keystore password
- key alias
- key password

Then convert the keystore to base64:

```bash
base64 -i sanctuary-upload-key.jks | pbcopy
```

That copied value is what GitHub will store.

## Step 4: Add GitHub secrets

Repo:

- `pa-mi-su/sanctuary-platform`

Add these GitHub **repository secrets**:

- `ANDROID_UPLOAD_KEYSTORE_BASE64`
- `ANDROID_UPLOAD_KEYSTORE_PASSWORD`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`
- `ANDROID_PLAY_SERVICE_ACCOUNT_JSON`

Notes:

- `ANDROID_UPLOAD_KEYSTORE_BASE64`
  - base64 text of `sanctuary-upload-key.jks`
- `ANDROID_PLAY_SERVICE_ACCOUNT_JSON`
  - paste the raw JSON service account key content

## Step 5: Add GitHub variable

Add this repository variable:

- `ANDROID_PLAY_PACKAGE_NAME`

Value:

- `com.pamisu.sanctuary`

The workflow already defaults to this package name, but storing it as a variable is cleaner.

## Step 6: First end-to-end test

After the secrets and variable are in place:

1. merge Android work to `dev`
2. the Android workflow should:
   - build `app-dev-release.aab`
   - upload it as a GitHub artifact
   - upload it to Play **internal** track

Then:

3. promote to `uat`
4. the Android workflow should:
   - build `app-uat-release.aab`
   - upload it as a GitHub artifact
   - upload it to Play **closed testing**

Then:

5. promote to `main`
6. the Android workflow should:
   - build `app-prod-release.aab`
   - upload it as a GitHub artifact
   - upload it to Play **production** as a **draft**

## Step 7: Important release behavior

The Android workflow is path-scoped.

That means:

- Android changes trigger Android
- API changes trigger API
- web changes trigger web
- iOS changes trigger iOS

So Android releases stay independent from the other app surfaces.

## What is still intentionally unfinished

This Android app is now testable, but not complete.

Current scope:

- auth/session foundation
- live saints list
- live novenas list
- dev/uat/prod build lanes

Still to build later:

- deeper content detail views
- favorites
- novena progress
- reminders
- liturgical calendar
- prayers
- intentions UX parity

## Final blocker summary

Code/build side:

- ready for Play-track automation

Operational side still needed:

- Play app created
- service account created and granted access
- upload keystore created
- GitHub secrets added

Once those are done, the GitHub workflow is ready to perform real Android track uploads.
