CREATE TABLE admin_users (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_users_enabled
    ON admin_users (enabled, created_at DESC);

CREATE TABLE user_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fcm_token TEXT NOT NULL UNIQUE,
    platform TEXT NOT NULL CHECK (platform IN ('ios', 'android')),
    app_version TEXT,
    language TEXT NOT NULL DEFAULT 'en' CHECK (language IN ('en', 'es', 'pl')),
    notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    token_status TEXT NOT NULL DEFAULT 'valid' CHECK (token_status IN ('valid', 'invalid')),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_devices_user_id
    ON user_devices (user_id, updated_at DESC);

CREATE INDEX idx_user_devices_platform
    ON user_devices (platform, token_status, updated_at DESC);

CREATE INDEX idx_user_devices_language
    ON user_devices (language, token_status, updated_at DESC);

CREATE INDEX idx_user_devices_last_seen
    ON user_devices (last_seen_at DESC);

CREATE TABLE admin_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    audience_type TEXT NOT NULL DEFAULT 'all' CHECK (audience_type IN ('all')),
    status TEXT NOT NULL DEFAULT 'draft' CHECK (status IN ('draft', 'queued', 'sending', 'sent', 'failed', 'canceled')),
    target_count INTEGER NOT NULL DEFAULT 0 CHECK (target_count >= 0),
    sent_count INTEGER NOT NULL DEFAULT 0 CHECK (sent_count >= 0),
    failed_count INTEGER NOT NULL DEFAULT 0 CHECK (failed_count >= 0),
    created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    sent_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_notifications_status
    ON admin_notifications (status, created_at DESC);

CREATE INDEX idx_admin_notifications_created_at
    ON admin_notifications (created_at DESC);

CREATE TABLE admin_notification_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES admin_notifications(id) ON DELETE CASCADE,
    user_device_id UUID REFERENCES user_devices(id) ON DELETE SET NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    platform TEXT NOT NULL CHECK (platform IN ('ios', 'android')),
    status TEXT NOT NULL CHECK (status IN ('targeted', 'sent', 'failed')),
    failure_reason TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_notification_deliveries_notification
    ON admin_notification_deliveries (notification_id, status);

CREATE INDEX idx_admin_notification_deliveries_user
    ON admin_notification_deliveries (user_id, created_at DESC);

CREATE TABLE admin_audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action TEXT NOT NULL,
    target_type TEXT,
    target_id TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_audit_events_actor
    ON admin_audit_events (actor_user_id, created_at DESC);

CREATE INDEX idx_admin_audit_events_created_at
    ON admin_audit_events (created_at DESC);
