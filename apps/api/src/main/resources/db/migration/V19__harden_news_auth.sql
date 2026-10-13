-- Existing proofs cannot be safely attributed to a subject. Require sign-in again.
DELETE FROM news_mfa_sessions;
ALTER TABLE news_mfa_sessions ADD COLUMN subject TEXT NOT NULL;
CREATE INDEX news_mfa_sessions_subject ON news_mfa_sessions(subject);

-- Ciphertext only: challenge payloads may contain short-lived Cognito tokens.
CREATE TABLE news_auth_challenges (
    id UUID PRIMARY KEY,
    payload BYTEA NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX news_auth_challenges_expiry ON news_auth_challenges(expires_at);

-- Shared across API replicas. No plaintext emails or passwords.
CREATE TABLE news_auth_attempts (
    key_hash TEXT PRIMARY KEY,
    attempts INTEGER NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX news_auth_attempts_expiry ON news_auth_attempts(expires_at);
