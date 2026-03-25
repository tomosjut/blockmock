-- V21: Introduce TestScenario as sub-unit of TestSuite
-- Scenarios own the expectations, triggers and runs; blocks stay on the suite.

-- 1. New table
CREATE TABLE test_scenario (
    id          BIGSERIAL PRIMARY KEY,
    test_suite_id BIGINT NOT NULL REFERENCES test_suite(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE SEQUENCE IF NOT EXISTS test_scenario_SEQ START WITH 1 INCREMENT BY 50;

-- 2. Migrate existing data: create a "Default" scenario for every existing suite
INSERT INTO test_scenario (test_suite_id, name, created_at, updated_at)
SELECT id, 'Default', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM test_suite;

-- 3. test_expectation: move FK from test_suite to test_scenario
ALTER TABLE test_expectation
    ADD COLUMN test_scenario_id BIGINT REFERENCES test_scenario(id) ON DELETE CASCADE;

UPDATE test_expectation te
SET test_scenario_id = (
    SELECT ts.id FROM test_scenario ts WHERE ts.test_suite_id = te.test_suite_id LIMIT 1
);

ALTER TABLE test_expectation ALTER COLUMN test_scenario_id SET NOT NULL;
ALTER TABLE test_expectation DROP COLUMN test_suite_id;

-- 4. trigger_config: move FK from test_suite to test_scenario
ALTER TABLE trigger_config
    ADD COLUMN test_scenario_id BIGINT REFERENCES test_scenario(id) ON DELETE SET NULL;

UPDATE trigger_config tc
SET test_scenario_id = (
    SELECT ts.id FROM test_scenario ts WHERE ts.test_suite_id = tc.test_suite_id LIMIT 1
)
WHERE tc.test_suite_id IS NOT NULL;

ALTER TABLE trigger_config DROP COLUMN test_suite_id;

-- Unique constraint now on scenario level
ALTER TABLE trigger_config DROP CONSTRAINT IF EXISTS uq_trigger_config_suite_name;
ALTER TABLE trigger_config
    ADD CONSTRAINT uq_trigger_config_scenario_name UNIQUE (test_scenario_id, name);

-- 5. test_run: move FK from test_suite to test_scenario
ALTER TABLE test_run
    ADD COLUMN test_scenario_id BIGINT REFERENCES test_scenario(id) ON DELETE CASCADE;

UPDATE test_run tr
SET test_scenario_id = (
    SELECT ts.id FROM test_scenario ts WHERE ts.test_suite_id = tr.test_suite_id LIMIT 1
);

ALTER TABLE test_run ALTER COLUMN test_scenario_id SET NOT NULL;
ALTER TABLE test_run DROP COLUMN test_suite_id;

-- 6. Drop old scenario/scenario_step tables (different concept, replaced by test_scenario)
DROP TABLE IF EXISTS scenario_step CASCADE;
DROP TABLE IF EXISTS scenario CASCADE;
DROP SEQUENCE IF EXISTS scenario_SEQ;
DROP SEQUENCE IF EXISTS scenario_step_SEQ;
