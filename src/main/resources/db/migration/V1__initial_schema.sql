-- Protocol types
CREATE TYPE protocol_type AS ENUM ('HTTP', 'HTTPS', 'SFTP', 'AMQP', 'MQTT');

-- Pattern types
CREATE TYPE pattern_type AS ENUM ('REQUEST_REPLY', 'FIRE_FORGET', 'PUB_SUB');

-- HTTP methods
CREATE TYPE http_method AS ENUM ('GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'HEAD', 'OPTIONS');

-- Mock endpoints
CREATE TABLE mock_endpoint (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    protocol protocol_type NOT NULL,
    pattern pattern_type NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mock_endpoint_protocol ON mock_endpoint(protocol);
CREATE INDEX idx_mock_endpoint_enabled ON mock_endpoint(enabled);

-- HTTP specific configuration
CREATE TABLE http_config (
    id BIGSERIAL PRIMARY KEY,
    mock_endpoint_id BIGINT NOT NULL REFERENCES mock_endpoint(id) ON DELETE CASCADE,
    method http_method NOT NULL,
    path VARCHAR(1000) NOT NULL,
    path_regex BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(mock_endpoint_id)
);

CREATE INDEX idx_http_config_path ON http_config(path);

-- Request/Response pairs (for REQUEST_REPLY pattern)
CREATE TABLE mock_response (
    id BIGSERIAL PRIMARY KEY,
    mock_endpoint_id BIGINT NOT NULL REFERENCES mock_endpoint(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,

    -- Matching criteria (JSON format)
    match_headers JSONB,
    match_body JSONB,
    match_query_params JSONB,

    -- Response configuration
    response_status_code INTEGER,
    response_headers JSONB,
    response_body TEXT,
    response_delay_ms INTEGER DEFAULT 0,

    -- Conditional matching
    match_script TEXT, -- For advanced matching logic

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mock_response_endpoint ON mock_response(mock_endpoint_id);
CREATE INDEX idx_mock_response_priority ON mock_response(priority DESC);

-- Request logs
CREATE TABLE request_log (
    id BIGSERIAL PRIMARY KEY,
    mock_endpoint_id BIGINT REFERENCES mock_endpoint(id) ON DELETE SET NULL,
    mock_response_id BIGINT REFERENCES mock_response(id) ON DELETE SET NULL,

    protocol protocol_type NOT NULL,

    -- Request details
    request_method VARCHAR(50),
    request_path VARCHAR(1000),
    request_headers JSONB,
    request_query_params JSONB,
    request_body TEXT,

    -- Response details
    response_status_code INTEGER,
    response_headers JSONB,
    response_body TEXT,
    response_delay_ms INTEGER,

    -- Metadata
    matched BOOLEAN NOT NULL DEFAULT false,
    client_ip VARCHAR(45),
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_request_log_endpoint ON request_log(mock_endpoint_id);
CREATE INDEX idx_request_log_received_at ON request_log(received_at DESC);
CREATE INDEX idx_request_log_matched ON request_log(matched);

-- Update timestamps trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_mock_endpoint_updated_at BEFORE UPDATE ON mock_endpoint
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_mock_response_updated_at BEFORE UPDATE ON mock_response
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
