# iOS build caching and simulator validation

The iOS workflow keeps its existing `macos-15-intel` runners, Xcode selection, signing settings, device archive targets, and release triggers.

## Reuse

- Swift Package Manager checkouts/artifacts use a dedicated `SanctuarySourcePackages` directory. Cache identity includes runner OS/architecture, the selected Xcode version/build, and `Package.resolved`. Validation saves this cache before a dependent upload job starts.
- Simulator DerivedData and device compilation intermediates have separate caches. Simulator keys include the resolved scheme; device keys include the DEV/UAT/production job. Both include dependency/configuration fingerprints and commit SHA, with a compatible-prefix restore key for incremental builds.
- Xcode remains responsible for invalidating changed source and project settings. Cache prefixes deliberately exclude marketing-version-only project changes so routine release bumps do not discard reusable compilation work.
- Device caches include only compilation/module/SDK intermediates. Signing keychains, API keys, certificates, provisioning-profile directories, exported IPAs, and archives are outside the cached paths.

The first cache population is still cold. PR caches are scoped by GitHub and cannot be assumed to seed DEV caches; subsequent compatible runs can reuse their own accessible cache. Actual improvement must be measured in CI, not inferred from local warm builds.

## Validation

Simulator validation passes `ARCHS="$(uname -m)" ONLY_ACTIVE_ARCH=YES`, matching INSTEAD's approach. The signed device archive retains `generic/platform=iOS` and has no simulator architecture override. Both builds print Xcode build timing summaries.

Workflow-only PRs now execute simulator validation to check build-command changes. Workflow-only pushes still do not select an upload/deployment, preserving existing release behavior. Admin-only changes remain isolated from native builds.

Checks: the workflow regression suite covers 12 cases / 204 assertions, including runner preservation, separate cache identities, architecture isolation, signing safeguards, and workflow-trigger behavior. Single-architecture simulator builds passed twice locally using explicit package/DerivedData paths. The next PR run validates the Intel simulator architecture in CI.

## Badge follow-up scope

The reported missing iPhone icon badge was being checked on an app that had not been updated. Build 1.0.28 (199) already contains the badge fix; this branch leaves all application code and version settings unchanged. Update that build and send a fresh background alert with iOS Badges enabled before investigating a new device-specific fault.
