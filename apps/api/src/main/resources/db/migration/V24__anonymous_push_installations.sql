ALTER TABLE push_devices ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE push_devices ADD COLUMN installation_secret_hash TEXT;
ALTER TABLE push_devices ADD COLUMN registration_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE push_messages DROP CONSTRAINT push_messages_audience_check;
ALTER TABLE push_messages ADD CONSTRAINT push_messages_audience_check CHECK(audience IN ('all','ios','android','test','guests','members'));
CREATE TABLE push_registration_limits (
 bucket TEXT NOT NULL,
 window_start BIGINT NOT NULL,
 requests INTEGER NOT NULL,
 PRIMARY KEY(bucket,window_start)
);
