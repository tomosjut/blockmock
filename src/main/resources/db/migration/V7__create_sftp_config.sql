-- SFTP Configuration table
CREATE TABLE sftp_config (
    id BIGSERIAL PRIMARY KEY,
    mock_endpoint_id BIGINT NOT NULL UNIQUE REFERENCES mock_endpoint(id) ON DELETE CASCADE,

    -- SFTP specific fields
    port INTEGER NOT NULL DEFAULT 2222,
    operation VARCHAR(50) NOT NULL, -- UPLOAD, DOWNLOAD, LIST, DELETE
    path_pattern VARCHAR(500) NOT NULL,
    path_is_regex BOOLEAN NOT NULL DEFAULT false,

    -- Authentication
    username VARCHAR(255),
    password VARCHAR(255),
    allow_anonymous BOOLEAN NOT NULL DEFAULT false,

    -- Mock response for different operations
    -- For DOWNLOAD: file content to return
    -- For LIST: directory listing JSON
    -- For UPLOAD/DELETE: success/failure response
    mock_response_content TEXT,
    success BOOLEAN NOT NULL DEFAULT true,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add index on mock_endpoint_id for faster lookups
CREATE INDEX idx_sftp_config_endpoint ON sftp_config(mock_endpoint_id);

-- Add sequence for sftp_config
CREATE SEQUENCE IF NOT EXISTS sftp_config_SEQ START WITH 1 INCREMENT BY 50;
