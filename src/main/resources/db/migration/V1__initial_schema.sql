-- =============================================================================
-- BlockMock initial schema
-- =============================================================================

-- Shared updated_at trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- =============================================================================
-- Mock endpoints
-- =============================================================================

CREATE TABLE mock_endpoint (
    id                       BIGSERIAL PRIMARY KEY,
    name                     VARCHAR(255) NOT NULL,
    description              TEXT,
    protocol                 VARCHAR(50) NOT NULL,
    pattern                  VARCHAR(50) NOT NULL,
    enabled                  BOOLEAN NOT NULL DEFAULT true,
    http_method              VARCHAR(50),
    http_path                VARCHAR(1000),
    http_path_regex          BOOLEAN DEFAULT false,
    total_requests           BIGINT DEFAULT 0,
    matched_requests         BIGINT DEFAULT 0,
    unmatched_requests       BIGINT DEFAULT 0,
    last_request_at          TIMESTAMP,
    average_response_time_ms INTEGER DEFAULT 0,
    forced_response_id       BIGINT,  -- FK added after mock_response is created
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mock_endpoint_enabled ON mock_endpoint(enabled);
CREATE SEQUENCE IF NOT EXISTS mock_endpoint_SEQ START WITH 1 INCREMENT BY 50;

CREATE TRIGGER update_mock_endpoint_updated_at BEFORE UPDATE ON mock_endpoint
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================================================
-- Mock responses
-- =============================================================================

CREATE TABLE mock_response (
    id                   BIGSERIAL PRIMARY KEY,
    mock_endpoint_id     BIGINT NOT NULL REFERENCES mock_endpoint(id) ON DELETE CASCADE,
    name                 VARCHAR(255) NOT NULL,
    priority             INTEGER NOT NULL DEFAULT 0,
    match_headers        JSONB,
    match_body           TEXT,
    match_query_params   JSONB,
    response_status_code INTEGER,
    response_headers     JSONB,
    response_body        TEXT,
    response_delay_ms    INTEGER DEFAULT 0,
    match_script         TEXT,
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mock_response_endpoint ON mock_response(mock_endpoint_id);
CREATE INDEX idx_mock_response_priority ON mock_response(priority DESC);
CREATE SEQUENCE IF NOT EXISTS mock_response_SEQ START WITH 1 INCREMENT BY 50;

CREATE TRIGGER update_mock_response_updated_at BEFORE UPDATE ON mock_response
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Forward reference: mock_endpoint.forced_response_id → mock_response
ALTER TABLE mock_endpoint
    ADD CONSTRAINT fk_mock_endpoint_forced_response
    FOREIGN KEY (forced_response_id) REFERENCES mock_response(id) ON DELETE SET NULL;

-- =============================================================================
-- Request logs
-- =============================================================================

CREATE TABLE request_log (
    id                   BIGSERIAL PRIMARY KEY,
    mock_endpoint_id     BIGINT REFERENCES mock_endpoint(id) ON DELETE SET NULL,
    mock_response_id     BIGINT REFERENCES mock_response(id) ON DELETE SET NULL,
    protocol             VARCHAR(50) NOT NULL,
    request_method       VARCHAR(50),
    request_path         VARCHAR(1000),
    request_headers      JSONB,
    request_query_params JSONB,
    request_body         TEXT,
    response_status_code INTEGER,
    response_headers     JSONB,
    response_body        TEXT,
    response_delay_ms    INTEGER,
    matched              BOOLEAN NOT NULL DEFAULT false,
    client_ip            VARCHAR(45),
    received_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_request_log_endpoint    ON request_log(mock_endpoint_id);
CREATE INDEX idx_request_log_received_at ON request_log(received_at DESC);
CREATE INDEX idx_request_log_matched     ON request_log(matched);
CREATE SEQUENCE IF NOT EXISTS request_log_SEQ START WITH 1 INCREMENT BY 50;

-- =============================================================================
-- Blocks (groups of endpoints)
-- =============================================================================

CREATE TABLE block (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    color       VARCHAR(7) DEFAULT '#667eea',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE IF NOT EXISTS block_SEQ START WITH 1 INCREMENT BY 50;

CREATE TRIGGER update_block_updated_at BEFORE UPDATE ON block
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE block_endpoint (
    block_id         BIGINT NOT NULL REFERENCES block(id) ON DELETE CASCADE,
    mock_endpoint_id BIGINT NOT NULL REFERENCES mock_endpoint(id) ON DELETE CASCADE,
    added_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (block_id, mock_endpoint_id)
);

CREATE INDEX idx_block_endpoint_block    ON block_endpoint(block_id);
CREATE INDEX idx_block_endpoint_endpoint ON block_endpoint(mock_endpoint_id);

-- =============================================================================
-- Test suites, scenarios, expectations, runs
-- =============================================================================

CREATE TABLE test_suite (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    color       VARCHAR(20) DEFAULT '#667eea',
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE IF NOT EXISTS test_suite_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE test_suite_block (
    test_suite_id BIGINT NOT NULL REFERENCES test_suite(id) ON DELETE CASCADE,
    block_id      BIGINT NOT NULL REFERENCES block(id) ON DELETE CASCADE,
    PRIMARY KEY (test_suite_id, block_id)
);

CREATE TABLE test_scenario (
    id            BIGSERIAL PRIMARY KEY,
    test_suite_id BIGINT NOT NULL REFERENCES test_suite(id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE IF NOT EXISTS test_scenario_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE test_expectation (
    id                     BIGSERIAL PRIMARY KEY,
    test_scenario_id       BIGINT NOT NULL REFERENCES test_scenario(id) ON DELETE CASCADE,
    name                   VARCHAR(255) NOT NULL,
    mock_endpoint_id       BIGINT REFERENCES mock_endpoint(id) ON DELETE SET NULL,
    min_call_count         INTEGER NOT NULL DEFAULT 1,
    max_call_count         INTEGER,
    required_body_contains TEXT,
    required_headers       JSONB,
    expectation_order      INTEGER,
    created_at             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE IF NOT EXISTS test_expectation_SEQ START WITH 1 INCREMENT BY 50;
CREATE INDEX idx_test_expectation_scenario ON test_expectation(test_scenario_id);

CREATE TABLE test_run (
    id               BIGSERIAL PRIMARY KEY,
    test_scenario_id BIGINT NOT NULL REFERENCES test_scenario(id) ON DELETE CASCADE,
    status           VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    started_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at     TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE IF NOT EXISTS test_run_SEQ START WITH 1 INCREMENT BY 50;
CREATE INDEX idx_test_run_scenario ON test_run(test_scenario_id);
CREATE INDEX idx_test_run_status   ON test_run(status);

CREATE TABLE test_expectation_result (
    id                  BIGSERIAL PRIMARY KEY,
    test_run_id         BIGINT NOT NULL REFERENCES test_run(id) ON DELETE CASCADE,
    test_expectation_id BIGINT NOT NULL REFERENCES test_expectation(id) ON DELETE CASCADE,
    passed              BOOLEAN NOT NULL,
    actual_call_count   INTEGER NOT NULL DEFAULT 0,
    failure_reason      TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE IF NOT EXISTS test_expectation_result_SEQ START WITH 1 INCREMENT BY 50;
CREATE INDEX idx_expectation_result_run ON test_expectation_result(test_run_id);

-- =============================================================================
-- Scenario response overrides
-- =============================================================================

CREATE TABLE scenario_response_override (
    id               BIGSERIAL PRIMARY KEY,
    test_scenario_id BIGINT NOT NULL REFERENCES test_scenario(id) ON DELETE CASCADE,
    mock_endpoint_id BIGINT NOT NULL REFERENCES mock_endpoint(id) ON DELETE CASCADE,
    mock_response_id BIGINT NOT NULL REFERENCES mock_response(id) ON DELETE CASCADE
);

CREATE SEQUENCE IF NOT EXISTS scenario_response_override_SEQ START WITH 1 INCREMENT BY 50;

-- =============================================================================
-- Triggers
-- =============================================================================

CREATE TABLE trigger_config (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    type             VARCHAR(50) NOT NULL,
    test_scenario_id BIGINT REFERENCES test_scenario(id) ON DELETE SET NULL,
    http_url         VARCHAR(2000),
    http_method      VARCHAR(20) DEFAULT 'POST',
    http_body        TEXT,
    http_headers     JSONB,
    cron_expression  VARCHAR(100),
    enabled          BOOLEAN NOT NULL DEFAULT true,
    last_fired_at    TIMESTAMP,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_trigger_config_scenario_name UNIQUE (test_scenario_id, name)
);

CREATE SEQUENCE IF NOT EXISTS trigger_config_SEQ START WITH 1 INCREMENT BY 50;
