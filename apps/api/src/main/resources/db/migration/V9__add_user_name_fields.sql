ALTER TABLE users
    ADD COLUMN IF NOT EXISTS first_name TEXT,
    ADD COLUMN IF NOT EXISTS last_name TEXT;

UPDATE users
SET first_name = SPLIT_PART(display_name, ' ', 1)
WHERE first_name IS NULL
  AND display_name IS NOT NULL
  AND POSITION(' ' IN display_name) > 0;

UPDATE users
SET last_name = SUBSTRING(display_name FROM POSITION(' ' IN display_name) + 1)
WHERE last_name IS NULL
  AND display_name IS NOT NULL
  AND POSITION(' ' IN display_name) > 0;
