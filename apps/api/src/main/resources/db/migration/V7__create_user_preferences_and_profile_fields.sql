ALTER TABLE users
    ADD COLUMN avatar_url TEXT,
    ADD COLUMN last_sign_in_at TIMESTAMPTZ;

UPDATE users
SET last_sign_in_at = COALESCE(updated_at, created_at)
WHERE last_sign_in_at IS NULL;

ALTER TABLE users
    ALTER COLUMN last_sign_in_at SET NOT NULL;

CREATE INDEX idx_users_last_sign_in_at ON users (last_sign_in_at DESC);

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    time_zone_id TEXT NOT NULL DEFAULT 'UTC',
    novena_reminders_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    feast_reminders_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    email_updates_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO user_preferences (user_id)
SELECT id
FROM users
ON CONFLICT (user_id) DO NOTHING;

CREATE INDEX idx_user_preferences_updated_at ON user_preferences (updated_at DESC);
