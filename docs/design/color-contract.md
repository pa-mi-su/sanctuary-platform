# Sanctuary color contract

Version 1 · Light appearance · 24 November 2026

This contract applies to the public web app, admin panel, iOS and Android. The machine-readable authority is [`design/tokens/colors.json`](../../design/tokens/colors.json). Colors have semantic roles; do not select a shade separately for each screen.

## Core colors

| Role | Hex | Use |
| --- | --- | --- |
| `surface` | `#D8E3D9` | Page canvas, safe areas and navigation surfaces |
| `surface-raised` | `#C4D5C8` | Secondary controls, selected neutral rows and disabled backgrounds |
| `surface-card` | `#E3EBE2` | Cards, sheets, dialogs and input surfaces |
| `surface-elevated` | `#D3E1D4` | Optional elevated sage panels |
| `surface-intermediate` | `#D2E0D4` | Optional sage transition surface |
| `text` | `#173D36` | Headings, body text, labels and normal icons |
| `text-muted` | `#465B4D` | Secondary copy, hints, placeholders and inactive navigation labels |
| `primary` | `#24634F` | Main actions, links, active selections and checked controls |
| `primary-strong` | `#174B3D` | Hover/pressed actions and the dark end of primary gradients |
| `on-accent` | `#F4F2E7` | Text/icons on filled primary, destructive and deep teal surfaces |
| `hero` | `#073C3B` | Editorial artwork backdrop, launch canvas, expanded images and footer |
| `border` | `#A9C2B2` | Subtle separators and decorative card/input borders |
| `border-strong` | `#6F9380` | Stronger decorative outlines; focus uses `primary-strong` |
| `error` | `#9C2630` | Error text, unread indicators and destructive actions |
| `error-surface` | `#F0DDD7` | Error banners and lightly styled destructive controls |
| `warning` | `#755019` | Warning text/icons |
| `warning-surface` | `#EADFC5` | Warning banners and caution statuses |
| `success` | `#24634F` | Completion/success text and status icons |
| `gold` | `#75521D` | Restrained gold text/icons on sage |
| `berry` | `#88596E` | Decorative accents; not an approved normal body-text color |

Warning and gold were darkened slightly during the cross-platform audit to meet normal-text contrast on raised sage.

## Season and functional colors

| Role | Hex | Use |
| --- | --- | --- |
| `advent` | `#7858B9` | Advent calendar marker and matching legend dot |
| `christmas` | `#DCB969` | Christmas marker; decorative bright gold on dark artwork |
| `lent` | `#9B5087` | Lent marker |
| `easter` | `#F5F5FA` | Easter marker, with a subtle outline where needed |
| `ordinary` | `#3C9B5F` | Ordinary Time marker |
| `today` | `#F2CF63` | Optional decorative today indicator; never the only selected-state cue |
| `qr-canvas` | `#FFFFFF` | QR-code canvas only |

Markers and legends must use the same season mapping. Season hues are informational markers, not replacements for accessible body-text roles. Selected dates use filled `primary` with `on-accent`; an outline can mark today when it is not selected.

## Component and state rules

- Standard screens use `surface`, `surface-card` and `text`. Light content surfaces remain sage. Cream text belongs on dark or filled accent surfaces.
- Primary actions pair `primary` / `primary-strong` with `on-accent`. Secondary actions pair `surface-raised` with `text`. Destructive actions use either `error` + `on-accent`, or `error-surface` + `error`.
- Inputs use a sage surface, `text` for input/labels, `text-muted` for placeholders and a clearly visible focus treatment. Do not reduce label opacity to create a muted color.
- Error, empty, loading, retry, disabled, selected and completed states follow the same roles as the rest of the screen. Never reuse bright colors from the old navy theme for text on sage.
- Disabled controls use raised sage and muted text. Do not rely on opacity alone to indicate availability.
- Focus rings use `primary-strong` or `primary` with an offset. Subtle borders are decorative and must not be the sole focus or state indicator.
- Dialogs/sheets use `surface-card` and `text`; their scrim derives from `hero`. Shadows derive from `text` at low opacity. Do not reintroduce blue glows or heavy black shadows.
- Artwork, photography and external store marks keep their intrinsic colors. Captions over imagery need an adequate dark overlay and `on-accent` text. The same cream/dark pairing applies to the footer.
- The embedded readings viewer generates its host-scoped colors from this contract. Other external websites and system-owned surfaces may retain their own branding.
- The current contract is light-only. Device dark mode must not invert these surfaces. A future dark appearance needs a separate, reviewed role set.

## Accessibility

Normal text requires at least 4.5:1 contrast; large text requires 3:1. The checker currently verifies 24 approved text/background pairs, including the three main sage surfaces and filled actions. Examples:

| Pair | Contrast |
| --- | --- |
| Body text on page sage | 9.05:1 |
| Secondary text on page sage | 5.55:1 |
| Secondary text on raised sage | 4.78:1 |
| Gold text on raised sage | 4.60:1 |

Do not use decorative season colors as body text, and do not communicate meaning through color alone. Keep labels, icons, selected-state semantics and keyboard focus visible.

## Implementation and future updates

1. Change semantic values in `design/tokens/colors.json`.
2. Run `python3 scripts/sync-color-contract.py`.
3. Use existing platform roles in new UI: CSS `var(--sanctuary-text)`, SwiftUI `AppTheme.cardText` / `AppTheme.homeInk`, and Compose `SanctuaryText` / `SanctuaryHomeInk`, for example. Legacy aliases resolve to these same values.
4. Run `python3 scripts/verify-color-contract.py` and `python3 scripts/verify-mobile-theme.py`.
5. Build web, iOS and Android. Review light/filled controls, focus, validation, dialogs, loading/empty states, and calendar selection at desktop and phone sizes.

The synchronizer updates the web/admin CSS token file, native palette declarations, Android XML color resources, both embedded readings scripts, and iOS launch color surfaces. It supports `--check` for CI without changing files. The verifier rejects raw interface colors in web/admin styles and undefined CSS roles, checks native adapter drift and reader parity, and validates contrast.

Do not edit generated CSS, Android `colors.xml`, or embedded readings scripts directly. Edit the contract or the readings template, then regenerate. Native palette declarations are synchronized in their existing theme files so callers retain stable names. New roles require explicit native adapter mappings in the synchronizer.
