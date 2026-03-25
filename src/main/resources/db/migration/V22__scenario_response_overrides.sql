-- V22: Scenario response overrides
-- Allows a scenario to force a specific mock response on an endpoint during a run.

-- forced_response_id on mock_endpoint: set at run start, cleared at run end
ALTER TABLE mock_endpoint
    ADD COLUMN forced_response_id BIGINT REFERENCES mock_response(id) ON DELETE SET NULL;

-- Override table: which response should be active for a given endpoint in a scenario
CREATE TABLE scenario_response_override (
    id              BIGSERIAL PRIMARY KEY,
    test_scenario_id BIGINT NOT NULL REFERENCES test_scenario(id) ON DELETE CASCADE,
    mock_endpoint_id BIGINT NOT NULL REFERENCES mock_endpoint(id) ON DELETE CASCADE,
    mock_response_id BIGINT NOT NULL REFERENCES mock_response(id) ON DELETE CASCADE
);

CREATE SEQUENCE IF NOT EXISTS scenario_response_override_SEQ START WITH 1 INCREMENT BY 50;
