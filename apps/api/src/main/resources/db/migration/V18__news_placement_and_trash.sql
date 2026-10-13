ALTER TABLE news_articles ADD COLUMN placement TEXT NOT NULL DEFAULT 'MORE'
    CHECK (placement IN ('LEAD','SIDEBAR_1','SIDEBAR_2','MORE'));
ALTER TABLE news_articles ADD COLUMN deleted_at TIMESTAMPTZ;
UPDATE news_articles SET placement='LEAD' WHERE is_lead AND status<>'SCHEDULED';
UPDATE news_articles SET is_lead=false WHERE status='SCHEDULED';
DROP INDEX news_one_lead;
CREATE UNIQUE INDEX news_homepage_slots ON news_articles(placement)
    WHERE deleted_at IS NULL AND status='PUBLISHED' AND placement <> 'MORE';
ALTER TABLE news_articles ADD CONSTRAINT news_lead_matches_placement CHECK (is_lead = (placement='LEAD'));
