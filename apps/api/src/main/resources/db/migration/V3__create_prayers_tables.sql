CREATE TABLE prayers (
    id TEXT PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    category TEXT NOT NULL,
    title_en TEXT NOT NULL,
    title_es TEXT NOT NULL,
    title_pl TEXT NOT NULL,
    body_en TEXT NOT NULL,
    body_es TEXT NOT NULL,
    body_pl TEXT NOT NULL,
    alternate_title_en TEXT NOT NULL,
    alternate_title_es TEXT NOT NULL,
    alternate_title_pl TEXT NOT NULL,
    note_en TEXT NOT NULL,
    note_es TEXT NOT NULL,
    note_pl TEXT NOT NULL,
    image_url TEXT,
    source_title TEXT NOT NULL,
    source_type TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prayers_slug ON prayers (slug);
CREATE INDEX idx_prayers_category ON prayers (category);

CREATE TABLE prayer_tags (
    prayer_id TEXT NOT NULL REFERENCES prayers(id) ON DELETE CASCADE,
    tag TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (prayer_id, tag)
);

CREATE INDEX idx_prayer_tags_tag ON prayer_tags (tag);
