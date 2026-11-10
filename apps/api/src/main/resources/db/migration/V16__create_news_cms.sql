CREATE TABLE news_images (
    id UUID PRIMARY KEY,
    content_type TEXT NOT NULL CHECK (content_type IN ('image/jpeg', 'image/png')),
    data BYTEA NOT NULL CHECK (octet_length(data) <= 5242880),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE news_articles (
    id UUID PRIMARY KEY,
    title TEXT NOT NULL,
    category TEXT NOT NULL CHECK (category IN ('Vatican', 'U.S. Church')),
    summary TEXT NOT NULL,
    body TEXT NOT NULL,
    author TEXT NOT NULL,
    image_id UUID REFERENCES news_images(id),
    image_alt TEXT NOT NULL DEFAULT '',
    image_caption TEXT NOT NULL DEFAULT '',
    image_credit TEXT NOT NULL DEFAULT '',
    sources TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED', 'SCHEDULED')),
    publish_at TIMESTAMPTZ,
    is_lead BOOLEAN NOT NULL DEFAULT false,
    version INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (status = 'DRAFT' OR publish_at IS NOT NULL)
);
CREATE UNIQUE INDEX news_one_lead ON news_articles (is_lead) WHERE is_lead;
CREATE INDEX news_publication ON news_articles (publish_at DESC) WHERE status <> 'DRAFT';
CREATE TABLE news_corrections (
    id UUID PRIMARY KEY,
    article_id UUID NOT NULL REFERENCES news_articles(id),
    note TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE news_audit (
    id UUID PRIMARY KEY,
    article_id UUID NOT NULL REFERENCES news_articles(id),
    actor_sub TEXT NOT NULL,
    action TEXT NOT NULL,
    version INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
