CREATE TABLE novenas (
    id TEXT PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    title_en TEXT NOT NULL,
    title_es TEXT NOT NULL,
    title_pl TEXT NOT NULL,
    description_en TEXT NOT NULL,
    description_es TEXT NOT NULL,
    description_pl TEXT NOT NULL,
    duration_days INTEGER NOT NULL CHECK (duration_days > 0),
    image_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_novenas_slug ON novenas (slug);

CREATE TABLE novena_tags (
    novena_id TEXT NOT NULL REFERENCES novenas(id) ON DELETE CASCADE,
    tag TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (novena_id, tag)
);

CREATE INDEX idx_novena_tags_tag ON novena_tags (tag);

CREATE TABLE novena_days (
    id BIGSERIAL PRIMARY KEY,
    novena_id TEXT NOT NULL REFERENCES novenas(id) ON DELETE CASCADE,
    day_number INTEGER NOT NULL CHECK (day_number > 0),
    title_en TEXT NOT NULL,
    title_es TEXT NOT NULL,
    title_pl TEXT NOT NULL,
    scripture_en TEXT NOT NULL,
    scripture_es TEXT NOT NULL,
    scripture_pl TEXT NOT NULL,
    prayer_en TEXT NOT NULL,
    prayer_es TEXT NOT NULL,
    prayer_pl TEXT NOT NULL,
    reflection_en TEXT NOT NULL,
    reflection_es TEXT NOT NULL,
    reflection_pl TEXT NOT NULL,
    body_en TEXT NOT NULL,
    body_es TEXT NOT NULL,
    body_pl TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (novena_id, day_number)
);

CREATE INDEX idx_novena_days_novena_id ON novena_days (novena_id, day_number);

CREATE TABLE novena_intentions (
    id BIGSERIAL PRIMARY KEY,
    novena_id TEXT NOT NULL REFERENCES novenas(id) ON DELETE CASCADE,
    locale TEXT NOT NULL CHECK (locale IN ('en', 'es', 'pl')),
    intention_text TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (novena_id, locale, sort_order)
);

CREATE INDEX idx_novena_intentions_novena_id ON novena_intentions (novena_id, locale, sort_order);
CREATE INDEX idx_novena_intentions_locale ON novena_intentions (locale);

CREATE TABLE novena_serving_rules (
    novena_id TEXT PRIMARY KEY REFERENCES novenas(id) ON DELETE CASCADE,
    start_rule_type TEXT,
    start_rule_month INTEGER,
    start_rule_day INTEGER,
    start_rule_anchor TEXT,
    start_rule_offset_days INTEGER,
    start_rule_weekday INTEGER,
    start_rule_weekday_policy TEXT,
    start_rule_n INTEGER,
    start_rule_days_before INTEGER,
    feast_rule_type TEXT,
    feast_rule_month INTEGER,
    feast_rule_day INTEGER,
    feast_rule_anchor TEXT,
    feast_rule_offset_days INTEGER,
    feast_rule_weekday INTEGER,
    feast_rule_weekday_policy TEXT,
    feast_rule_n INTEGER,
    feast_rule_days_before INTEGER,
    entry_duration_days INTEGER,
    category TEXT,
    notes TEXT,
    patronage TEXT,
    source TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_novena_serving_rules_start_fixed ON novena_serving_rules (start_rule_type, start_rule_month, start_rule_day);
CREATE INDEX idx_novena_serving_rules_feast_fixed ON novena_serving_rules (feast_rule_type, feast_rule_month, feast_rule_day);
