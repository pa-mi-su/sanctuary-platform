# Cross-platform color audit

24 November 2026 · Public web, admin, iOS and Android · Working branch `codex/home-feature-carousel`

Authority: [color contract](color-contract.md) and [machine-readable values](../../design/tokens/colors.json).

## Coverage and corrections

| Client | Source coverage | Corrections |
| --- | --- | --- |
| Public web | App shell, header/footer, mobile navigation, Home, Saints, Novenas, Liturgical, Prayers/Rosary, term search, content details, Me, auth, reminders, announcements, About and legal | Replaced the navy canvas and glass panels with sage; changed body/secondary text, fields, focus, controls, selected/completed states and overlays to semantic roles. Kept deep teal editorial areas with cream text. Selected calendar captions now remain readable; calendar cells use restrained season dots. |
| Admin | Session/login, verification/password forms, content library/editor, inputs and image previews, taxonomy picker, users/activity/audit views, confirmations, announcement/message composer and history | Shared sage canvas/cards, dark labels, primary actions, disabled states, status/error/warning surfaces, focus and dialog colors. No separate admin palette. |
| iOS | All native screen families listed in [mobile audit](mobile-appearance-audit.md), plus shared theme and launch resources | Removed remaining system-red/orange text and unread indicators; unified reminder controls, error surfaces and editorial roles. Native palette declarations and readings/launch colors now synchronize from the contract. |
| Android | Compose screens, account/delete dialog, reminders/switches, announcements, calendars, theme and native XML surfaces | Fixed white delete-dialog title on a light card; replaced alternate switch/reminder greens and unread red; standardized season and error-container roles. Native theme XML now references generated color resources. |

The same approved artwork/logo is used on web/admin and the native clients. Intrinsic artwork, photographs, avatar drawing colors, QR canvases, liturgical markers and external/system-owned surfaces have documented exceptions. This audit aligns appearance roles, not every platform's navigation or layout feature set.

## Automated validation

- `python3 scripts/sync-color-contract.py --check`: checks the shared adapters against all 27 contract values, without writing changes.
- `python3 scripts/verify-color-contract.py`: 24 approved normal-text/background pairs meet at least 4.5:1; 18 web/admin stylesheets and 3 inline style blocks contain no raw UI colors; referenced CSS roles exist; embedded native readers match.
- `python3 scripts/verify-mobile-theme.py`: native palette parity, primary contrast pairs and launcher asset contracts pass.
- Existing web/admin unit suite: **10 files, 69 tests passed**.
- Production Angular build, iOS `Sanctuary-Dev` build and Android `assembleDevDebug` all passed.

The Angular component style warning threshold is 14 kB; the admin component currently remains below its existing 18 kB error limit. The build may also report the existing `qrcode` CommonJS warning. Budget limits were not raised.

## Browser and device review

Reviewed the local public Home, Saints day/month, Novenas week, account choices/login form, Prayers list/detail and admin login against DEV content. Read-only computed-style contrast sampling passed after correcting the selected calendar caption. This sampling is a useful spot check, not a replacement for the role-pair checks or a formal accessibility certification.

Desktop browser captures:

- [Public web](previews/web-sage.jpg)
- [Admin login](previews/admin-sage.jpg)

The browser viewport override did not change its actual 1265 px viewport, so phone-width web CSS was source-reviewed rather than claimed as visually tested. Native Home/detail/calendar/reading checks from the prior mobile audit remain documented separately; the synchronized native builds were rebuilt and installed for this pass. The complete authenticated admin and native interaction matrix was source-audited, not exercised with account credentials. No production deployment, content mutation, account change or announcement send was performed.

## Future drift prevention

Edit `design/tokens/colors.json`, run the synchronizer, then run both verification scripts and the relevant builds. New web/admin components must reference semantic roles. New native colors need explicit adapter mappings; do not copy a hex value into a screen. Review standard, disabled, focused, selected, loading, empty and error states whenever a surface or foreground changes.
