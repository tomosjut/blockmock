package nl.blockmock.broker;

import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import nl.blockmock.domain.AmqpConfig;
import nl.blockmock.domain.AmqpExchangeType;
import org.jboss.logging.Logger;

import javax.jms.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * IBM MQ implementation using JMS API with IBM MQ specific connection factory
 */
public class IbmMqClient implements MessageBrokerClient {

    private static final Logger LOG = Logger.getLogger(IbmMqClient.class);

    private Connection connection;
    private Session session;
    private MessageProducer producer;
    private MessageConsumer consumer;
    private AmqpConfig config;
    private Destination destination;

    @Override
    public void connect(AmqpConfig config) throws Exception {
        this.config = config;

        MQConnectionFactory factory = new MQConnectionFactory();
        factory.setHostName(config.getHost());
        factory.setPort(config.getPort());
        factory.setTransportType(WMQConstants.WMQ_CM_CLIENT);

        // Queue Manager name (stored in virtualHost field for compatibility)
        if (config.getVirtualHost() != null && !config.getVirtualHost().equals("/")) {
            factory.setQueueManager(config.getVirtualHost());
        }

        // Channel name - can be passed via exchange name or use default
        String channel = "SYSTEM.DEF.SVRCONN"; // Default channel
        factory.setChannel(channel);

        if (config.getUsername() != null && config.getPassword() != null) {
            factory.setStringProperty(WMQConstants.USERID, config.getUsername());
            factory.setStringProperty(WMQConstants.PASSWORD, config.getPassword());
        }

        connection = factory.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Create destination based on configuration
        destination = createDestination(config);

        // Create producer
        producer = session.createProducer(destination);

        connection.start();

        LOG.info("IBM MQ connected to " + config.getHost() + ":" + config.getPort() +
                 " queue: " + getDestinationName());
    }

    private Destination createDestination(AmqpConfig config) throws JMSException {
        // IBM MQ primarily uses queues, topics are less common
        // Map exchange types to queue/topic semantics
        String destinationName = config.getQueueName() != null && !config.getQueueName().isEmpty()
            ? config.getQueueName()
            : config.getExchangeName();

        if (config.getExchangeType() == AmqpExchangeType.FANOUT) {
            // Use topic for pub-sub pattern
            return session.createTopic(destinationName);
        } else {
            // Use queue for point-to-point (most common in IBM MQ)
            return session.createQueue(destinationName);
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
        // Create selector for routing if needed
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
                    replyTo != null ? getReplyToString(replyTo) : null,
                    headers
                );

                messageHandler.handleMessage(message);
            } catch (Exception e) {
                LOG.error("Error handling IBM MQ message", e);
            }
        });

        LOG.info("Started consuming from IBM MQ: " + getDestinationName());
    }

    private String createMessageSelector(AmqpConfig config) {
        // For TOPIC exchange type, create selector based on routing key pattern
        if (config.getExchangeType() == AmqpExchangeType.TOPIC && config.getBindingPattern() != null) {
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

    private String getReplyToString(Destination destination) throws JMSException {
        if (destination instanceof Queue) {
            return "queue://" + ((Queue) destination).getQueueName();
        } else if (destination instanceof Topic) {
            return "topic://" + ((Topic) destination).getTopicName();
        }
        return destination.toString();
    }

    @Override
    public void publish(String message, String routingKey) throws Exception {
        TextMessage textMessage = session.createTextMessage(message);

        // Add routing key as property
        if (routingKey != null) {
            textMessage.setStringProperty("routingKey", routingKey);
        }

        // Add custom headers if configured
        if (config.getMockMessageHeaders() != null) {
            textMessage.setStringProperty("customHeaders", config.getMockMessageHeaders());
        }

        producer.send(textMessage);

        LOG.debug("Published message to IBM MQ: " + getDestinationName() + " with routingKey: " + routingKey);
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

        LOG.debug("Sent reply to IBM MQ: " + replyTo + " with correlationId: " + correlationId);
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
        LOG.info("IBM MQ connection closed");
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
