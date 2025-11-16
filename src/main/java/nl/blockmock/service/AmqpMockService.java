package nl.blockmock.service;

import com.rabbitmq.client.*;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import nl.blockmock.domain.*;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class AmqpMockService {

    private static final Logger LOG = Logger.getLogger(AmqpMockService.class);

    @Inject
    MockEndpointService mockEndpointService;

    @Inject
    RequestLogService requestLogService;

    private final Map<Long, Connection> connections = new ConcurrentHashMap<>();
    private final Map<Long, Channel> channels = new ConcurrentHashMap<>();

    void onStart(@Observes StartupEvent ev) {
        LOG.info("AMQP Mock Service started - ready to configure mock endpoints");
        // Initialize AMQP mocks when endpoints are created
        initializeAmqpMocks();
    }

    void onStop(@Observes ShutdownEvent ev) {
        // Clean up all connections and channels
        channels.values().forEach(channel -> {
            try {
                if (channel.isOpen()) {
                    channel.close();
                }
            } catch (Exception e) {
                LOG.warn("Error closing AMQP channel", e);
            }
        });

        connections.values().forEach(connection -> {
            try {
                if (connection.isOpen()) {
                    connection.close();
                }
            } catch (Exception e) {
                LOG.warn("Error closing AMQP connection", e);
            }
        });

        LOG.info("AMQP Mock Service stopped");
    }

    public void initializeAmqpMocks() {
        List<MockEndpoint> amqpEndpoints = mockEndpointService.findEnabledByProtocol(ProtocolType.AMQP);

        for (MockEndpoint endpoint : amqpEndpoints) {
            try {
                startAmqpMock(endpoint);
            } catch (Exception e) {
                LOG.error("Failed to start AMQP mock for endpoint: " + endpoint.getName(), e);
            }
        }
    }

    public void startAmqpMock(MockEndpoint endpoint) throws IOException, TimeoutException {
        if (endpoint.getAmqpConfig() == null) {
            LOG.warn("No AMQP config found for endpoint: " + endpoint.getName());
            return;
        }

        AmqpConfig config = endpoint.getAmqpConfig();

        // Create connection
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.getHost());
        factory.setPort(config.getPort());
        factory.setVirtualHost(config.getVirtualHost());

        if (config.getUsername() != null && config.getPassword() != null) {
            factory.setUsername(config.getUsername());
            factory.setPassword(config.getPassword());
        }

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

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
            String bindingKey = config.getBindingPattern() != null ? config.getBindingPattern() : config.getRoutingKey();
            if (bindingKey != null) {
                channel.queueBind(config.getQueueName(), config.getExchangeName(), bindingKey);
            }

            // Set up consumer if operation is CONSUME or BOTH
            if (config.getOperation() == AmqpOperation.CONSUME || config.getOperation() == AmqpOperation.BOTH) {
                setupConsumer(channel, config, endpoint);
            }
        }

        connections.put(endpoint.id, connection);
        channels.put(endpoint.id, channel);

        LOG.info("Started AMQP mock for endpoint: " + endpoint.getName() +
                 " on exchange: " + config.getExchangeName());
    }

    private void setupConsumer(Channel channel, AmqpConfig config, MockEndpoint endpoint) throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String routingKey = delivery.getEnvelope().getRoutingKey();

            LOG.info("AMQP Mock received message on queue " + config.getQueueName() +
                    " with routing key: " + routingKey);

            // Log the request
            logAmqpRequest(config, routingKey, message, delivery.getProperties(), true);

            // Auto-reply if configured
            if (config.getAutoReply() && config.getMockMessageContent() != null) {
                try {
                    if (config.getReplyDelayMs() > 0) {
                        Thread.sleep(config.getReplyDelayMs());
                    }

                    AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
                            .correlationId(delivery.getProperties().getCorrelationId())
                            .build();

                    channel.basicPublish(
                        "",
                        delivery.getProperties().getReplyTo(),
                        replyProps,
                        config.getMockMessageContent().getBytes(StandardCharsets.UTF_8)
                    );

                    LOG.info("Sent auto-reply to: " + delivery.getProperties().getReplyTo());
                } catch (Exception e) {
                    LOG.error("Error sending auto-reply", e);
                }
            }
        };

        channel.basicConsume(config.getQueueName(), true, deliverCallback, consumerTag -> {});
    }

    public void publishMockMessage(Long endpointId) throws IOException {
        MockEndpoint endpoint = mockEndpointService.findById(endpointId).orElse(null);
        if (endpoint == null || endpoint.getAmqpConfig() == null) {
            throw new IllegalArgumentException("Invalid endpoint or missing AMQP config");
        }

        AmqpConfig config = endpoint.getAmqpConfig();
        Channel channel = channels.get(endpointId);

        if (channel == null || !channel.isOpen()) {
            throw new IllegalStateException("AMQP channel not available for endpoint: " + endpoint.getName());
        }

        if (config.getMockMessageContent() == null) {
            throw new IllegalArgumentException("No mock message content configured");
        }

        // Parse headers if provided
        AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
        if (config.getMockMessageHeaders() != null) {
            // Simple JSON-like header parsing (in real scenario, use proper JSON parser)
            propsBuilder.headers(new HashMap<>());
        }

        channel.basicPublish(
            config.getExchangeName(),
            config.getRoutingKey() != null ? config.getRoutingKey() : "",
            propsBuilder.build(),
            config.getMockMessageContent().getBytes(StandardCharsets.UTF_8)
        );

        LOG.info("Published mock message to exchange: " + config.getExchangeName());

        // Log the publish
        logAmqpRequest(config, config.getRoutingKey(), config.getMockMessageContent(), null, true);
    }

    public void stopAmqpMock(Long endpointId) {
        Channel channel = channels.remove(endpointId);
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception e) {
                LOG.warn("Error closing channel for endpoint: " + endpointId, e);
            }
        }

        Connection connection = connections.remove(endpointId);
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                LOG.warn("Error closing connection for endpoint: " + endpointId, e);
            }
        }
    }

    private void logAmqpRequest(AmqpConfig config, String routingKey, String message,
                                 AMQP.BasicProperties properties, boolean matched) {
        RequestLog log = new RequestLog();
        log.setProtocol(ProtocolType.AMQP);
        log.setRequestMethod(config.getOperation().name());
        log.setRequestPath(config.getExchangeName() + "/" + (routingKey != null ? routingKey : ""));
        log.setRequestBody(message);
        log.setMatched(matched);

        if (properties != null && properties.getHeaders() != null) {
            Map<String, String> headers = new HashMap<>();
            properties.getHeaders().forEach((key, value) ->
                headers.put(key, value != null ? value.toString() : null)
            );
            log.setRequestHeaders(headers);
        }

        requestLogService.log(log);
    }
}
