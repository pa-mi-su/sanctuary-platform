CREATE TABLE user_activity_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_type TEXT NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id TEXT,
    activity_date DATE NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX idx_user_activity_events_user_date ON user_activity_events (user_id, activity_date DESC);
CREATE INDEX idx_user_activity_events_user_type ON user_activity_events (user_id, activity_type, occurred_at DESC);
