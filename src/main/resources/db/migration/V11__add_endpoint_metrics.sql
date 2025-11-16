-- Add metrics/statistics fields to mock_endpoint
ALTER TABLE mock_endpoint ADD COLUMN total_requests BIGINT DEFAULT 0;
ALTER TABLE mock_endpoint ADD COLUMN matched_requests BIGINT DEFAULT 0;
ALTER TABLE mock_endpoint ADD COLUMN unmatched_requests BIGINT DEFAULT 0;
ALTER TABLE mock_endpoint ADD COLUMN last_request_at TIMESTAMP;
ALTER TABLE mock_endpoint ADD COLUMN average_response_time_ms INTEGER DEFAULT 0;
