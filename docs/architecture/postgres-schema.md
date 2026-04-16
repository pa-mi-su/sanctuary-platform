# Sanctuary PostgreSQL Schema

## Goal

Use PostgreSQL as the primary system of record for:

- public content
- authenticated user state
- future editorial/admin workflows
- future search indexing

This schema is the starting point for the Java backend.

## Design Principles

- use explicit relational tables
- prefer stable ids and slugs
- keep content localized in structured fields
- separate public content from user state
- support gradual migration from the legacy iOS app

## Core Content Tables

### saints

Purpose:

- canonical saint and feast entries

Suggested columns:

- `id`
- `slug`
- `name`
- `name_en`
- `name_es`
- `name_pl`
- `feast_month`
- `feast_day`
- `feast_label_en`
- `feast_label_es`
- `feast_label_pl`
- `summary_en`
- `summary_es`
- `summary_pl`
- `biography_en`
- `biography_es`
- `biography_pl`
- `image_url`
- `created_at`
- `updated_at`

### saint_tags

Purpose:

- normalized saint tags

Suggested columns:

- `saint_id`
- `tag`

### saint_patronages

Purpose:

- normalized saint patronage values

Suggested columns:

- `saint_id`
- `patronage`

### saint_sources

Purpose:

- source attribution and provenance

Suggested columns:

- `saint_id`
- `source_text`
- `source_url`
- `sort_order`

### saint_prayers

Purpose:

- saint-linked prayers if present later

Suggested columns:

- `id`
- `saint_id`
- `title`
- `body_en`
- `body_es`
- `body_pl`
- `sort_order`

### novenas

Purpose:

- novena summary records

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

- normalized novena tags

Suggested columns:

- `novena_id`
- `tag`

### novena_days

Purpose:

- day-by-day novena content

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

- searchable intention records and future curated intention library

Suggested columns:

- `id`
- `novena_id`
- `slug`
- `title_en`
- `title_es`
- `title_pl`
- `summary_en`
- `summary_es`
- `summary_pl`
- `body_en`
- `body_es`
- `body_pl`
- `created_at`
- `updated_at`

### prayers

Purpose:

- standalone prayer library

Suggested columns:

- `id`
- `slug`
- `title_en`
- `title_es`
- `title_pl`
- `summary_en`
- `summary_es`
- `summary_pl`
- `body_en`
- `body_es`
- `body_pl`
- `image_url`
- `created_at`
- `updated_at`

### prayer_tags

Purpose:

- normalized prayer tags

Suggested columns:

- `prayer_id`
- `tag`

### liturgical_days

Purpose:

- canonical liturgical calendar data by date

Suggested columns:

- `id`
- `calendar_date`
- `season`
- `rank`
- `title_en`
- `title_es`
- `title_pl`
- `summary_en`
- `summary_es`
- `summary_pl`
- `metadata_json`
- `created_at`
- `updated_at`

### feast_day_saints

Purpose:

- map feast dates to saints

Suggested columns:

- `calendar_month`
- `calendar_day`
- `saint_id`

## User and Auth-Linked Tables

### users

Purpose:

- internal application user record linked to auth identity

Suggested columns:

- `id`
- `cognito_sub`
- `email`
- `display_name`
- `created_at`
- `updated_at`

### favorite_saints

Suggested columns:

- `user_id`
- `saint_id`
- `created_at`

### favorite_novenas

Suggested columns:

- `user_id`
- `novena_id`
- `created_at`

### novena_progress

Purpose:

- one row per user and novena

Suggested columns:

- `id`
- `user_id`
- `novena_id`
- `started_at`
- `completed_at`
- `last_completed_day`
- `status`
- `created_at`
- `updated_at`

### novena_day_progress

Purpose:

- per-user completed day tracking

Suggested columns:

- `novena_progress_id`
- `novena_day_id`
- `completed_at`

### user_preferences

Suggested columns:

- `user_id`
- `preferred_language`
- `notifications_enabled`
- `timezone`
- `created_at`
- `updated_at`

## Search Direction

PostgreSQL remains the source of truth.

We should still assume a future search layer for:

- saints search
- novena search
- novena intention search
- full-text discovery

That search layer can be added later without changing PostgreSQL as the canonical database.

## Initial Import Order

1. saints
2. novenas
3. novena_days
4. novena_intentions
5. prayers
6. liturgical_days
7. feast_day_saints
8. users and user-state tables

## Initial API Priorities

1. public content reads
2. saint and novena detail reads
3. calendar reads
4. auth integration
5. favorites and progress sync
