# Novenas Import Plan

## Goal

Define the one-time external import shape for novenas so we can load the legacy content into PostgreSQL and then audit the result with confidence.

## Import Scope

Import only the active novena catalog used by the current app.

Source of truth for the active catalog:

- `/Users/pms/repos/Sanctuary/Sanctuary/Resources/novenas.json`
- `/Users/pms/repos/Sanctuary/Sanctuary/Resources/LegacyData/novenas_index.json`
- `/Users/pms/repos/Sanctuary/Sanctuary/Resources/LegacyData/novenas/*.json`

Catalog size:

- `237` active indexed novenas

Do not import these two non-indexed legacy novena docs into the active catalog:

- `cardinal_burke_our_lady_of_guadalupe`
- `one_year_st_bridget_of_sweden`

## Tables To Populate

### novenas

One row per active novena.

Columns to populate:

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

Primary source:

- normalized `/Users/pms/repos/Sanctuary/Sanctuary/Resources/novenas.json`

### novena_tags

One row per novena tag.

Columns to populate:

- `novena_id`
- `tag`

Primary source:

- normalized `/Users/pms/repos/Sanctuary/Sanctuary/Resources/novenas.json`

### novena_days

One row per novena day.

Columns to populate:

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

Primary source:

- normalized `/Users/pms/repos/Sanctuary/Sanctuary/Resources/novenas.json`

### novena_intentions

One row per intention entry per locale.

Columns to populate:

- `novena_id`
- `locale`
- `intention_text`
- `sort_order`

Primary source:

- legacy novena docs in `/Users/pms/repos/Sanctuary/Sanctuary/Resources/LegacyData/novenas/*.json`

Import rules:

- import English intentions when present
- import Spanish intentions when present
- import Polish intentions when present
- do not synthesize translated DB rows during import
- preserve source order through `sort_order`

Reason:

- the current app already handles fallback behavior at the product layer
- the import should preserve source truth, not create artificial localized content

### novena_serving_rules

One row per active indexed novena.

Columns to populate:

- `novena_id`
- all `start_rule_*` fields
- all `feast_rule_*` fields
- `entry_duration_days`
- `category`
- `notes`
- `patronage`
- `source`

Primary source:

- `/Users/pms/repos/Sanctuary/Sanctuary/Resources/LegacyData/novenas_index.json`

## Import Join Strategy

Use `id` as the stable join key across:

- normalized novenas
- legacy novena docs
- novena index rules

Expected relationships:

- every imported `novenas` row must have a matching index entry
- every imported `novena_serving_rules` row must map to an active novena
- every imported `novena_days` row must map to an active novena
- `novena_intentions` may be sparse because only a subset of novenas have intentions

## Recommended External Import Sequence

1. load and validate the active novena id set from `novenas_index.json`
2. load normalized novenas from `novenas.json`
3. filter normalized novenas to the active indexed id set
4. load legacy novena docs for intention extraction
5. build import rows for:
   - `novenas`
   - `novena_tags`
   - `novena_days`
   - `novena_intentions`
   - `novena_serving_rules`
6. clear existing novena tables in dependency order
7. insert fresh rows in parent-first order

Recommended delete order:

- `novena_intentions`
- `novena_days`
- `novena_tags`
- `novena_serving_rules`
- `novenas`

Recommended insert order:

- `novenas`
- `novena_tags`
- `novena_days`
- `novena_intentions`
- `novena_serving_rules`

## Verification Checklist

After the import, verify:

### counts

- `novenas` = `237`
- `novena_serving_rules` = `237`
- `novena_days` = total active day count from normalized source
- `novena_tags` = total active tag count from normalized source
- `novena_intentions` = total intention row count from active legacy docs

### spot checks

Check a few novenas across:

- short novena (`9` days)
- long novena (`30` days)
- very long novena (`54`, `275`, or `365` days)
- novena with intentions
- novena without intentions

### rule checks

Verify `novena_serving_rules` contains:

- populated fixed-rule month/day values where expected
- raw metadata fields from `novenas_index.json`

### parity audit

Run a programmatic comparison against the legacy sources for:

- novena summary fields
- day rows
- tags
- intention rows
- serving-rule fields

## Success Criteria

The novena import is complete when:

- active novena count matches `237`
- child-table counts match source-derived counts
- sampled novena detail rows look correct
- serving-rule rows match `novenas_index.json`
- audit finds no missing rows, no extra rows, and no field mismatches
