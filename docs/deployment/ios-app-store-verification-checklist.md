# iOS App Store Verification Checklist

This checklist exists so we verify the Apple-side release reality before moving the native app into the `sanctuary-platform` monorepo.

The goal is to prevent a structural migration from accidentally breaking the release path or obscuring who can actually ship the app.

## Current Known Values From The Legacy Xcode Project

Observed from the current standalone iOS project:

- Development team: `Z9L9BZFYLS`
- Production bundle identifier: `com.pamisu.Sanctuary`
- Development bundle identifier: `com.pamisu.Sanctuary.dev`
- UAT bundle identifier: `com.pamisu.Sanctuary.uat`
- Code signing style: automatic
- Marketing version: `1.0`
- Build number: `1`

These values should be verified against Apple systems, not just trusted because they exist in the project file.

## Verification Goals

- confirm the production app is owned by the correct Apple account
- confirm the bundle identifiers are registered and healthy
- confirm the app can still be archived and uploaded after migration
- confirm TestFlight and release permissions are clear
- document who can ship builds and where credentials live

## App Store Connect Checklist

- [x] Verify the production app record exists for `com.pamisu.Sanctuary`
- [x] Verify the displayed app name matches Sanctuary branding
- [ ] Verify the app record is owned by the correct Apple Developer organization/account
- [ ] Verify access roles for the people who need to ship builds
- [x] Verify TestFlight is enabled and accessible
- [x] Verify whether there is an active production version or only draft/test metadata
- [ ] Verify privacy, support, and marketing URLs are set or document what is still missing
- [ ] Verify who owns screenshots, description text, and release notes workflow

Verified from App Store Connect screenshots and operator confirmation:

- App name: `Sanctuary: Prayer & Peace`
- Bundle ID: `com.pamisu.Sanctuary`
- SKU: `sanctuary-ios-prod-001`
- Apple ID: `6759986068`
- iOS App Version `1.0` is present and ready for distribution in App Store Connect
- the native iOS app has already shipped publicly
- TestFlight has already been used for dev and UAT releases

## Certificates, Identifiers, And Profiles Checklist

- [ ] Verify `com.pamisu.Sanctuary` exists under Identifiers
- [ ] Verify `com.pamisu.Sanctuary.dev` exists under Identifiers
- [ ] Verify `com.pamisu.Sanctuary.uat` exists under Identifiers
- [ ] Verify the expected capabilities are enabled for each identifier
- [ ] Verify signing certificates are valid and not expired
- [ ] Verify provisioning profiles are valid if any manual overrides exist
- [ ] Verify automatic signing still resolves correctly on a machine that can archive

## Local Build And Archive Checklist

- [x] Open the legacy Xcode project and confirm it still resolves signing correctly
- [x] Confirm the expected build configurations map to dev, UAT, and production
- [x] Confirm a simulator build succeeds
- [ ] Confirm a device build succeeds
- [ ] Confirm an Archive can be produced without signing failures
- [ ] Confirm the Archive can be uploaded to TestFlight if needed

Verified locally:

- `xcodebuild -project Sanctuary.xcodeproj -scheme Sanctuary-Prod -configuration Debug -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build` succeeded
- `xcodebuild -project Sanctuary.xcodeproj -scheme Sanctuary-Dev -configuration Debug-Dev -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build` succeeded
- `xcodebuild -project Sanctuary.xcodeproj -scheme Sanctuary-UAT -configuration Debug-UAT -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build` succeeded

## Release Ownership Checklist

- [x] Document who can submit to App Store Connect today
- [ ] Document who can manage certificates/profiles if manual intervention is ever needed
- [ ] Document where release credentials are stored and who controls them
- [x] Confirm whether release is manual in Xcode today or already automated elsewhere
- [ ] Confirm whether we want to keep manual release first and automate later

Current understanding:

- release is currently manual rather than repository-automated
- the product owner has direct App Store Connect access and has already shipped production plus dev/UAT TestFlight builds

## Migration Gate

Do not move the iOS project into `apps/ios` until these statements are true:

- [x] We know the correct App Store Connect app record
- [ ] We know the correct Apple team and release owners
- [x] We know the bundle identifiers we are preserving
- [ ] We know the app can still be built and archived successfully
- [ ] We have written down any gaps that still need follow-up

## After Verification

The structural import into `apps/ios` is now complete.

The next platform-safe steps are:

1. keep validating archive/signing behavior
2. begin shared iOS environment/auth/API integration
3. migrate legacy local repositories to platform-backed repositories domain by domain
