# Mobile appearance audit

Scope: every iOS SwiftUI screen and shared UI component, every Android Compose screen/component, and native Android theme resources. Web is a separate client and was not modified.

The mobile palette is now shared across Home and all other native screens: sage `#D8E3D9`, raised sage `#C4D5C8`, light card sage `#E3EBE2`, dark teal text `#173D36`, secondary text `#465B4D`, teal actions `#24634F`, and deep teal editorial/image backgrounds `#073C3B`.

## Screen and state inventory

All rows below received a source audit, including explicit foregrounds and surfaces outside the shared theme.

| Screen family | Screens and states covered | iOS implementation | Android implementation |
| --- | --- | --- | --- |
| Home | Welcome and feature slides, shortcuts, verse, content rows, looping/pause controls, bottom safe area | `HomeView.swift`, `AppShellView.swift` | `HomeLanding`, `HomeWelcomeSlide`, `HomeFeatureShelf`, `AuthenticatedShell` |
| Search | Saints, novenas, prayers, rosaries, intentions and patronage; input, placeholder, results, loading, empty/error, selected terms | `SearchViews.swift`, `PrayerViews.swift`, list views | `SearchCard`, `ContentCard`, `TermSearchSheet`, `SearchListSheet`, calendar search components |
| Saint detail | Image, title, metadata, biography, favorite states, sharing, reminder/account gate, related content | `SaintDetailView.swift` | `SaintDetailSheet` |
| Novena detail | Metadata, intentions, active/inactive day controls, day content, start/complete/stop, favorite states | `NovenaDetailView.swift` | `NovenaDetailSheet` |
| Prayer and rosary detail | Images, expandable image, categories, text, favorite/share/reminder actions | `PrayerViews.swift` | `PrayerDetailSheet`, `ExpandedPrayerImageDialog` |
| Calendars | Novenas and saints; day/week/month (Liturgical tab temporarily hidden), year selection, mode selection, date navigation, empty/loading/error, seasonal markers | `CalendarViews.swift` | `CalendarSurface`, grids, cells, preview cards and season legend |
| Me | Guest/account header, progress, favorites, empty states, saved content/reminders, logout and delete confirmation | `MeView.swift` | `MeScreen`, identity/profile/section/linked rows |
| Authentication | Entry choices, sign-in, registration, confirmation, forgot/reset password, validation rules, reveal/clear, busy/disabled states and account-required prompts | `AccountAccessView.swift` | `AccountAccessScreen`, `AccountAccessContent`, auth cards, password panel and buttons |
| Settings | Account, General, language selection, notifications, permission recovery, Support & Legal, About and version | `MeView.swift`, `HomeView.swift`, `AboutView.swift` | settings shell, language and notification components, About sheets |
| Announcements | Inbox, list/detail, dates/body copy, empty/error/loading, refresh/more/clear/undo, clear confirmation | `AnnouncementInboxView.swift`, shell invitation | `AnnouncementInbox.kt`, invitation and inbox sheet |
| Reminders | Add/edit/remove, time selection, saving, recovery prompts, timing warnings, reminder list and destructive controls | `NovenaReminderScheduler.swift`, `MeView.swift` | reminder controls/setup/list components; native date/time picker themes |
| Parish | Search, map wrapper, place list/detail and actions | `ParishFinderView.swift` | No corresponding Android parish screen |
| Support/legal | About, support, privacy and terms/native document wrappers | `AboutView.swift` | About/document sheets |
| Shared UI | Loading/error cards, back controls, navigation rows, text fields, primary/secondary/destructive controls, sheet backgrounds | `AppTheme.swift`, `StickyBackHeader.swift`, `SanctuaryLoadingCard.swift` | `Color.kt`, `Theme.kt`, shared components in `MainActivity.kt` |
| Launch and identity | App icon at every native size, adaptive/themed Android icon, in-app brand mark, launch backgrounds | AppIcon/BrandLogo/LaunchBackground asset catalogs and launch plist | Launcher mipmaps, adaptive XML, monochrome vector, splash/native XML themes |

## Validation

- Both native development builds compile.
- `python3 scripts/verify-mobile-theme.py` verifies matching platform tokens, minimum 4.5:1 contrast for body/secondary/error text on relevant sage surfaces, icon dimensions, iOS marketing-icon opacity, and Android themed-icon references.
- Primary body text on the main surface: 9.05:1. Secondary text on the main surface: 5.55:1; on raised sage: 4.78:1.
- Loading/search headers keep top alignment when results are not yet available. Failed iOS prayer requests show an error and retry action instead of an empty list. GET requests retry a transient connection failure once. Live API health, announcements and Rosary endpoints returned HTTP 200 through curl and Apple URLSession; the reported earlier transport failure was not reproduced.
- Source sweep removed legacy navy panels, cyan interface accents and light-on-light text. Filled teal/red actions and photographic/editorial overlays deliberately retain contrasting cream text.
- Android Saints day/month, restored search and legend, Prayers, Rosary, and the themed readings viewer were checked against live DEV content. Prayer detail was also visually checked. Home and the new icon were inspected at simulator/native-icon sizes. The complete authenticated interaction matrix was source-audited; it was not exercised with a signed-in account.

Daily readings now use an embedded native web viewer with a host-scoped sage stylesheet for bible.usccb.org on both platforms; publisher content and links remain intact. Other external websites retain their own styling. Photographs, illustrations, system-owned UI, and canonical liturgical season colors keep their content/semantic colors.

## Icon provenance

The launch image uses safe-area aspect fit on iOS and native splash bounds on Android, without fixed image sizes or manual insets.

`app-icon-generation.json` records the built-in image-generation prompt and reference. `sanctuary-app-icon-source.png` is the retained master. Run `python3 scripts/generate-mobile-icons.py` on macOS to regenerate the native launcher sizes and matching in-app logo assets.
