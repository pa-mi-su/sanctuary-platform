CREATE TABLE content_intentions (
    id BIGSERIAL PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    label_en TEXT NOT NULL,
    label_es TEXT NOT NULL,
    label_pl TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE content_intention_aliases (
    id BIGSERIAL PRIMARY KEY,
    intention_id BIGINT NOT NULL REFERENCES content_intentions(id) ON DELETE CASCADE,
    locale TEXT NOT NULL CHECK (locale IN ('en', 'es', 'pl')),
    alias_text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (intention_id, locale, alias_text)
);

CREATE TABLE novena_intention_links (
    novena_id TEXT NOT NULL REFERENCES novenas(id) ON DELETE CASCADE,
    intention_id BIGINT NOT NULL REFERENCES content_intentions(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (novena_id, intention_id)
);

CREATE TABLE saint_intention_links (
    saint_id TEXT NOT NULL REFERENCES saints(id) ON DELETE CASCADE,
    intention_id BIGINT NOT NULL REFERENCES content_intentions(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (saint_id, intention_id)
);

CREATE INDEX idx_content_intention_aliases_lookup ON content_intention_aliases (locale, alias_text);
CREATE INDEX idx_novena_intention_links_intention_id ON novena_intention_links (intention_id);
CREATE INDEX idx_saint_intention_links_intention_id ON saint_intention_links (intention_id);

WITH novena_rows AS (
    SELECT
        ni.*,
        COALESCE(en.intention_text, ni.intention_text) AS canonical_label,
        regexp_replace(
            lower(trim(COALESCE(en.intention_text, ni.intention_text))),
            '[^a-z0-9]+',
            '-',
            'g'
        ) AS canonical_slug
    FROM novena_intentions ni
    LEFT JOIN novena_intentions en
        ON en.novena_id = ni.novena_id
       AND en.sort_order = ni.sort_order
       AND en.locale = 'en'
),
novena_canon AS (
    SELECT
        canonical_slug,
        MIN(canonical_label) AS label_en,
        COALESCE(MIN(intention_text) FILTER (WHERE locale = 'es'), MIN(canonical_label)) AS label_es,
        COALESCE(MIN(intention_text) FILTER (WHERE locale = 'pl'), MIN(canonical_label)) AS label_pl
    FROM novena_rows
    WHERE canonical_slug IS NOT NULL AND canonical_slug <> ''
    GROUP BY canonical_slug
),
saint_rows AS (
    SELECT
        regexp_replace(lower(trim(patronage)), '[^a-z0-9]+', '-', 'g') AS slug,
        trim(patronage) AS label
    FROM saint_patronages
    WHERE trim(patronage) <> ''
    UNION
    SELECT
        regexp_replace(lower(trim(tag)), '[^a-z0-9]+', '-', 'g') AS slug,
        trim(tag) AS label
    FROM saint_tags
    WHERE trim(tag) <> ''
)
INSERT INTO content_intentions (slug, label_en, label_es, label_pl)
SELECT canonical_slug, label_en, label_es, label_pl
FROM novena_canon
ON CONFLICT (slug) DO NOTHING;

WITH saint_rows AS (
    SELECT
        regexp_replace(lower(trim(patronage)), '[^a-z0-9]+', '-', 'g') AS slug,
        trim(patronage) AS label
    FROM saint_patronages
    WHERE trim(patronage) <> ''
    UNION
    SELECT
        regexp_replace(lower(trim(tag)), '[^a-z0-9]+', '-', 'g') AS slug,
        trim(tag) AS label
    FROM saint_tags
    WHERE trim(tag) <> ''
)
INSERT INTO content_intentions (slug, label_en, label_es, label_pl)
SELECT slug, MIN(label), MIN(label), MIN(label)
FROM saint_rows
WHERE slug IS NOT NULL AND slug <> ''
GROUP BY slug
ON CONFLICT (slug) DO NOTHING;

WITH novena_rows AS (
    SELECT
        ni.*,
        regexp_replace(
            lower(trim(COALESCE(en.intention_text, ni.intention_text))),
            '[^a-z0-9]+',
            '-',
            'g'
        ) AS canonical_slug
    FROM novena_intentions ni
    LEFT JOIN novena_intentions en
        ON en.novena_id = ni.novena_id
       AND en.sort_order = ni.sort_order
       AND en.locale = 'en'
)
INSERT INTO novena_intention_links (novena_id, intention_id, sort_order)
SELECT DISTINCT nr.novena_id, ci.id, nr.sort_order
FROM novena_rows nr
JOIN content_intentions ci ON ci.slug = nr.canonical_slug
ON CONFLICT (novena_id, intention_id) DO NOTHING;

WITH saint_rows AS (
    SELECT
        saint_id,
        regexp_replace(lower(trim(patronage)), '[^a-z0-9]+', '-', 'g') AS slug
    FROM saint_patronages
    WHERE trim(patronage) <> ''
    UNION
    SELECT
        saint_id,
        regexp_replace(lower(trim(tag)), '[^a-z0-9]+', '-', 'g') AS slug
    FROM saint_tags
    WHERE trim(tag) <> ''
)
INSERT INTO saint_intention_links (saint_id, intention_id)
SELECT DISTINCT sr.saint_id, ci.id
FROM saint_rows sr
JOIN content_intentions ci ON ci.slug = sr.slug
ON CONFLICT (saint_id, intention_id) DO NOTHING;

WITH novena_rows AS (
    SELECT
        ni.*,
        regexp_replace(
            lower(trim(COALESCE(en.intention_text, ni.intention_text))),
            '[^a-z0-9]+',
            '-',
            'g'
        ) AS canonical_slug
    FROM novena_intentions ni
    LEFT JOIN novena_intentions en
        ON en.novena_id = ni.novena_id
       AND en.sort_order = ni.sort_order
       AND en.locale = 'en'
)
INSERT INTO content_intention_aliases (intention_id, locale, alias_text)
SELECT DISTINCT ci.id, nr.locale, nr.intention_text
FROM novena_rows nr
JOIN content_intentions ci ON ci.slug = nr.canonical_slug
WHERE trim(nr.intention_text) <> ''
ON CONFLICT (intention_id, locale, alias_text) DO NOTHING;

WITH saint_rows AS (
    SELECT
        regexp_replace(lower(trim(patronage)), '[^a-z0-9]+', '-', 'g') AS slug,
        trim(patronage) AS label
    FROM saint_patronages
    WHERE trim(patronage) <> ''
    UNION
    SELECT
        regexp_replace(lower(trim(tag)), '[^a-z0-9]+', '-', 'g') AS slug,
        trim(tag) AS label
    FROM saint_tags
    WHERE trim(tag) <> ''
)
INSERT INTO content_intention_aliases (intention_id, locale, alias_text)
SELECT DISTINCT ci.id, locale.code, sr.label
FROM saint_rows sr
JOIN content_intentions ci ON ci.slug = sr.slug
CROSS JOIN (VALUES ('en'), ('es'), ('pl')) AS locale(code)
ON CONFLICT (intention_id, locale, alias_text) DO NOTHING;
