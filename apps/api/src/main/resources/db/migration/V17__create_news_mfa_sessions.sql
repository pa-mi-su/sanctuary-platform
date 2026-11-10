-- Store only hashes of opaque browser session proofs and their bound Cognito tokens.
CREATE TABLE news_mfa_sessions (
    proof_hash TEXT PRIMARY KEY,
    token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX news_mfa_sessions_expiry ON news_mfa_sessions(expires_at);
