-- Reminder definitions only; no database configuration changes.
CREATE TABLE user_content_reminders (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    kind TEXT NOT NULL CHECK (kind IN ('prayers', 'novenas')),
    slug TEXT NOT NULL,
    title TEXT NOT NULL,
    hour INTEGER NOT NULL CHECK (hour BETWEEN 0 AND 23),
    minute INTEGER NOT NULL CHECK (minute BETWEEN 0 AND 59),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, kind, slug)
);
-- Hour/minute are wall-clock time on each device (including after travel/DST).
