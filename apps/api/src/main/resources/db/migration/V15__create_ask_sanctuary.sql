CREATE TABLE ask_sanctuary_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    input_message TEXT,
    input_hash TEXT,
    reused_response BOOLEAN NOT NULL DEFAULT FALSE,
    detected_intent TEXT NOT NULL,
    guardrail_type TEXT NOT NULL,
    guardrail_triggered BOOLEAN NOT NULL DEFAULT FALSE,
    response_status TEXT NOT NULL,
    response_payload JSONB NOT NULL,
    classification_model TEXT,
    classification_input_tokens INTEGER,
    classification_output_tokens INTEGER,
    classification_total_tokens INTEGER,
    generation_model TEXT,
    generation_input_tokens INTEGER,
    generation_output_tokens INTEGER,
    generation_total_tokens INTEGER,
    old_testament_reference TEXT,
    new_testament_reference TEXT,
    saint TEXT,
    prayer TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ask_sanctuary_sessions_user_created
    ON ask_sanctuary_sessions (user_id, created_at DESC);

CREATE INDEX idx_ask_sanctuary_sessions_intent_created
    ON ask_sanctuary_sessions (detected_intent, created_at DESC);

CREATE INDEX idx_ask_sanctuary_sessions_user_hash_created
    ON ask_sanctuary_sessions (user_id, input_hash, created_at DESC)
    WHERE input_hash IS NOT NULL;

CREATE TABLE ask_sanctuary_user_entitlements (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    tier TEXT NOT NULL DEFAULT 'FREE',
    daily_limit_override INTEGER,
    unlimited BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ask_sanctuary_entitlement_tier
        CHECK (tier IN ('FREE', 'PLUS', 'ADMIN')),
    CONSTRAINT chk_ask_sanctuary_daily_limit_override
        CHECK (daily_limit_override IS NULL OR daily_limit_override >= 0)
);

CREATE TABLE ask_sanctuary_daily_usage (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, usage_date),
    CONSTRAINT chk_ask_sanctuary_daily_usage_count
        CHECK (request_count >= 0)
);

CREATE TABLE ask_sanctuary_ip_daily_usage (
    ip_hash TEXT NOT NULL,
    usage_date DATE NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (ip_hash, usage_date),
    CONSTRAINT chk_ask_sanctuary_ip_daily_usage_count
        CHECK (request_count >= 0)
);

CREATE TABLE ask_sanctuary_request_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ask_sanctuary_request_events_user_created
    ON ask_sanctuary_request_events (user_id, created_at DESC);

CREATE TABLE ask_sanctuary_misuse_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    guardrail_type TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ask_sanctuary_misuse_events_user_created
    ON ask_sanctuary_misuse_events (user_id, created_at DESC);

CREATE TABLE ask_sanctuary_account_locks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    locked_until TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ask_sanctuary_account_locks_user_until
    ON ask_sanctuary_account_locks (user_id, locked_until DESC);

CREATE TABLE user_feature_consents (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    feature TEXT NOT NULL,
    version TEXT NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, feature, version),
    CONSTRAINT chk_user_feature_consents_feature
        CHECK (BTRIM(feature) <> ''),
    CONSTRAINT chk_user_feature_consents_version
        CHECK (BTRIM(version) <> '')
);

CREATE INDEX idx_user_feature_consents_user_feature
    ON user_feature_consents (user_id, feature, accepted_at DESC);
