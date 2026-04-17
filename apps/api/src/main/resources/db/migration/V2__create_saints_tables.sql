CREATE TABLE saints (
    id TEXT PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    name_en TEXT NOT NULL,
    name_es TEXT NOT NULL,
    name_pl TEXT NOT NULL,
    feast_month INTEGER NOT NULL CHECK (feast_month BETWEEN 1 AND 12),
    feast_day INTEGER NOT NULL CHECK (feast_day BETWEEN 1 AND 31),
    image_url TEXT,
    feast_label_en TEXT NOT NULL,
    feast_label_es TEXT NOT NULL,
    feast_label_pl TEXT NOT NULL,
    summary_en TEXT NOT NULL,
    summary_es TEXT NOT NULL,
    summary_pl TEXT NOT NULL,
    biography_en TEXT NOT NULL,
    biography_es TEXT NOT NULL,
    biography_pl TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saints_slug ON saints (slug);
CREATE INDEX idx_saints_feast_day ON saints (feast_month, feast_day);

CREATE TABLE saint_tags (
    saint_id TEXT NOT NULL REFERENCES saints(id) ON DELETE CASCADE,
    tag TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (saint_id, tag)
);

CREATE INDEX idx_saint_tags_tag ON saint_tags (tag);

CREATE TABLE saint_patronages (
    saint_id TEXT NOT NULL REFERENCES saints(id) ON DELETE CASCADE,
    patronage TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (saint_id, patronage)
);

CREATE INDEX idx_saint_patronages_patronage ON saint_patronages (patronage);

CREATE TABLE saint_sources (
    id BIGSERIAL PRIMARY KEY,
    saint_id TEXT NOT NULL REFERENCES saints(id) ON DELETE CASCADE,
    source_text TEXT NOT NULL,
    source_url TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_saint_sources_saint_id ON saint_sources (saint_id, sort_order);
