CREATE TABLE deleted_user_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cognito_sub TEXT UNIQUE,
    email_hash TEXT,
    deleted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (cognito_sub IS NOT NULL OR email_hash IS NOT NULL)
);

CREATE INDEX idx_deleted_user_accounts_deleted_at ON deleted_user_accounts (deleted_at DESC);
CREATE INDEX idx_deleted_user_accounts_email_hash ON deleted_user_accounts (email_hash);
