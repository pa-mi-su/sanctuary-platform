# Native calendar regression audit — November 27, 2026

Compared `origin/main`, `origin/dev`, and `codex/home-feature-carousel`. This follows the web-only correction and specifically restores native calendar regressions now reported on Android and iOS.

## Confirmed regressions and fixes

| Area | Regression compared with main | Correction |
| --- | --- | --- |
| Android and iOS day cards | Seasonal card outline removed; only a small dot remained | Restore the seasonal outline around the card |
| Android and iOS week/month | Ordinary date outlines removed, leaving only a thin today outline | Restore season-colored outlines on every date; gold outline for today; retain teal selection fill |
| Android week | Shared cell ignored its supplied height and always used 60 dp | Honor the supplied height (80 dp week / 60 dp month) |
| Android legend | Framed container removed; legend below calendar content; tiny labels/dots | Restore a framed panel above content, with wrapping entries, larger labels/dots, and outlined pale markers |
| iOS legend | Bare legend below content, with horizontal-or-entirely-vertical fallback | Framed adaptive grid above content; labels can wrap without being covered by bottom navigation |
| iOS calendar swipe | Main's horizontal swipe handler was removed during restyling | Restore horizontal previous/next gestures on calendar content, rejecting vertical drags |

## Behavior review

- Calendar previous/next/today handlers, date-picker selection, day/week/month mode changes, and date-cell selection still call the existing calendar state operations. Android swipe handling remains connected. iOS swipe handling is restored.
- Saints and Novenas search actions remain connected and visible in the scroll content. Search/filter screens retain their existing search callbacks. Rosary intentionally omits search as previously requested.
- Saint/novena detail changes compared with main are presentation changes; favorite and reminder callbacks are retained.
- Android data/repository sources have no changes in this feature comparison. iOS API request construction and mutation contracts are retained; the previously introduced one-time transient GET retry is an intentional nonvisual change from the earlier connectivity request, not a calendar change.
- Reminder scheduler changes in the feature comparison are UI styling. Reminder time conversion, account isolation, and progress mutations were exercised by the existing regression checks.
- Intentional earlier scope includes Home/carousel, shared theme, launch/icon artwork, temporary Liturgical navigation removal, Rosary search removal, readings presentation, and keyboard/form fixes. These were not rolled back by this audit.

## Validation

Both native development builds passed. Android's 55 unit tests passed with no failures/errors, including calendar swipe, reminder presentation/timing/policy, links and account/inbox tests. iOS checks passed for reminder clocks (12 time/zone cases and DST), progress updates/rollback/account isolation, inbox receipts/logout/undo, notification consent/recovery, and platform configuration. Color adapters, 24 contrast pairs, native theme checks, and diff whitespace checks passed.

Native simulator UI access through the available computer-use surface was unavailable. Build and logic checks do not establish complete physical-device or end-to-end authentication coverage. This audit does not claim every possible screen interaction is verified. No production deployment or store release is performed by this fix.
