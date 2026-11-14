# Android screen parity audit

Android 1.0.21 aligns the app-owned layouts with the current iOS design: visible rounded panels, consistent borders and spacing, framed hero images, stronger headings, clear action pills, and the Sanctuary blue palette. This change is Android-only.

## Coverage

| Screen family | Changes and checks |
| --- | --- |
| Home and navigation | Shared card surfaces, spacing, buttons, house icon and calendar/clock tab icons. Home visually checked in the emulator. |
| Novenas calendar | Framed date header with working date jump, Day/Week/Month controls, image-backed event cards and seasonal legend. All three modes and date selection visually checked. |
| Saints and liturgical calendars | Same shared calendar header, controls, grid and card system. Saints Day and liturgical Day/Month visually checked. |
| Novena details | Hero before title, grouped actions, duration/start/end chips, section dividers and framed day selector. St. Cecilia visually checked. Dates use the existing public calendar-window endpoint and selected calendar year. |
| Saint details | Framed hero, title/action panel and outlined biography/metadata sections. St. Frances Xavier Cabrini visually checked. |
| Prayer details | Framed image, title/actions panel and prayer section. Apostles' Creed visually checked, including the signed-out reminder account prompt. |
| Search, Rosary, patronages and intentions | Shared search fields, result cards, panels and buttons audited in source; prayer search visually checked. Rosary uses the shared prayer detail renderer. |
| Me and notifications | Existing account-only reminder rules retained; shared panel/button styling applies to account, notifications, device details and scheduled reminders. Signed-out Me visually checked. |
| Account forms | Shared AuthCard, field, primary/secondary button, banner and password components cover login, registration, confirmation and password recovery. Login and registration visually checked without submitting credentials. |
| About, documents and language picker | Shared card/sheet/button renderers audited in source. |
| Daily readings | Calendar/readings card visually checked; embedded publisher content remains publisher-controlled. |
| Loading, empty, error and dialogs | Shared sheet/card/border/palette changes cover these states. Loading and account prompt observed; native date picker opened and date selection checked. |

## Validation

- `:app:assembleDevDebug` passed.
- `:app:testDevDebugUnitTest` passed: 16 tests, zero failures.
- `:app:compileUatDebugKotlin` and `:app:compileProdDebugKotlin` passed.
- `git diff --check` passed.
- Visual checks used an isolated Pixel 8 Android emulator and real DEV public content. Firebase test-lab exclusion was enabled; no announcement registration, push send, account creation or database edit was performed.

## Previews

- [Novena calendar](previews/novena-calendar.png)
- [Novena detail](previews/novena-detail.png)
- [Prayer detail](previews/prayer-detail.png)

The novena previews capture the layout pass before the final action-icon refinement; the prayer preview includes the final heart/bell controls.

## Remaining device verification

A physical Android/iPhone comparison is still needed at matching font/display settings. Native fonts, OS pickers, system bars and different screen sizes are not pixel-identical. Authenticated account management and reminder delivery were not exercised in this visual pass; their existing behavior and tests remain in place. Source coverage above does not imply every live route was individually opened.
