package nl.blockmock.broker;

import jakarta.enterprise.context.ApplicationScoped;
import nl.blockmock.domain.AmqpConfig;
import nl.blockmock.domain.MessageBrokerType;
import org.jboss.logging.Logger;

/**
 * Factory for creating the appropriate MessageBrokerClient based on broker type
 */
@ApplicationScoped
public class MessageBrokerClientFactory {

    private static final Logger LOG = Logger.getLogger(MessageBrokerClientFactory.class);

    /**
     * Create a message broker client based on the configuration
     */
    public MessageBrokerClient createClient(AmqpConfig config) {
        MessageBrokerType brokerType = config.getBrokerType();
        if (brokerType == null) {
            // Default to RabbitMQ for backward compatibility
            brokerType = MessageBrokerType.RABBITMQ;
            LOG.warn("No broker type specified, defaulting to RabbitMQ");
        }

        MessageBrokerClient client = switch (brokerType) {
            case RABBITMQ -> {
                LOG.info("Creating RabbitMQ client");
                yield new RabbitMqClient();
            }
            case ARTEMIS -> {
                LOG.info("Creating Apache Artemis client");
                yield new ArtemisClient();
            }
            case IBM_MQ -> {
                LOG.info("Creating IBM MQ client");
                yield new IbmMqClient();
            }
        };

        return client;
    }
}
