# Settings and About parity — 1.0.27

The Android About layout is the reference for iOS: matching section order, 34-point page headings, 24-point card headings, 16-point body text, 24-point outer margins, and compact full-width primary/secondary actions. iOS About buttons previously added vertical padding in both their labels and their button style; the dedicated About style applies it once. Support and Privacy use the same separate-card layout and back control.

Settings now has a prominent full-width Language card showing the current language on both native platforms. iOS presents a bottom sheet with the same three choices and selected-language checkmark as Android. Selecting a language updates the existing preference and dismisses the chooser.

About is only a Home menu item, not a Settings entry. The duplicate Announcements menu item is removed on iOS, Android, and web; the dedicated Home announcement button remains. Notification preferences and device controls stay in Settings. Signed-in native Settings orders language, account actions, and notification preferences consistently.

Android Settings uses its own bounded LazyColumn rather than sharing the generic dialog scroll container. Content includes 96 dp of bottom padding so the final controls, including expanded delivery details, can move above the system gesture area. The generic modal supports opting out of its default scrolling for this purpose. Android menu actions have a 56 dp minimum height.

Validation:

- Android DEV APK, 18 existing unit tests, and UAT/production Kotlin compilation passed.
- iOS DEV simulator build passed. Checked Menu, explicit Language card, bottom presentation, selected-language state, Spanish selection and English restoration, About, and Support navigation/back behavior.
- Android emulator checked the two-entry Menu, Language card/bottom sheet, no About in Settings, and scrolling to the final expanded delivery controls.
- Web: 55 existing tests and production build passed; existing stylesheet-budget/CommonJS warnings remain.
- No API, database, authentication, notification delivery, or reminder behavior changes. No live notifications sent.

Preview images are from the isolated QA simulator/emulator. Signed-in layout uses the same Settings container; the visual QA session was signed out and did not alter a real account.
