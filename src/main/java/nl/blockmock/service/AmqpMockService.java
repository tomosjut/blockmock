package nl.blockmock.service;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import nl.blockmock.broker.MessageBrokerClient;
import nl.blockmock.broker.MessageBrokerClientFactory;
import nl.blockmock.domain.*;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class AmqpMockService {

    private static final Logger LOG = Logger.getLogger(AmqpMockService.class);

    @Inject
    MockEndpointService mockEndpointService;

    @Inject
    RequestLogService requestLogService;

    @Inject
    MessageBrokerClientFactory brokerClientFactory;

    private final Map<Long, MessageBrokerClient> brokerClients = new ConcurrentHashMap<>();

    void onStart(@Observes StartupEvent ev) {
        LOG.info("AMQP Mock Service started - ready to configure mock endpoints");
        // Initialize AMQP mocks when endpoints are created
        initializeAmqpMocks();
    }

    void onStop(@Observes ShutdownEvent ev) {
        // Clean up all broker connections
        brokerClients.values().forEach(client -> {
            try {
                if (client.isConnected()) {
                    client.close();
                }
            } catch (Exception e) {
                LOG.warn("Error closing message broker connection", e);
            }
        });

        LOG.info("Message Broker Mock Service stopped");
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

    public void startAmqpMock(MockEndpoint endpoint) throws Exception {
        if (endpoint.getAmqpConfig() == null) {
            LOG.warn("No message broker config found for endpoint: " + endpoint.getName());
            return;
        }

        AmqpConfig config = endpoint.getAmqpConfig();

        // Create appropriate broker client based on configuration
        MessageBrokerClient client = brokerClientFactory.createClient(config);

        // Connect to broker
        client.connect(config);

        // Set up consumer if operation is CONSUME or BOTH
        if (config.getOperation() == AmqpOperation.CONSUME || config.getOperation() == AmqpOperation.BOTH) {
            client.startConsuming(message -> {
                LOG.info("Message broker received message on " + config.getExchangeName() +
                        " with routing key: " + message.routingKey());

                // Log the request
                logBrokerRequest(config, message.routingKey(), message.body(), message.headers(), true);

                // Auto-reply if configured
                if (config.getAutoReply() && config.getMockMessageContent() != null) {
                    try {
                        if (config.getReplyDelayMs() > 0) {
                            Thread.sleep(config.getReplyDelayMs());
                        }

                        client.sendReply(
                            message.replyTo(),
                            message.correlationId(),
                            config.getMockMessageContent()
                        );

                        LOG.info("Sent auto-reply to: " + message.replyTo());
                    } catch (Exception e) {
                        LOG.error("Error sending auto-reply", e);
                    }
                }
            });
        }

        brokerClients.put(endpoint.id, client);

        LOG.info("Started message broker mock (" + config.getBrokerType() + ") for endpoint: " +
                 endpoint.getName() + " on exchange: " + config.getExchangeName());
    }

    public void publishMockMessage(Long endpointId) throws Exception {
        MockEndpoint endpoint = mockEndpointService.findById(endpointId).orElse(null);
        if (endpoint == null || endpoint.getAmqpConfig() == null) {
            throw new IllegalArgumentException("Invalid endpoint or missing message broker config");
        }

        AmqpConfig config = endpoint.getAmqpConfig();
        MessageBrokerClient client = brokerClients.get(endpointId);

        if (client == null || !client.isConnected()) {
            throw new IllegalStateException("Message broker client not available for endpoint: " + endpoint.getName());
        }

        if (config.getMockMessageContent() == null) {
            throw new IllegalArgumentException("No mock message content configured");
        }

        client.publish(
            config.getMockMessageContent(),
            config.getRoutingKey()
        );

        LOG.info("Published mock message to exchange: " + config.getExchangeName());

        // Log the publish
        logBrokerRequest(config, config.getRoutingKey(), config.getMockMessageContent(), null, true);
    }

    public void stopAmqpMock(Long endpointId) {
        MessageBrokerClient client = brokerClients.remove(endpointId);
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                LOG.warn("Error closing message broker client for endpoint: " + endpointId, e);
            }
        }
    }

    private void logBrokerRequest(AmqpConfig config, String routingKey, String message,
                                   Map<String, Object> headers, boolean matched) {
        RequestLog log = new RequestLog();
        log.setProtocol(ProtocolType.AMQP);
        log.setRequestMethod(config.getOperation().name());
        log.setRequestPath(config.getExchangeName() + "/" + (routingKey != null ? routingKey : ""));
        log.setRequestBody(message);
        log.setMatched(matched);

        if (headers != null && !headers.isEmpty()) {
            Map<String, String> stringHeaders = new HashMap<>();
            headers.forEach((key, value) ->
                stringHeaders.put(key, value != null ? value.toString() : null)
            );
            log.setRequestHeaders(stringHeaders);
        }

        requestLogService.log(log);
    }
}
