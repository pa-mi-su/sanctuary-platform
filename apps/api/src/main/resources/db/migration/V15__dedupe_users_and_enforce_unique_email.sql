-- Normal Cognito signup uses email as username, but the Sanctuary app database
-- must also prevent duplicate active account rows for the same email.

CREATE TEMP TABLE duplicate_user_merge_map AS
WITH ranked AS (
    SELECT
        id,
        FIRST_VALUE(id) OVER (
            PARTITION BY LOWER(email)
            ORDER BY last_sign_in_at DESC, updated_at DESC, created_at DESC, id
        ) AS canonical_id
    FROM users
    WHERE email IS NOT NULL
      AND BTRIM(email) <> ''
)
SELECT id AS duplicate_id, canonical_id
FROM ranked
WHERE id <> canonical_id;

INSERT INTO user_preferences (
    user_id,
    time_zone_id,
    novena_reminders_enabled,
    feast_reminders_enabled,
    email_updates_enabled,
    onboarding_completed,
    created_at,
    updated_at
)
SELECT
    merge_map.canonical_id,
    user_preferences.time_zone_id,
    user_preferences.novena_reminders_enabled,
    user_preferences.feast_reminders_enabled,
    user_preferences.email_updates_enabled,
    user_preferences.onboarding_completed,
    user_preferences.created_at,
    user_preferences.updated_at
FROM user_preferences
JOIN duplicate_user_merge_map merge_map ON merge_map.duplicate_id = user_preferences.user_id
ON CONFLICT (user_id) DO UPDATE SET
    novena_reminders_enabled = user_preferences.novena_reminders_enabled OR EXCLUDED.novena_reminders_enabled,
    feast_reminders_enabled = user_preferences.feast_reminders_enabled OR EXCLUDED.feast_reminders_enabled,
    email_updates_enabled = user_preferences.email_updates_enabled OR EXCLUDED.email_updates_enabled,
    onboarding_completed = user_preferences.onboarding_completed OR EXCLUDED.onboarding_completed,
    updated_at = GREATEST(user_preferences.updated_at, EXCLUDED.updated_at);

DELETE FROM user_preferences
USING duplicate_user_merge_map merge_map
WHERE user_preferences.user_id = merge_map.duplicate_id;

INSERT INTO user_favorites (user_id, item_type, item_id, created_at)
SELECT merge_map.canonical_id, item_type, item_id, created_at
FROM user_favorites
JOIN duplicate_user_merge_map merge_map ON merge_map.duplicate_id = user_favorites.user_id
ON CONFLICT (user_id, item_type, item_id) DO NOTHING;

DELETE FROM user_favorites
USING duplicate_user_merge_map merge_map
WHERE user_favorites.user_id = merge_map.duplicate_id;

INSERT INTO user_novena_commitments (
    user_id,
    novena_id,
    started_at,
    current_day,
    completed_days,
    reminder_enabled,
    reminder_morning_hour,
    reminder_evening_hour,
    reminder_time_zone_id,
    status,
    updated_at
)
SELECT
    merge_map.canonical_id,
    novena_id,
    started_at,
    current_day,
    completed_days,
    reminder_enabled,
    reminder_morning_hour,
    reminder_evening_hour,
    reminder_time_zone_id,
    status,
    updated_at
FROM user_novena_commitments
JOIN duplicate_user_merge_map merge_map ON merge_map.duplicate_id = user_novena_commitments.user_id
ON CONFLICT (user_id, novena_id) DO UPDATE SET
    started_at = LEAST(user_novena_commitments.started_at, EXCLUDED.started_at),
    current_day = GREATEST(user_novena_commitments.current_day, EXCLUDED.current_day),
    completed_days = ARRAY(
        SELECT DISTINCT completed_day
        FROM UNNEST(user_novena_commitments.completed_days || EXCLUDED.completed_days) AS completed_day
        ORDER BY completed_day
    ),
    reminder_enabled = user_novena_commitments.reminder_enabled OR EXCLUDED.reminder_enabled,
    reminder_morning_hour = COALESCE(user_novena_commitments.reminder_morning_hour, EXCLUDED.reminder_morning_hour),
    reminder_evening_hour = COALESCE(user_novena_commitments.reminder_evening_hour, EXCLUDED.reminder_evening_hour),
    reminder_time_zone_id = COALESCE(user_novena_commitments.reminder_time_zone_id, EXCLUDED.reminder_time_zone_id),
    status = CASE
        WHEN user_novena_commitments.status = 'active' OR EXCLUDED.status = 'active' THEN 'active'
        WHEN user_novena_commitments.status = 'paused' OR EXCLUDED.status = 'paused' THEN 'paused'
        ELSE 'completed'
    END,
    updated_at = GREATEST(user_novena_commitments.updated_at, EXCLUDED.updated_at);

DELETE FROM user_novena_commitments
USING duplicate_user_merge_map merge_map
WHERE user_novena_commitments.user_id = merge_map.duplicate_id;

UPDATE user_activity_events
SET user_id = merge_map.canonical_id
FROM duplicate_user_merge_map merge_map
WHERE user_activity_events.user_id = merge_map.duplicate_id;

DELETE FROM users
USING duplicate_user_merge_map merge_map
WHERE users.id = merge_map.duplicate_id;

DROP INDEX IF EXISTS idx_users_email;

CREATE UNIQUE INDEX idx_users_email
    ON users (LOWER(email))
    WHERE email IS NOT NULL AND BTRIM(email) <> '';
