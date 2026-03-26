package nl.blockmock.service;

import io.vertx.amqp.AmqpClient;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.amqp.AmqpConnection;
import io.vertx.amqp.AmqpMessage;
import io.vertx.amqp.AmqpMessageBuilder;
import io.vertx.amqp.AmqpReceiver;
import io.vertx.amqp.AmqpSender;
import java.util.UUID;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import nl.blockmock.domain.AmqpMockEndpoint;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the AMQP client connection to the external broker (Artemis).
 * Handles consumer lifecycle (start/stop per endpoint) and outbound message publishing.
 * If the broker is unavailable at startup, a warning is logged and AMQP endpoints stay inactive.
 */
@ApplicationScoped
public class AmqpConnectionService {

    private static final Logger LOG = Logger.getLogger(AmqpConnectionService.class);

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "blockmock.amqp.host")
    String host;

    @ConfigProperty(name = "blockmock.amqp.port")
    int port;

    @ConfigProperty(name = "blockmock.amqp.username")
    String username;

    @ConfigProperty(name = "blockmock.amqp.password")
    String password;

    @Inject
    AmqpMockService amqpMockService;

    private AmqpClient client;
    private volatile AmqpConnection connection;
    private final Map<Long, AmqpReceiver> receivers = new ConcurrentHashMap<>();

    @PostConstruct
    void connect() {
        AmqpClientOptions options = new AmqpClientOptions()
                .setHost(host)
                .setPort(port)
                .setUsername(username)
                .setPassword(password);

        client = AmqpClient.create(vertx, options);
        client.connect(ar -> {
            if (ar.succeeded()) {
                connection = ar.result();
                LOG.infof("Connected to AMQP broker at %s:%d", host, port);
            } else {
                LOG.warnf("Could not connect to AMQP broker at %s:%d — %s. AMQP endpoints will be inactive.",
                        host, port, ar.cause().getMessage());
            }
        });
    }

    @PreDestroy
    void disconnect() {
        receivers.values().forEach(r -> r.close(v -> {}));
        receivers.clear();
        if (connection != null) {
            connection.close(v -> {});
        }
        if (client != null) {
            client.close(v -> {});
        }
        connection = null;
    }

    public boolean isConnected() {
        return connection != null;
    }

    public void startConsumer(AmqpMockEndpoint endpoint) {
        if (connection == null) {
            LOG.warnf("Cannot start AMQP consumer for '%s': not connected to broker", endpoint.getAmqpAddress());
            return;
        }
        // Close any existing receiver for this endpoint before creating a new one.
        // Without this, restarting a block adds a second receiver on the same address
        // while the first keeps running, causing duplicate message delivery.
        AmqpReceiver existing = receivers.remove(endpoint.id);
        if (existing != null) {
            existing.close(v -> LOG.infof("Closed stale AMQP receiver for address: %s", endpoint.getAmqpAddress()));
        }
        String address = endpoint.getAmqpAddress();
        // Note: amqpRoutingType (ANYCAST/MULTICAST) is persisted and shown in the UI, but we intentionally
        // do not pass it as an AMQP capability on the receiver link. Artemis capabilities create implicit
        // queues that conflict when the address already exists with a different routing type. Routing type
        // should be configured at the broker level (auto-create-addresses config in broker.xml).
        connection.createReceiver(address, ar -> {
            if (ar.succeeded()) {
                receivers.put(endpoint.id, ar.result());
                ar.result().handler(msg -> amqpMockService.onMessage(address, msg));
                LOG.infof("AMQP consumer started for address: %s (routing: %s)", address, endpoint.getAmqpRoutingType());
            } else {
                LOG.errorf("Failed to create AMQP receiver for '%s': %s", address, ar.cause().getMessage());
            }
        });
    }

    public void stopConsumer(AmqpMockEndpoint endpoint) {
        AmqpReceiver receiver = receivers.remove(endpoint.id);
        if (receiver != null) {
            receiver.close(ar -> LOG.infof("AMQP consumer stopped for address: %s", endpoint.getAmqpAddress()));
        }
    }

    /**
     * Publishes a message to the given address.
     * <p>
     * Note: Artemis only honours routing-type capabilities on <em>receiver</em> attach frames, not sender frames.
     * The routing type of the address is already established by the receiver side (see {@link #startConsumer}).
     * The {@code routingType} parameter is accepted for API consistency but not applied to the sender link.
     *
     * @return the generated message ID so callers can correlate the send with a log entry
     */
    public String publish(String address, String body, Map<String, String> properties, String routingType) {
        if (connection == null) {
            throw new IllegalStateException("Not connected to AMQP broker");
        }
        String messageId = UUID.randomUUID().toString();
        connection.createSender(address, ar -> {
            if (ar.succeeded()) {
                AmqpSender sender = ar.result();
                AmqpMessageBuilder builder = AmqpMessage.create()
                        .id(messageId)
                        .withBody(body != null ? body : "");
                if (properties != null && !properties.isEmpty()) {
                    JsonObject props = new JsonObject();
                    properties.forEach(props::put);
                    builder.applicationProperties(props);
                }
                sender.send(builder.build());
                sender.close(v -> {});
                LOG.infof("AMQP message published to: %s (id: %s, routing: %s)", address, messageId, routingType);
            } else {
                LOG.errorf("Failed to publish AMQP message to '%s': %s", address, ar.cause().getMessage());
            }
        });
        return messageId;
    }

}
