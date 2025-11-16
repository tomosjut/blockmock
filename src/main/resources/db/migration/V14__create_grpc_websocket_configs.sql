-- gRPC Mock Configuration
CREATE TABLE grpc_config (
    id BIGSERIAL PRIMARY KEY,
    mock_endpoint_id BIGINT NOT NULL UNIQUE REFERENCES mock_endpoint(id) ON DELETE CASCADE,

    -- Server settings
    port INTEGER NOT NULL DEFAULT 50051,

    -- Service definition
    service_name VARCHAR(255) NOT NULL,
    method_name VARCHAR(255) NOT NULL,

    -- Proto definition (simplified - just store the service/method names)
    package_name VARCHAR(255),

    -- Response configuration
    response_data TEXT, -- JSON format for the response message
    response_delay_ms INTEGER DEFAULT 0,

    -- Stream type
    is_client_streaming BOOLEAN DEFAULT false,
    is_server_streaming BOOLEAN DEFAULT false,

    -- Created/updated timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- WebSocket Mock Configuration
CREATE TABLE websocket_config (
    id BIGSERIAL PRIMARY KEY,
    mock_endpoint_id BIGINT NOT NULL UNIQUE REFERENCES mock_endpoint(id) ON DELETE CASCADE,

    -- WebSocket path
    path VARCHAR(500) NOT NULL,

    -- Behavior
    operation VARCHAR(50) NOT NULL, -- ECHO, BROADCAST, CUSTOM

    -- Auto-response configuration
    auto_response_enabled BOOLEAN DEFAULT false,
    auto_response_message TEXT,
    auto_response_delay_ms INTEGER DEFAULT 0,

    -- Connection limits
    max_connections INTEGER DEFAULT 100,

    -- Message format
    message_format VARCHAR(50) DEFAULT 'TEXT', -- TEXT, JSON, BINARY

    -- Created/updated timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_grpc_config_endpoint ON grpc_config(mock_endpoint_id);
CREATE INDEX idx_websocket_config_endpoint ON websocket_config(mock_endpoint_id);
