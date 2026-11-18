CREATE TABLE announcements (
 id UUID PRIMARY KEY, title TEXT NOT NULL, body TEXT NOT NULL, destination TEXT NOT NULL,
 published_at TIMESTAMPTZ NOT NULL DEFAULT now(), withdrawn_at TIMESTAMPTZ
);
CREATE INDEX announcements_published ON announcements(published_at DESC,id) WHERE withdrawn_at IS NULL;
CREATE TABLE announcement_receipts (
 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 announcement_id UUID NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
 read_at TIMESTAMPTZ, cleared_at TIMESTAMPTZ,
 PRIMARY KEY(user_id,announcement_id)
);
CREATE TABLE announcement_inbox_preferences (
 user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
 cleared_before TIMESTAMPTZ
);
