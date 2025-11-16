-- NoSQL Mock Configuration
CREATE TABLE nosql_config (
    id BIGSERIAL PRIMARY KEY,
    mock_endpoint_id BIGINT NOT NULL UNIQUE REFERENCES mock_endpoint(id) ON DELETE CASCADE,

    -- Database type
    database_type VARCHAR(50) NOT NULL, -- MONGODB, REDIS, CASSANDRA

    -- Connection settings
    host VARCHAR(255) DEFAULT 'localhost',
    port INTEGER DEFAULT 27017,
    database_name VARCHAR(100) NOT NULL,
    collection_name VARCHAR(100), -- For MongoDB

    -- Authentication
    username VARCHAR(100),
    password VARCHAR(255),
    auth_database VARCHAR(100), -- For MongoDB authentication

    -- Redis specific
    redis_db_index INTEGER DEFAULT 0,

    -- MongoDB specific
    replica_set VARCHAR(255),

    -- Init data/script
    init_data TEXT, -- JSON data to insert on startup
    init_script TEXT, -- Commands to execute on startup

    -- Created/updated timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_nosql_config_endpoint ON nosql_config(mock_endpoint_id);
