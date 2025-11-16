-- Kafka Mock Configuration
CREATE TABLE kafka_config (
    id BIGSERIAL PRIMARY KEY,
    mock_endpoint_id BIGINT NOT NULL UNIQUE REFERENCES mock_endpoint(id) ON DELETE CASCADE,

    -- Broker settings
    bootstrap_servers VARCHAR(500) DEFAULT 'localhost:9092',

    -- Topic configuration
    topic_name VARCHAR(255) NOT NULL,
    num_partitions INTEGER DEFAULT 1,
    replication_factor INTEGER DEFAULT 1,

    -- Consumer/Producer mode
    operation VARCHAR(50) NOT NULL, -- PRODUCE, CONSUME, BOTH

    -- Consumer settings
    group_id VARCHAR(255),
    auto_offset_reset VARCHAR(50) DEFAULT 'earliest', -- earliest, latest, none
    enable_auto_commit BOOLEAN DEFAULT true,

    -- Message content for producing
    message_key VARCHAR(500),
    message_value TEXT,
    message_headers TEXT, -- JSON format

    -- Created/updated timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kafka_config_endpoint ON kafka_config(mock_endpoint_id);
