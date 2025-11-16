-- Change match_body from JSONB to TEXT for regex support
ALTER TABLE mock_response ALTER COLUMN match_body TYPE TEXT;
