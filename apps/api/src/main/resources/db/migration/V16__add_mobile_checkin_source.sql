ALTER TABLE user_devices
    ADD COLUMN client_instance_id TEXT,
    ADD COLUMN automated_test BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN check_in_source TEXT NOT NULL DEFAULT 'app'
        CHECK (check_in_source IN ('app', 'automated_test', 'legacy'));

ALTER TABLE anonymous_app_devices
    ADD COLUMN client_instance_id TEXT,
    ADD COLUMN automated_test BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN check_in_source TEXT NOT NULL DEFAULT 'legacy'
        CHECK (check_in_source IN ('app', 'automated_test', 'legacy'));

ALTER TABLE user_app_activity_events
    ADD COLUMN client_instance_id TEXT,
    ADD COLUMN automated_test BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN check_in_source TEXT NOT NULL DEFAULT 'app'
        CHECK (check_in_source IN ('app', 'automated_test', 'legacy'));

ALTER TABLE anonymous_app_activity_events
    ADD COLUMN client_instance_id TEXT,
    ADD COLUMN automated_test BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN check_in_source TEXT NOT NULL DEFAULT 'legacy'
        CHECK (check_in_source IN ('app', 'automated_test', 'legacy'));

ALTER TABLE user_app_activity_events
    DROP CONSTRAINT IF EXISTS user_app_activity_events_event_type_check,
    ADD CONSTRAINT user_app_activity_events_event_type_check
        CHECK (event_type IN ('app_open', 'session_start', 'foreground_heartbeat'));

ALTER TABLE anonymous_app_activity_events
    DROP CONSTRAINT IF EXISTS anonymous_app_activity_events_event_type_check,
    ADD CONSTRAINT anonymous_app_activity_events_event_type_check
        CHECK (event_type IN (
            'app_open',
            'session_start',
            'foreground_heartbeat',
            'notification_permission_allowed',
            'notification_permission_denied',
            'screen_view'
        ));

CREATE INDEX idx_user_devices_live_source
    ON user_devices (check_in_source, automated_test, last_seen_at DESC);

CREATE INDEX idx_anonymous_app_devices_live_source
    ON anonymous_app_devices (check_in_source, automated_test, last_seen_at DESC);

CREATE INDEX idx_user_app_activity_events_live_heartbeat
    ON user_app_activity_events (client_instance_id, occurred_at DESC)
    WHERE event_type = 'foreground_heartbeat'
      AND automated_test = FALSE
      AND check_in_source = 'app';

CREATE INDEX idx_anonymous_app_activity_events_live_heartbeat
    ON anonymous_app_activity_events (client_instance_id, occurred_at DESC)
    WHERE event_type = 'foreground_heartbeat'
      AND automated_test = FALSE
      AND check_in_source = 'app';
