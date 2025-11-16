-- Create blocks table
CREATE TABLE block (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    color VARCHAR(7) DEFAULT '#667eea',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_block_name ON block(name);

-- Create junction table for many-to-many relationship
CREATE TABLE block_endpoint (
    block_id BIGINT NOT NULL REFERENCES block(id) ON DELETE CASCADE,
    mock_endpoint_id BIGINT NOT NULL REFERENCES mock_endpoint(id) ON DELETE CASCADE,
    added_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (block_id, mock_endpoint_id)
);

CREATE INDEX idx_block_endpoint_block ON block_endpoint(block_id);
CREATE INDEX idx_block_endpoint_endpoint ON block_endpoint(mock_endpoint_id);

-- Add update trigger for blocks
CREATE TRIGGER update_block_updated_at BEFORE UPDATE ON block
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
