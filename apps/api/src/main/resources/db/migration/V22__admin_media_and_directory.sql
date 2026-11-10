CREATE TABLE admin_content_images (
    id UUID PRIMARY KEY,
    content_type TEXT NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png')),
    data BYTEA NOT NULL CHECK (octet_length(data) <= 5242880),
    width INTEGER NOT NULL CHECK (width BETWEEN 1 AND 4096),
    height INTEGER NOT NULL CHECK (height BETWEEN 1 AND 4096),
    created_by TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- Provider registrations are cached separately: syncing never creates an app profile/activity.
CREATE TABLE admin_user_directory (
    cognito_sub TEXT PRIMARY KEY,
    email TEXT,
    display_name TEXT,
    email_verified BOOLEAN NOT NULL,
    status TEXT NOT NULL,
    enabled BOOLEAN NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL,
    provider_updated_at TIMESTAMPTZ,
    sync_id UUID NOT NULL
);
CREATE TABLE admin_directory_state (
    singleton BOOLEAN PRIMARY KEY CHECK (singleton),
    last_synced_at TIMESTAMPTZ
);
INSERT INTO admin_directory_state(singleton) VALUES (true);
CREATE FUNCTION remove_admin_directory_profile() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    DELETE FROM admin_user_directory WHERE cognito_sub = OLD.cognito_sub;
    RETURN OLD;
END $$;
CREATE TRIGGER remove_admin_directory_profile AFTER DELETE ON users FOR EACH ROW EXECUTE FUNCTION remove_admin_directory_profile();

-- Shared patronages can also be attached from a novena editor without free-text duplicates.
CREATE TABLE novena_patronage_links (
    novena_id TEXT NOT NULL REFERENCES novenas(id) ON DELETE CASCADE,
    patronage_id BIGINT NOT NULL REFERENCES content_patronages(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (novena_id, patronage_id)
);
CREATE INDEX novena_patronage_links_patronage_idx ON novena_patronage_links(patronage_id);
