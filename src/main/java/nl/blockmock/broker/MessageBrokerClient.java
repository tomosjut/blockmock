package nl.blockmock.broker;

import nl.blockmock.domain.AmqpConfig;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Abstraction layer for different message broker implementations.
 * Supports RabbitMQ, Apache Artemis, and IBM MQ.
 */
public interface MessageBrokerClient {

    /**
     * Initialize connection to the broker and set up exchange/queue/topic
     */
    void connect(AmqpConfig config) throws Exception;

    /**
     * Start consuming messages from the broker
     */
    void startConsuming(MessageHandler messageHandler) throws Exception;

    /**
     * Publish a message to the broker
     */
    void publish(String message, String routingKey) throws Exception;

    /**
     * Send a reply message (for RPC patterns)
     */
    void sendReply(String replyTo, String correlationId, String message) throws Exception;

    /**
     * Close connection and release resources
     */
    void close() throws Exception;

    /**
     * Check if the connection is still active
     */
    boolean isConnected();

    /**
     * Callback interface for handling incoming messages
     */
    interface MessageHandler {
        void handleMessage(IncomingMessage message) throws Exception;
    }

    /**
     * Represents an incoming message with all metadata
     */
    record IncomingMessage(
        String body,
        String routingKey,
        String correlationId,
        String replyTo,
        java.util.Map<String, Object> headers
    ) {}
}
