ALTER TABLE users ADD COLUMN last_login_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN last_seen_at TIMESTAMPTZ DEFAULT now();
-- Existing rows must not appear active merely because the migration ran.
UPDATE users SET last_seen_at = NULL;
ALTER TABLE users ADD COLUMN login_count BIGINT NOT NULL DEFAULT 0;
-- Historical last_sign_in_at is updated on profile requests; it is not reliable login history.
CREATE INDEX users_last_seen_at_idx ON users(last_seen_at DESC);
CREATE OR REPLACE FUNCTION record_novena_activity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' OR NEW.status IS DISTINCT FROM OLD.status OR NEW.completed_days IS DISTINCT FROM OLD.completed_days THEN
        INSERT INTO user_activity_events(user_id, activity_type, resource_type, resource_id, activity_date, metadata)
        VALUES (NEW.user_id, CASE WHEN NEW.status = 'completed' THEN 'novena_completed' WHEN TG_OP = 'INSERT' THEN 'novena_started' ELSE 'novena_progress' END,
                'novena', NEW.novena_id, (now() AT TIME ZONE 'UTC')::date,
                jsonb_build_object('status', NEW.status, 'completedDays', NEW.completed_days, 'currentDay', NEW.current_day));
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER record_novena_activity AFTER INSERT OR UPDATE ON user_novena_commitments FOR EACH ROW EXECUTE FUNCTION record_novena_activity();
CREATE OR REPLACE FUNCTION record_favorite_activity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO user_activity_events(user_id, activity_type, resource_type, resource_id, activity_date)
    VALUES (NEW.user_id, 'favorite_saved', NEW.item_type, NEW.item_id, (now() AT TIME ZONE 'UTC')::date);
    RETURN NEW;
END $$;
CREATE TRIGGER record_favorite_activity AFTER INSERT ON user_favorites FOR EACH ROW EXECUTE FUNCTION record_favorite_activity();
