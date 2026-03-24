CREATE TABLE trigger_config (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL,
    test_suite_id BIGINT REFERENCES test_suite(id) ON DELETE SET NULL,
    http_url VARCHAR(2000),
    http_method VARCHAR(20) DEFAULT 'POST',
    http_body TEXT,
    http_headers JSONB,
    cron_expression VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT true,
    last_fired_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
