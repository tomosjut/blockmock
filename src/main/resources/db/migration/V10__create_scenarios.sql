-- Scenarios for sequential endpoint activation
CREATE TABLE scenario (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    color VARCHAR(20) DEFAULT '#667eea',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Scenario Steps (ordered sequence)
CREATE TABLE scenario_step (
    id BIGSERIAL PRIMARY KEY,
    scenario_id BIGINT NOT NULL REFERENCES scenario(id) ON DELETE CASCADE,
    step_order INTEGER NOT NULL, -- Order of execution
    action VARCHAR(50) NOT NULL, -- ENABLE, DISABLE, DELAY
    mock_endpoint_id BIGINT REFERENCES mock_endpoint(id) ON DELETE CASCADE,
    delay_ms INTEGER DEFAULT 0, -- For DELAY action
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_scenario_step_scenario ON scenario_step(scenario_id);
CREATE INDEX idx_scenario_step_order ON scenario_step(scenario_id, step_order);

-- Sequences
CREATE SEQUENCE IF NOT EXISTS scenario_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS scenario_step_SEQ START WITH 1 INCREMENT BY 50;
