-- Add broker_type column to amqp_config table to support multiple message brokers
-- Supports: RABBITMQ, ARTEMIS, IBM_MQ

ALTER TABLE amqp_config
ADD COLUMN broker_type VARCHAR(50) NOT NULL DEFAULT 'RABBITMQ';

-- Add comment for clarity
COMMENT ON COLUMN amqp_config.broker_type IS 'Type of message broker: RABBITMQ, ARTEMIS, or IBM_MQ';
