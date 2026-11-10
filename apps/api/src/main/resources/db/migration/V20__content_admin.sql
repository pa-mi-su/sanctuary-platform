-- Admin identities are pinned by deployment configuration, never by public signup.
CREATE TABLE admin_challenges (
    token_hash TEXT PRIMARY KEY,
    encrypted_state TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE admin_sessions (
    token_hash TEXT PRIMARY KEY,
    owner_sub TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE admin_login_attempts (
    bucket TEXT PRIMARY KEY,
    attempts INTEGER NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE admin_content_audit (
    id BIGSERIAL PRIMARY KEY,
    actor_sub TEXT NOT NULL,
    entity TEXT NOT NULL,
    record_id TEXT NOT NULL,
    reason TEXT NOT NULL,
    before_value JSONB,
    after_value JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX admin_content_audit_recent ON admin_content_audit(created_at DESC);
