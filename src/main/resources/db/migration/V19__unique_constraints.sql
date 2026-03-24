ALTER TABLE test_suite ADD CONSTRAINT uq_test_suite_name UNIQUE (name);
ALTER TABLE trigger_config ADD CONSTRAINT uq_trigger_config_suite_name UNIQUE (test_suite_id, name);
