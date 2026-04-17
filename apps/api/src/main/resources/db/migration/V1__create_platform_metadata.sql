CREATE TABLE IF NOT EXISTS platform_metadata (
    id BIGSERIAL PRIMARY KEY,
    metadata_key TEXT NOT NULL UNIQUE,
    metadata_value TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
