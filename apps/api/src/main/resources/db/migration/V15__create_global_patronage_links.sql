CREATE TABLE IF NOT EXISTS content_patronages (
    id BIGSERIAL PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    label_en TEXT NOT NULL,
    label_es TEXT NOT NULL,
    label_pl TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS content_patronage_aliases (
    id BIGSERIAL PRIMARY KEY,
    patronage_id BIGINT NOT NULL REFERENCES content_patronages(id) ON DELETE CASCADE,
    locale TEXT NOT NULL CHECK (locale IN ('en', 'es', 'pl')),
    alias_text TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (patronage_id, locale, alias_text)
);

CREATE TABLE IF NOT EXISTS saint_patronage_links (
    saint_id TEXT NOT NULL REFERENCES saints(id) ON DELETE CASCADE,
    patronage_id BIGINT NOT NULL REFERENCES content_patronages(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (saint_id, patronage_id)
);

CREATE INDEX IF NOT EXISTS idx_content_patronage_aliases_lookup
    ON content_patronage_aliases (locale, alias_text);
CREATE INDEX IF NOT EXISTS idx_saint_patronage_links_patronage_id
    ON saint_patronage_links (patronage_id);

WITH source_patronages AS (
    SELECT DISTINCT
        regexp_replace(lower(trim(patronage)), '[^a-z0-9]+', '-', 'g') AS slug,
        trim(patronage) AS label_en
    FROM saint_patronages
    WHERE trim(patronage) <> ''
),
translated AS (
    SELECT
        slug,
        label_en,
        CASE label_en
            WHEN 'California' THEN 'California'
            WHEN 'Chastity' THEN 'Castidad'
            WHEN 'Engaged couples' THEN 'Parejas comprometidas'
            WHEN 'Gardeners' THEN 'Jardineros'
            WHEN 'Girl Scouts' THEN 'Guías Scouts'
            WHEN 'Hispanic Americans' THEN 'Hispanoamericanos'
            WHEN 'Rape survivors' THEN 'Sobrevivientes de violación'
            WHEN 'Virgins' THEN 'Vírgenes'
            WHEN 'Vocations' THEN 'Vocaciones'
            WHEN 'Young girls' THEN 'Niñas'
            ELSE label_en
        END AS label_es,
        CASE label_en
            WHEN 'California' THEN 'Kalifornia'
            WHEN 'Chastity' THEN 'Czystość'
            WHEN 'Engaged couples' THEN 'Narzeczeni'
            WHEN 'Gardeners' THEN 'Ogrodnicy'
            WHEN 'Girl Scouts' THEN 'Harcerki'
            WHEN 'Hispanic Americans' THEN 'Amerykanie pochodzenia latynoskiego'
            WHEN 'Rape survivors' THEN 'Osoby ocalałe z gwałtu'
            WHEN 'Virgins' THEN 'Dziewice'
            WHEN 'Vocations' THEN 'Powołania'
            WHEN 'Young girls' THEN 'Młode dziewczęta'
            ELSE label_en
        END AS label_pl
    FROM source_patronages
)
INSERT INTO content_patronages (slug, label_en, label_es, label_pl)
SELECT slug, MIN(label_en), MIN(label_es), MIN(label_pl)
FROM translated
WHERE slug IS NOT NULL AND slug <> ''
GROUP BY slug
ON CONFLICT (slug) DO UPDATE SET
    label_en = EXCLUDED.label_en,
    label_es = EXCLUDED.label_es,
    label_pl = EXCLUDED.label_pl,
    updated_at = NOW();

WITH source_patronages AS (
    SELECT
        saint_id,
        regexp_replace(lower(trim(patronage)), '[^a-z0-9]+', '-', 'g') AS slug,
        ROW_NUMBER() OVER (
            PARTITION BY saint_id
            ORDER BY LOWER(trim(patronage)), trim(patronage)
        ) - 1 AS sort_order
    FROM saint_patronages
    WHERE trim(patronage) <> ''
)
INSERT INTO saint_patronage_links (saint_id, patronage_id, sort_order)
SELECT sp.saint_id, cp.id, sp.sort_order
FROM source_patronages sp
JOIN content_patronages cp ON cp.slug = sp.slug
ON CONFLICT (saint_id, patronage_id) DO UPDATE SET
    sort_order = EXCLUDED.sort_order;

INSERT INTO content_patronage_aliases (patronage_id, locale, alias_text)
SELECT DISTINCT cp.id, alias.locale, alias.label
FROM content_patronages cp
CROSS JOIN LATERAL (
    VALUES
        ('en', cp.label_en),
        ('es', cp.label_es),
        ('pl', cp.label_pl),
        ('es', cp.label_en),
        ('pl', cp.label_en)
) AS alias(locale, label)
WHERE trim(alias.label) <> ''
ON CONFLICT (patronage_id, locale, alias_text) DO NOTHING;

DELETE FROM saint_intention_links;

DELETE FROM content_intentions ci
WHERE NOT EXISTS (
    SELECT 1
    FROM novena_intention_links nil
    WHERE nil.intention_id = ci.id
);
