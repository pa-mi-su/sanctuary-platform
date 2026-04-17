CREATE TABLE user_favorites (
    user_id TEXT NOT NULL,
    item_type TEXT NOT NULL CHECK (item_type IN ('saint', 'novena', 'prayer')),
    item_id TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, item_type, item_id)
);

CREATE INDEX idx_user_favorites_user_id ON user_favorites (user_id, created_at DESC);
CREATE INDEX idx_user_favorites_item ON user_favorites (item_type, item_id);

CREATE TABLE user_novena_commitments (
    user_id TEXT NOT NULL,
    novena_id TEXT NOT NULL REFERENCES novenas(id) ON DELETE CASCADE,
    started_at TIMESTAMPTZ NOT NULL,
    current_day INTEGER NOT NULL CHECK (current_day > 0),
    completed_days INTEGER[] NOT NULL DEFAULT '{}',
    reminder_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_morning_hour INTEGER,
    reminder_evening_hour INTEGER,
    reminder_time_zone_id TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('active', 'paused', 'completed')),
    updated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, novena_id)
);

CREATE INDEX idx_user_novena_commitments_user_id ON user_novena_commitments (user_id, updated_at DESC);
CREATE INDEX idx_user_novena_commitments_status ON user_novena_commitments (user_id, status, updated_at DESC);
