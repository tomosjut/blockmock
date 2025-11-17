package nl.blockmock.broker;

import com.rabbitmq.client.*;
import nl.blockmock.domain.AmqpConfig;
import nl.blockmock.domain.AmqpOperation;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ implementation of MessageBrokerClient using AMQP 0.9.1 protocol
 */
public class RabbitMqClient implements MessageBrokerClient {

    private static final Logger LOG = Logger.getLogger(RabbitMqClient.class);

    private Connection connection;
    private Channel channel;
    private AmqpConfig config;

    @Override
    public void connect(AmqpConfig config) throws IOException, TimeoutException {
        this.config = config;

        // Create connection
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.getHost());
        factory.setPort(config.getPort());
        factory.setVirtualHost(config.getVirtualHost());

        if (config.getUsername() != null && config.getPassword() != null) {
            factory.setUsername(config.getUsername());
            factory.setPassword(config.getPassword());
        }

        connection = factory.newConnection();
        channel = connection.createChannel();

        // Declare exchange
        channel.exchangeDeclare(
            config.getExchangeName(),
            config.getExchangeType().name().toLowerCase(),
            config.getExchangeDurable(),
            config.getExchangeAutoDelete(),
            null
        );

        // Declare queue if specified
        if (config.getQueueName() != null && !config.getQueueName().isEmpty()) {
            channel.queueDeclare(
                config.getQueueName(),
                config.getQueueDurable(),
                config.getQueueExclusive(),
                config.getQueueAutoDelete(),
                null
            );

            // Bind queue to exchange
            String bindingKey = config.getBindingPattern() != null ?
                config.getBindingPattern() : config.getRoutingKey();
            if (bindingKey != null) {
                channel.queueBind(config.getQueueName(), config.getExchangeName(), bindingKey);
            }
        }

        LOG.info("RabbitMQ connected to " + config.getHost() + ":" + config.getPort() +
                 " exchange: " + config.getExchangeName());
    }

    @Override
    public void startConsuming(MessageHandler messageHandler) throws IOException {
        if (config.getQueueName() == null || config.getQueueName().isEmpty()) {
            throw new IllegalStateException("Queue name is required for consuming messages");
        }

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String body = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String routingKey = delivery.getEnvelope().getRoutingKey();
            AMQP.BasicProperties props = delivery.getProperties();

            Map<String, Object> headers = new HashMap<>();
            if (props.getHeaders() != null) {
                headers.putAll(props.getHeaders());
            }

            IncomingMessage message = new IncomingMessage(
                body,
                routingKey,
                props.getCorrelationId(),
                props.getReplyTo(),
                headers
            );

            try {
                messageHandler.handleMessage(message);
            } catch (Exception e) {
                LOG.error("Error handling message", e);
            }
        };

        channel.basicConsume(config.getQueueName(), true, deliverCallback, consumerTag -> {});
        LOG.info("Started consuming from queue: " + config.getQueueName());
    }

    @Override
    public void publish(String message, String routingKey) throws IOException {
        AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();

        if (config.getMockMessageHeaders() != null) {
            propsBuilder.headers(new HashMap<>());
        }

        channel.basicPublish(
            config.getExchangeName(),
            routingKey != null ? routingKey : "",
            propsBuilder.build(),
            message.getBytes(StandardCharsets.UTF_8)
        );

        LOG.debug("Published message to exchange: " + config.getExchangeName() +
                  " with routing key: " + routingKey);
    }

    @Override
    public void sendReply(String replyTo, String correlationId, String message) throws Exception {
        if (replyTo == null || replyTo.isEmpty()) {
            LOG.warn("Cannot send reply: replyTo address is empty");
            return;
        }

        AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                .correlationId(correlationId)
                .build();

        channel.basicPublish(
            "",
            replyTo,
            replyProps,
            message.getBytes(StandardCharsets.UTF_8)
        );

        LOG.debug("Sent reply to: " + replyTo + " with correlationId: " + correlationId);
    }

    @Override
    public void close() throws IOException, TimeoutException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
        LOG.info("RabbitMQ connection closed");
    }

    @Override
    public boolean isConnected() {
        return connection != null && connection.isOpen() &&
               channel != null && channel.isOpen();
    }
}
