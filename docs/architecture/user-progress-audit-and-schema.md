# User Progress Audit And Schema

## Goal

Define the PostgreSQL model for user progress based on what the current iOS app actually stores and reads today.

## Sources Reviewed

- `/Users/pms/repos/Sanctuary/Sanctuary/Core/Domain/Entities.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Core/Domain/RepositoryProtocols.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Core/Data/Local/LocalUserProgressRepository.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Core/Application/UserProgressStore.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Core/Application/NovenaReminderScheduler.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Features/Me/MeView.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Features/Novenas/NovenaDetailView.swift`
- `/Users/pms/repos/Sanctuary/Sanctuary/Features/Saints/SaintDetailView.swift`

## What The App Actually Uses

### favorites

The app stores user favorites for:

- saints
- novenas
- prayers

Current domain model:

- `userID`
- `itemType`
- `itemID`
- `createdAt`

Used by:

- `UserProgressStore`
- `MeView`
- saint detail favorite toggle
- novena detail favorite toggle

### novena commitments

The app stores per-user novena progress for:

- active novenas
- completed novenas
- paused novenas
- completed day list
- reminder settings

Current domain model:

- `userID`
- `novenaID`
- `startedAt`
- `currentDay`
- `completedDays`
- `reminder.enabled`
- `reminder.morningHour`
- `reminder.eveningHour`
- `reminder.timeZoneID`
- `status`
- `updatedAt`

Used by:

- `UserProgressStore`
- `NovenaDetailView`
- `MeView`

### reminder behavior

The current app schedules digest reminders through `NovenaReminderScheduler` using the count of active commitments.

Important nuance:

- the scheduler itself is not persisted
- but the domain model still stores `ReminderConfig` inside each `UserNovenaCommitment`

So the persisted reminder fields should still exist in PostgreSQL even though current notification scheduling is digest-based.

## Current Local-Only State

Today this data is stored only in:

- `LocalUserProgressRepository`
- `UserDefaults`

That means user progress is still outside PostgreSQL and is one of the main remaining platform-data gaps.

## Recommended PostgreSQL Tables

### user_favorites

Purpose:

- stores favorited content items by user

Suggested columns:

- `user_id`
- `item_type`
- `item_id`
- `created_at`

Constraints:

- unique favorite per `(user_id, item_type, item_id)`

Notes:

- `item_type` should match the current enum values:
  - `saint`
  - `novena`
  - `prayer`

### user_novena_commitments

Purpose:

- stores per-user novena progress and reminder configuration

Suggested columns:

- `user_id`
- `novena_id`
- `started_at`
- `current_day`
- `completed_days`
- `reminder_enabled`
- `reminder_morning_hour`
- `reminder_evening_hour`
- `reminder_time_zone_id`
- `status`
- `updated_at`
- `created_at`

Constraints:

- one current record per `(user_id, novena_id)`

Notes:

- `completed_days` should be stored as an integer array because that matches the current domain shape cleanly
- `status` should match the current enum values:
  - `active`
  - `paused`
  - `completed`

## Recommendation

The next PostgreSQL migration should add:

1. `user_favorites`
2. `user_novena_commitments`

That is the minimum honest platform model needed to say we moved real user progress into PostgreSQL.

We do not need a separate reminders table yet because the current app stores reminder configuration directly inside the novena commitment record.
