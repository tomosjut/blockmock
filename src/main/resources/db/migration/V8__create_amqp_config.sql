-- AMQP Configuration table
CREATE TABLE amqp_config (
    id BIGSERIAL PRIMARY KEY,
    mock_endpoint_id BIGINT NOT NULL UNIQUE REFERENCES mock_endpoint(id) ON DELETE CASCADE,

    -- AMQP specific fields
    host VARCHAR(255) NOT NULL DEFAULT 'localhost',
    port INTEGER NOT NULL DEFAULT 5672,
    virtual_host VARCHAR(255) DEFAULT '/',

    -- Exchange configuration
    exchange_name VARCHAR(255) NOT NULL,
    exchange_type VARCHAR(50) NOT NULL DEFAULT 'direct', -- direct, fanout, topic, headers
    exchange_durable BOOLEAN NOT NULL DEFAULT true,
    exchange_auto_delete BOOLEAN NOT NULL DEFAULT false,

    -- Queue configuration
    queue_name VARCHAR(255),
    queue_durable BOOLEAN NOT NULL DEFAULT true,
    queue_exclusive BOOLEAN NOT NULL DEFAULT false,
    queue_auto_delete BOOLEAN NOT NULL DEFAULT false,

    -- Routing
    routing_key VARCHAR(255),
    binding_pattern VARCHAR(255), -- For topic exchanges

    -- Authentication
    username VARCHAR(255),
    password VARCHAR(255),

    -- Mock behavior
    operation VARCHAR(50) NOT NULL, -- PUBLISH, CONSUME, BOTH
    mock_message_content TEXT,
    mock_message_headers TEXT, -- JSON string
    auto_reply BOOLEAN NOT NULL DEFAULT false,
    reply_delay_ms INTEGER DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add index on mock_endpoint_id for faster lookups
CREATE INDEX idx_amqp_config_endpoint ON amqp_config(mock_endpoint_id);

-- Add sequence for amqp_config
CREATE SEQUENCE IF NOT EXISTS amqp_config_SEQ START WITH 1 INCREMENT BY 50;
