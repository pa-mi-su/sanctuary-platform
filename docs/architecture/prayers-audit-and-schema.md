# Prayers Audit And Schema

## Why This Exists

Before importing `prayers.json` into PostgreSQL, we audited the legacy iOS app to confirm which prayer fields are actually used by the product.

The goal is to avoid importing dead data while also avoiding accidental feature regression in the prayer detail experience.

## Source Files Reviewed

- `/Users/pms/repos/Sanctuary/Sanctuary/Resources/prayers.json`
- `/Users/pms/repos/Sanctuary/Sanctuary/Resources/LegacyData/prayers/*.json`
- `/Users/pms/repos/Sanctuary/Sanctuary/Core/Domain/Entities.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Core/Data/Local/LocalContentRepository.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Features/Prayers/PrayerViews.swift`

## What The App Uses

### Prayer list and search

The normalized `Prayer` domain model used by the repository and search flow includes:

- `id`
- `slug`
- `category`
- `titleByLocale`
- `bodyByLocale`
- `tags`

Observed usage in the iOS app:

- list/search filters by `category`
- search indexes `title`, `body`, `category`, `slug`, and `tags`
- prayer cards display localized title
- prayer cards display the first line of localized body text

### Prayer detail screen

The detail view still reaches into the legacy prayer documents for additional fields:

- `alternateTitle`
- `note`
- `photoUrl`
- `source.title`

This means a prayer import that only carries the normalized list/search fields would be incomplete relative to the current product behavior.

## Dataset Facts

Across the current prayer data set:

- normalized prayer count: `14`
- legacy prayer document count: `14`
- all normalized prayers have matching legacy detail documents
- all legacy prayer documents have matching normalized prayer records
- prayers with non-empty tags: `13`
- total tag rows: `52`
- prayers with `alternateTitle`: `14`
- prayers with `note`: `14`
- prayers with `photoUrl`: `14`
- prayers with `source.title`: `14`
- observed `source.type` values:
  - `user_provided`: `14`

## Recommended Database Shape

### prayers

Keep one canonical `prayers` table with both list/search fields and detail fields the app actively uses.

Suggested columns:

- `id`
- `slug`
- `category`
- `title_en`
- `title_es`
- `title_pl`
- `body_en`
- `body_es`
- `body_pl`
- `alternate_title_en`
- `alternate_title_es`
- `alternate_title_pl`
- `note_en`
- `note_es`
- `note_pl`
- `image_url`
- `source_title`
- `source_type`
- `created_at`
- `updated_at`

### prayer_tags

Suggested columns:

- `prayer_id`
- `tag`

## What We Should Not Import

For the current legacy prayer set, we should not create extra tables for fields the app does not use.

Examples:

- no standalone prayer-source table is needed yet because there is only one source object per prayer and the app only uses `source.title` and implicitly `source.type`
- no prayer-note child table is needed because the note is a single localized detail field on the prayer
- no alternate-title child table is needed because the alternate title is a single localized detail field on the prayer

## Recommendation

For prayers, import exactly the fields the current app uses:

- `id`
- `slug`
- `category`
- localized titles
- localized body
- `tags`
- localized alternate title
- localized note
- `image_url`
- `source_title`
- `source_type`

This keeps the migration focused, preserves the present prayer UI behavior, and avoids over-modeling a very small dataset.
