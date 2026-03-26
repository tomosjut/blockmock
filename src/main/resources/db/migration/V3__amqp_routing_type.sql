-- AMQP routing type: ANYCAST (queue) or MULTICAST (topic)
-- Default ANYCAST matches point-to-point queue semantics

ALTER TABLE mock_endpoint_amqp
    ADD COLUMN IF NOT EXISTS amqp_routing_type VARCHAR(10) NOT NULL DEFAULT 'ANYCAST';

ALTER TABLE trigger_config_amqp
    ADD COLUMN IF NOT EXISTS amqp_routing_type VARCHAR(10) NOT NULL DEFAULT 'ANYCAST';
