package nl.blockmock.broker;

import nl.blockmock.domain.AmqpConfig;
import nl.blockmock.domain.AmqpExchangeType;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.jboss.logging.Logger;

import javax.jms.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Apache ActiveMQ Artemis implementation using JMS 2.0 API
 */
public class ArtemisClient implements MessageBrokerClient {

    private static final Logger LOG = Logger.getLogger(ArtemisClient.class);

    private Connection connection;
    private Session session;
    private MessageProducer producer;
    private MessageConsumer consumer;
    private AmqpConfig config;
    private Destination destination;

    @Override
    public void connect(AmqpConfig config) throws Exception {
        this.config = config;

        // Artemis broker URL format: tcp://host:port
        String brokerUrl = "tcp://" + config.getHost() + ":" + config.getPort();

        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

        if (config.getUsername() != null && config.getPassword() != null) {
            factory.setUser(config.getUsername());
            factory.setPassword(config.getPassword());
        }

        connection = factory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Map exchange types to JMS destination types
        destination = createDestination(config);

        // Create producer
        producer = session.createProducer(destination);

        connection.start();

        LOG.info("Artemis connected to " + brokerUrl + " destination: " + getDestinationName());
    }

    private Destination createDestination(AmqpConfig config) throws JMSException {
        String destinationName = config.getExchangeName();

        // Map AMQP exchange types to Artemis address/queue semantics
        // FANOUT -> Topic (all consumers get the message)
        // DIRECT/TOPIC -> Queue with routing (point-to-point or selective)
        switch (config.getExchangeType()) {
            case FANOUT:
                // Use topic for pub-sub pattern
                return session.createTopic(destinationName);
            case DIRECT:
            case TOPIC:
            case HEADERS:
            default:
                // Use queue for point-to-point with optional selectors
                if (config.getQueueName() != null && !config.getQueueName().isEmpty()) {
                    return session.createQueue(config.getQueueName());
                } else {
                    return session.createQueue(destinationName);
                }
        }
    }

    private String getDestinationName() throws JMSException {
        if (destination instanceof Queue) {
            return ((Queue) destination).getQueueName();
        } else if (destination instanceof Topic) {
            return ((Topic) destination).getTopicName();
        }
        return "unknown";
    }

    @Override
    public void startConsuming(MessageHandler messageHandler) throws Exception {
        // Create selector for topic-like routing if needed
        String selector = createMessageSelector(config);

        if (selector != null) {
            consumer = session.createConsumer(destination, selector);
        } else {
            consumer = session.createConsumer(destination);
        }

        consumer.setMessageListener(jmsMessage -> {
            try {
                String body = extractMessageBody(jmsMessage);
                String routingKey = jmsMessage.getStringProperty("routingKey");
                String correlationId = jmsMessage.getJMSCorrelationID();
                Destination replyTo = jmsMessage.getJMSReplyTo();

                Map<String, Object> headers = extractHeaders(jmsMessage);

                IncomingMessage message = new IncomingMessage(
                    body,
                    routingKey,
                    correlationId,
                    replyTo != null ? replyTo.toString() : null,
                    headers
                );

                messageHandler.handleMessage(message);
            } catch (Exception e) {
                LOG.error("Error handling JMS message", e);
            }
        });

        LOG.info("Started consuming from: " + getDestinationName());
    }

    private String createMessageSelector(AmqpConfig config) {
        // For TOPIC exchange type, create selector based on routing key pattern
        if (config.getExchangeType() == AmqpExchangeType.TOPIC && config.getBindingPattern() != null) {
            // Convert AMQP wildcard pattern to JMS selector
            // AMQP: # (multi-word), * (single word)
            // JMS: uses SQL-like LIKE syntax
            String pattern = config.getBindingPattern()
                .replace("#", "%")
                .replace("*", "%");
            return "routingKey LIKE '" + pattern + "'";
        }
        return null;
    }

    private String extractMessageBody(Message jmsMessage) throws JMSException {
        if (jmsMessage instanceof TextMessage) {
            return ((TextMessage) jmsMessage).getText();
        } else if (jmsMessage instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) jmsMessage;
            byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
            bytesMessage.readBytes(bytes);
            return new String(bytes);
        } else {
            return jmsMessage.getBody(String.class);
        }
    }

    private Map<String, Object> extractHeaders(Message jmsMessage) throws JMSException {
        Map<String, Object> headers = new HashMap<>();
        Enumeration<?> propertyNames = jmsMessage.getPropertyNames();

        while (propertyNames.hasMoreElements()) {
            String name = (String) propertyNames.nextElement();
            headers.put(name, jmsMessage.getObjectProperty(name));
        }

        return headers;
    }

    @Override
    public void publish(String message, String routingKey) throws Exception {
        TextMessage textMessage = session.createTextMessage(message);

        // Add routing key as property for topic-based routing
        if (routingKey != null) {
            textMessage.setStringProperty("routingKey", routingKey);
        }

        // Add custom headers if configured
        if (config.getMockMessageHeaders() != null) {
            // Parse and add headers (simplified - in production use JSON parser)
            textMessage.setStringProperty("customHeaders", config.getMockMessageHeaders());
        }

        producer.send(textMessage);

        LOG.debug("Published message to: " + getDestinationName() + " with routingKey: " + routingKey);
    }

    @Override
    public void sendReply(String replyTo, String correlationId, String message) throws Exception {
        if (replyTo == null || replyTo.isEmpty()) {
            LOG.warn("Cannot send reply: replyTo is empty");
            return;
        }

        // Parse replyTo to create destination
        Destination replyDestination;
        if (replyTo.startsWith("topic://")) {
            replyDestination = session.createTopic(replyTo.substring(8));
        } else if (replyTo.startsWith("queue://")) {
            replyDestination = session.createQueue(replyTo.substring(8));
        } else {
            replyDestination = session.createQueue(replyTo);
        }

        TextMessage replyMessage = session.createTextMessage(message);
        replyMessage.setJMSCorrelationID(correlationId);

        MessageProducer replyProducer = session.createProducer(replyDestination);
        replyProducer.send(replyMessage);
        replyProducer.close();

        LOG.debug("Sent reply to: " + replyTo + " with correlationId: " + correlationId);
    }

    @Override
    public void close() throws Exception {
        if (consumer != null) {
            consumer.close();
        }
        if (producer != null) {
            producer.close();
        }
        if (session != null) {
            session.close();
        }
        if (connection != null) {
            connection.close();
        }
        LOG.info("Artemis connection closed");
    }

    @Override
    public boolean isConnected() {
        try {
            return connection != null && session != null;
        } catch (Exception e) {
            return false;
        }
    }
}
