CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cognito_sub TEXT NOT NULL UNIQUE,
    email TEXT,
    display_name TEXT,
    preferred_language TEXT NOT NULL DEFAULT 'en' CHECK (preferred_language IN ('en', 'es', 'pl')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (LOWER(email));
CREATE INDEX idx_users_updated_at ON users (updated_at DESC);

INSERT INTO users (cognito_sub)
SELECT DISTINCT legacy_user_id
FROM (
    SELECT user_id AS legacy_user_id FROM user_favorites
    UNION
    SELECT user_id AS legacy_user_id FROM user_novena_commitments
) legacy_users
WHERE legacy_user_id IS NOT NULL AND legacy_user_id <> ''
ON CONFLICT (cognito_sub) DO NOTHING;

ALTER TABLE user_favorites ADD COLUMN account_user_id UUID;

UPDATE user_favorites favorite
SET account_user_id = app_user.id
FROM users app_user
WHERE app_user.cognito_sub = favorite.user_id;

ALTER TABLE user_favorites ALTER COLUMN account_user_id SET NOT NULL;
ALTER TABLE user_favorites DROP CONSTRAINT user_favorites_pkey;
DROP INDEX IF EXISTS idx_user_favorites_user_id;
DROP INDEX IF EXISTS idx_user_favorites_item;
ALTER TABLE user_favorites DROP COLUMN user_id;
ALTER TABLE user_favorites RENAME COLUMN account_user_id TO user_id;
ALTER TABLE user_favorites ADD CONSTRAINT user_favorites_pkey PRIMARY KEY (user_id, item_type, item_id);
ALTER TABLE user_favorites
    ADD CONSTRAINT fk_user_favorites_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_user_favorites_user_id ON user_favorites (user_id, created_at DESC);
CREATE INDEX idx_user_favorites_item ON user_favorites (item_type, item_id);

ALTER TABLE user_novena_commitments ADD COLUMN account_user_id UUID;

UPDATE user_novena_commitments commitment
SET account_user_id = app_user.id
FROM users app_user
WHERE app_user.cognito_sub = commitment.user_id;

ALTER TABLE user_novena_commitments ALTER COLUMN account_user_id SET NOT NULL;
ALTER TABLE user_novena_commitments DROP CONSTRAINT user_novena_commitments_pkey;
DROP INDEX IF EXISTS idx_user_novena_commitments_user_id;
DROP INDEX IF EXISTS idx_user_novena_commitments_status;
ALTER TABLE user_novena_commitments DROP COLUMN user_id;
ALTER TABLE user_novena_commitments RENAME COLUMN account_user_id TO user_id;
ALTER TABLE user_novena_commitments ADD CONSTRAINT user_novena_commitments_pkey PRIMARY KEY (user_id, novena_id);
ALTER TABLE user_novena_commitments
    ADD CONSTRAINT fk_user_novena_commitments_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

CREATE INDEX idx_user_novena_commitments_user_id ON user_novena_commitments (user_id, updated_at DESC);
CREATE INDEX idx_user_novena_commitments_status ON user_novena_commitments (user_id, status, updated_at DESC);
