-- Keep dispatch/idempotency records while allowing the owner to clear completed history.
ALTER TABLE push_messages ADD COLUMN history_cleared_at TIMESTAMPTZ;
