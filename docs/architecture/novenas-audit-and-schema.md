# Novenas Audit And Schema

## Goal

Define the PostgreSQL model for novenas based on what the current iOS app actually uses, not just what exists in the legacy JSON files.

## Sources Reviewed

- `/Users/pms/repos/Sanctuary/Sanctuary/Resources/novenas.json`
- `/Users/pms/repos/Sanctuary/Sanctuary/Resources/LegacyData/novenas/*.json`
- `/Users/pms/repos/Sanctuary/Sanctuary/Resources/LegacyData/novenas_index.json`
- `/Users/pms/repos/Sanctuary/Sanctuary/Core/Domain/Entities.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Core/Data/Local/LocalContentRepository.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Core/Data/Local/ContentStore.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Features/Novenas/NovenasListViewModel.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Features/Novenas/NovenaDetailView.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Features/Search/SearchViews.swift`

## What The App Actually Uses

### Normalized novena content

The current app uses normalized novena content for:

- novena list title
- novena list summary
- novena list search
- novena tags
- novena image
- novena duration
- novena detail rendering
- per-day title
- per-day scripture
- per-day prayer
- per-day reflection
- per-day `bodyByLocale` fallback

This comes from the normalized novena shape already represented in:

- `Novena`
- `NovenaDay`
- `LocalContentRepository.mapSourceNovena`

### Intentions search

The app also uses raw `NovenaDocument` data for novena intentions search.

`SearchViews` loads `ContentStore.novena(id:)` and reads:

- `intentions`
- `intentions_es`
- `intentions_pl`

This means novena intentions are part of the real product surface and need to be migrated even though they are not part of the normalized `Novena` model.

### Serving window logic

The app uses `novenas_index.json` for:

- novena start date
- novena feast date
- novena end date
- calendar/day lookup for novenas

This behavior is driven through:

- `ContentStore.novenaServingWindow`
- `ContentStore.novenaFeastDate`
- `ContentStore.firstNovenaIDForCalendarDay`

So `novenas_index.json` is not optional legacy noise. It contains real scheduling data used by the app.

## Audit Findings

- normalized novena count: `237`
- indexed novena count: `237`
- legacy novena docs on disk: `239`
- total normalized novena days: `2595`

Two legacy novena docs are not in the active indexed catalog:

- `cardinal_burke_our_lady_of_guadalupe`
- `one_year_st_bridget_of_sweden`

Important data shape findings:

- all `237` active normalized novenas have tags
- all `237` active normalized novenas have image URLs
- only `46` active legacy novena docs have non-empty English intentions
- no active legacy novena docs have non-empty Spanish intentions
- no active legacy novena docs have non-empty Polish intentions

That matches the current iOS fallback behavior, where non-English intention search falls back to English and applies lightweight translation when localized arrays are absent.

Observed novena lengths / day counts:

- `9`
- `20`
- `25`
- `30`
- `46`
- `54`
- `275`
- `365`

## Recommended PostgreSQL Tables

### novenas

Purpose:

- canonical novena summary record
- primary row used by list/detail APIs

Suggested columns:

- `id`
- `slug`
- `title_en`
- `title_es`
- `title_pl`
- `description_en`
- `description_es`
- `description_pl`
- `duration_days`
- `image_url`
- `created_at`
- `updated_at`

### novena_tags

Purpose:

- normalized tags used by list filtering and search indexing

Suggested columns:

- `novena_id`
- `tag`

### novena_days

Purpose:

- localized day-by-day novena content

Suggested columns:

- `id`
- `novena_id`
- `day_number`
- `title_en`
- `title_es`
- `title_pl`
- `scripture_en`
- `scripture_es`
- `scripture_pl`
- `prayer_en`
- `prayer_es`
- `prayer_pl`
- `reflection_en`
- `reflection_es`
- `reflection_pl`
- `body_en`
- `body_es`
- `body_pl`
- `created_at`
- `updated_at`

### novena_intentions

Purpose:

- searchable intention values tied to novenas
- preserves real app functionality for the intentions search screen

Suggested columns:

- `id`
- `novena_id`
- `locale`
- `intention_text`
- `sort_order`
- `created_at`

Notes:

- keep this intentionally simple
- this is a one-row-per-intention search table, not a separate editorial content model
- localized intention rows can be sparse because the current source data is sparse

### novena_serving_rules

Purpose:

- stores the app-used scheduling metadata from `novenas_index.json`

Suggested columns:

- `novena_id`
- `start_rule_type`
- `start_rule_month`
- `start_rule_day`
- `start_rule_anchor`
- `start_rule_offset_days`
- `start_rule_weekday`
- `start_rule_weekday_policy`
- `start_rule_n`
- `start_rule_days_before`
- `feast_rule_type`
- `feast_rule_month`
- `feast_rule_day`
- `feast_rule_anchor`
- `feast_rule_offset_days`
- `feast_rule_weekday`
- `feast_rule_weekday_policy`
- `feast_rule_n`
- `feast_rule_days_before`
- `entry_duration_days`
- `category`
- `notes`
- `patronage`
- `source`
- `created_at`
- `updated_at`

Notes:

- this table is not just for migration completeness
- it preserves the actual rule inputs the iOS app uses today
- keeping the raw rule structure gives the Java backend room to reproduce or evolve the serving-window logic cleanly

## Recommendation

For novenas, we should migrate:

1. normalized novena content
2. day-by-day content
3. tags
4. intentions used by search
5. serving-rule metadata used by calendar and date logic

We should not migrate the two non-indexed legacy novena documents as part of the active novena catalog unless we make an intentional product decision to expose them later.
