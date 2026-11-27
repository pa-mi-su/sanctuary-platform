# Mobile keyboard interaction audit

The bottom navigation belongs at the device bottom. A docked software keyboard covers it; it must never become an extra toolbar above the keyboard. Dismissing the keyboard restores navigation in its original position. Form content continues to use normal keyboard avoidance and scrolling.

## Implementation

- iOS observes the keyboard frame in the owning window's coordinate space. Only a keyboard covering the bottom edge suppresses the custom navigation inset; floating and hardware keyboards leave navigation available. No fixed screen sizes or keyboard-height offsets are used.
- Account forms scroll the focused row into view after keyboard layout. Password reveal restores focus after the replacement field mounts. The Me and account-required presentation paths share this behavior.
- Android uses IME insets to suppress bottom navigation while typing. Main content consumes Scaffold insets before applying IME padding. Standalone account, settings, and modal input containers also respect IME insets. The activity uses adjustResize.
- Search and prayer input containers were reviewed for keyboard placement. Authentication submission, validation, and networking behavior remain unchanged.

## Validation

- Both native development builds succeeded.
- Eight iOS geometry cases passed: docked, dismissed, home-indicator-only, hardware, floating tablet, landscape, off-window, and a phone without a bottom safe-area inset.
- The simulator keyboard harness passed nine assertions: initial bottom position, no lifted navigation for email/password, visible focused email/password, reveal retaining focus/keyboard and visibility, restoration after dismissal, and retained draft values. It compiles the production scrolling and keyboard observer components into an isolated app and never submits authentication.
- Android emulator checks confirmed visible focused email/password fields, navigation absent above the software keyboard, navigation restored at the device bottom after dismissal, retained email draft, and password reveal preserving focus and value. Synthetic credentials only; no account was submitted.
- Color contract and native theme checks passed.

## Repeating the iOS checks

Run `bash scripts/verify-ios-keyboard-ui.sh <booted-simulator-UDID>` on macOS with Xcode. The temporary harness installs under its own bundle ID and removes itself after the run. Geometry cases can be compiled with `KeyboardNavigationPolicy.swift` and `Scripts/verify-keyboard-navigation.swift`.

These checks cover keyboard layout and local input state, not end-to-end authentication or every physical device. Floating/hardware keyboard geometry is covered by policy checks rather than physical-device interaction.
