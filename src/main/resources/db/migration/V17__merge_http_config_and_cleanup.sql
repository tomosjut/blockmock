-- V17: Merge http_config into mock_endpoint and drop unused protocol config tables

-- Step 1: Add http_ columns to mock_endpoint
ALTER TABLE mock_endpoint
    ADD COLUMN IF NOT EXISTS http_method VARCHAR(50),
    ADD COLUMN IF NOT EXISTS http_path VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS http_path_regex BOOLEAN DEFAULT false;

-- Step 2: Migrate existing data from http_config into mock_endpoint
UPDATE mock_endpoint me
SET http_method    = hc.method,
    http_path      = hc.path,
    http_path_regex = hc.path_regex
FROM http_config hc
WHERE hc.mock_endpoint_id = me.id;

-- Step 3: Normalize any non-HTTP protocol values in request_log
UPDATE request_log
SET protocol = 'HTTP'
WHERE protocol NOT IN ('HTTP');

-- Step 4: Drop unused protocol config tables
DROP TABLE IF EXISTS http_config CASCADE;
DROP TABLE IF EXISTS sftp_config CASCADE;
DROP TABLE IF EXISTS amqp_config CASCADE;
DROP TABLE IF EXISTS sql_query_mock CASCADE;
DROP TABLE IF EXISTS sql_config CASCADE;
DROP TABLE IF EXISTS nosql_config CASCADE;
DROP TABLE IF EXISTS kafka_config CASCADE;
DROP TABLE IF EXISTS grpc_config CASCADE;
DROP TABLE IF EXISTS websocket_config CASCADE;
