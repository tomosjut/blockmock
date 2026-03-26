-- =============================================================================
-- V2: AMQP support — joined inheritance for MockEndpoint and TriggerConfig
-- =============================================================================

-- -----------------------------------------------------------------------------
-- mock_endpoint: add dtype discriminator column
-- -----------------------------------------------------------------------------

ALTER TABLE mock_endpoint ADD COLUMN dtype VARCHAR(50);
UPDATE mock_endpoint SET dtype = 'HTTP';
ALTER TABLE mock_endpoint ALTER COLUMN dtype SET NOT NULL;

-- HTTP-specific fields move to joined subtable
CREATE TABLE mock_endpoint_http (
    id              BIGINT PRIMARY KEY REFERENCES mock_endpoint(id) ON DELETE CASCADE,
    http_method     VARCHAR(50),
    http_path       VARCHAR(1000),
    http_path_regex BOOLEAN DEFAULT false
);

INSERT INTO mock_endpoint_http (id, http_method, http_path, http_path_regex)
SELECT id, http_method, http_path, http_path_regex FROM mock_endpoint;

ALTER TABLE mock_endpoint DROP COLUMN http_method;
ALTER TABLE mock_endpoint DROP COLUMN http_path;
ALTER TABLE mock_endpoint DROP COLUMN http_path_regex;

-- AMQP endpoint subtable
CREATE TABLE mock_endpoint_amqp (
    id           BIGINT PRIMARY KEY REFERENCES mock_endpoint(id) ON DELETE CASCADE,
    amqp_address VARCHAR(500),
    amqp_pattern VARCHAR(20)
);

-- -----------------------------------------------------------------------------
-- trigger_config: add dtype discriminator column
-- -----------------------------------------------------------------------------

ALTER TABLE trigger_config ADD COLUMN dtype VARCHAR(50);
UPDATE trigger_config SET dtype = type;
ALTER TABLE trigger_config ALTER COLUMN dtype SET NOT NULL;

-- HTTP trigger subtable
CREATE TABLE trigger_config_http (
    id           BIGINT PRIMARY KEY REFERENCES trigger_config(id) ON DELETE CASCADE,
    http_url     VARCHAR(2000),
    http_method  VARCHAR(20) DEFAULT 'POST',
    http_body    TEXT,
    http_headers JSONB
);

INSERT INTO trigger_config_http (id, http_url, http_method, http_body, http_headers)
SELECT id, http_url, http_method, http_body, http_headers FROM trigger_config
WHERE type = 'HTTP';

-- CRON trigger subtable
CREATE TABLE trigger_config_cron (
    id              BIGINT PRIMARY KEY REFERENCES trigger_config(id) ON DELETE CASCADE,
    cron_expression VARCHAR(100)
);

INSERT INTO trigger_config_cron (id, cron_expression)
SELECT id, cron_expression FROM trigger_config
WHERE type = 'CRON';

-- AMQP trigger subtable
CREATE TABLE trigger_config_amqp (
    id              BIGINT PRIMARY KEY REFERENCES trigger_config(id) ON DELETE CASCADE,
    amqp_address    VARCHAR(500),
    amqp_body       TEXT,
    amqp_properties JSONB
);

ALTER TABLE trigger_config DROP COLUMN http_url;
ALTER TABLE trigger_config DROP COLUMN http_method;
ALTER TABLE trigger_config DROP COLUMN http_body;
ALTER TABLE trigger_config DROP COLUMN http_headers;
ALTER TABLE trigger_config DROP COLUMN cron_expression;

-- -----------------------------------------------------------------------------
-- request_log: AMQP-specific fields
-- -----------------------------------------------------------------------------

ALTER TABLE request_log
    ADD COLUMN amqp_address        VARCHAR(500),
    ADD COLUMN amqp_subject        VARCHAR(500),
    ADD COLUMN amqp_message_id     VARCHAR(500),
    ADD COLUMN amqp_correlation_id VARCHAR(500),
    ADD COLUMN amqp_reply_to       VARCHAR(500),
    ADD COLUMN amqp_properties     JSONB;
