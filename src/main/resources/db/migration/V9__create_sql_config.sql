-- SQL Database Mock Configuration
CREATE TABLE sql_config (
    id BIGSERIAL PRIMARY KEY,
    mock_endpoint_id BIGINT NOT NULL UNIQUE REFERENCES mock_endpoint(id) ON DELETE CASCADE,

    -- Connection settings
    database_type VARCHAR(50) NOT NULL DEFAULT 'GENERIC', -- POSTGRESQL, MYSQL, SQL_SERVER, ORACLE, GENERIC
    host VARCHAR(255) NOT NULL DEFAULT 'localhost',
    port INTEGER NOT NULL DEFAULT 9092,
    database_name VARCHAR(255) NOT NULL DEFAULT 'mockdb',

    -- Authentication
    username VARCHAR(255),
    password VARCHAR(255),
    allow_anonymous BOOLEAN NOT NULL DEFAULT false,

    -- Initialization SQL
    init_script TEXT, -- SQL script to run on startup (CREATE TABLE, INSERT data, CREATE FUNCTION, etc.)

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- SQL Query Mocks (one-to-many with sql_config)
CREATE TABLE sql_query_mock (
    id BIGSERIAL PRIMARY KEY,
    sql_config_id BIGINT NOT NULL REFERENCES sql_config(id) ON DELETE CASCADE,

    -- Query matching
    query_pattern TEXT NOT NULL,
    query_is_regex BOOLEAN NOT NULL DEFAULT false,
    query_type VARCHAR(50), -- SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, etc.

    -- Mock response
    mock_resultset TEXT, -- JSON array of rows
    affected_rows INTEGER DEFAULT 0, -- For INSERT/UPDATE/DELETE
    error_message TEXT, -- If you want to mock an error
    execution_delay_ms INTEGER DEFAULT 0,

    -- Metadata
    enabled BOOLEAN NOT NULL DEFAULT true,
    priority INTEGER NOT NULL DEFAULT 0, -- Lower number = higher priority

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_sql_config_endpoint ON sql_config(mock_endpoint_id);
CREATE INDEX idx_sql_query_mock_config ON sql_query_mock(sql_config_id);
CREATE INDEX idx_sql_query_mock_priority ON sql_query_mock(priority);

-- Sequences
CREATE SEQUENCE IF NOT EXISTS sql_config_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS sql_query_mock_SEQ START WITH 1 INCREMENT BY 50;
