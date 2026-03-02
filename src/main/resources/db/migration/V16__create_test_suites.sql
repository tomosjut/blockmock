CREATE TABLE test_suite (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    color VARCHAR(20) DEFAULT '#667eea',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE SEQUENCE IF NOT EXISTS test_suite_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE test_suite_block (
    test_suite_id BIGINT REFERENCES test_suite(id) ON DELETE CASCADE,
    block_id BIGINT REFERENCES block(id) ON DELETE CASCADE,
    PRIMARY KEY (test_suite_id, block_id)
);

CREATE TABLE test_expectation (
    id BIGSERIAL PRIMARY KEY,
    test_suite_id BIGINT NOT NULL REFERENCES test_suite(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    mock_endpoint_id BIGINT REFERENCES mock_endpoint(id) ON DELETE SET NULL,
    min_call_count INTEGER NOT NULL DEFAULT 1,
    max_call_count INTEGER,
    required_body_contains TEXT,
    required_headers JSONB,
    expectation_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE SEQUENCE IF NOT EXISTS test_expectation_SEQ START WITH 1 INCREMENT BY 50;
CREATE INDEX idx_test_expectation_suite ON test_expectation(test_suite_id);

CREATE TABLE test_run (
    id BIGSERIAL PRIMARY KEY,
    test_suite_id BIGINT NOT NULL REFERENCES test_suite(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE SEQUENCE IF NOT EXISTS test_run_SEQ START WITH 1 INCREMENT BY 50;
CREATE INDEX idx_test_run_suite ON test_run(test_suite_id);
CREATE INDEX idx_test_run_status ON test_run(status);

CREATE TABLE test_expectation_result (
    id BIGSERIAL PRIMARY KEY,
    test_run_id BIGINT NOT NULL REFERENCES test_run(id) ON DELETE CASCADE,
    test_expectation_id BIGINT NOT NULL REFERENCES test_expectation(id) ON DELETE CASCADE,
    passed BOOLEAN NOT NULL,
    actual_call_count INTEGER NOT NULL DEFAULT 0,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE SEQUENCE IF NOT EXISTS test_expectation_result_SEQ START WITH 1 INCREMENT BY 50;
CREATE INDEX idx_expectation_result_run ON test_expectation_result(test_run_id);
