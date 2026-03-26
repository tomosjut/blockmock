package nl.blockmock.service;

import io.vertx.amqp.AmqpMessage;
import io.vertx.amqp.AmqpMessageBuilder;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.AmqpMockEndpoint;
import nl.blockmock.domain.MockResponse;
import nl.blockmock.domain.ProtocolType;
import nl.blockmock.domain.RequestLog;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes incoming AMQP messages: matches against enabled endpoints, logs the interaction,
 * updates metrics, and sends a reply for REQUEST_REPLY endpoints.
 * DB operations run on a worker thread via {@code vertx.executeBlocking} to avoid blocking the event loop.
 */
@ApplicationScoped
public class AmqpMockService {

    private static final Logger LOG = Logger.getLogger(AmqpMockService.class);

    @Inject
    Vertx vertx;

    @Inject
    AmqpMockService self;

    @Inject
    AmqpConnectionService amqpConnectionService;

    @Inject
    RequestLogService requestLogService;

    /**
     * Entry point — called from the Vert.x event loop.
     * Dispatches processing to a worker thread to allow blocking DB operations.
     */
    public void onMessage(String address, AmqpMessage message) {
        // Capture AMQP data immediately (message may be recycled after handler returns)
        String body           = message.bodyAsString();
        String messageId      = message.id();
        String correlationId  = message.correlationId();
        String replyTo        = message.replyTo();
        String subject        = message.subject();
        Map<String, String> properties = extractProperties(message);

        vertx.executeBlocking(() -> {
            self.processMessage(address, body, messageId, correlationId, replyTo, subject, properties);
            return null;
        });
    }

    @Transactional
    public void processMessage(String address, String body,
                               String messageId, String correlationId,
                               String replyTo, String subject,
                               Map<String, String> properties) {

        // 1. Find matching enabled endpoint by address
        AmqpMockEndpoint endpoint = AmqpMockEndpoint
                .find("amqpAddress = ?1 and enabled = true", address)
                .firstResult();

        // 2. Build request log
        RequestLog log = new RequestLog();
        log.setProtocol(ProtocolType.AMQP);
        log.setAmqpAddress(address);
        log.setAmqpSubject(subject);
        log.setAmqpMessageId(messageId);
        log.setAmqpCorrelationId(correlationId);
        log.setAmqpReplyTo(replyTo);
        log.setAmqpProperties(properties);
        log.setRequestBody(body);

        if (endpoint == null) {
            LOG.warnf("No AMQP endpoint found for address: %s", address);
            log.setMatched(false);
            requestLogService.log(log);
            return;
        }

        // 3. Find matching response
        MockResponse matchedResponse = findMatchingResponse(endpoint, body, properties);

        log.setMockEndpoint(endpoint);
        log.setMockResponse(matchedResponse);
        log.setMatched(true);
        requestLogService.log(log);

        // 4. Update metrics
        updateMetrics(endpoint);

        // 5. REQUEST_REPLY: publish reply on the reply-to address
        if ("REQUEST_REPLY".equals(endpoint.getAmqpPattern()) && replyTo != null) {
            String replyBody = matchedResponse != null ? matchedResponse.getResponseBody() : "";
            Map<String, String> replyProps = matchedResponse != null ? matchedResponse.getResponseHeaders() : null;
            try {
                amqpConnectionService.publish(replyTo, replyBody, replyProps, endpoint.getAmqpRoutingType());
                LOG.debugf("AMQP reply sent to: %s", replyTo);
            } catch (Exception e) {
                LOG.warnf("Failed to send AMQP reply to '%s': %s", replyTo, e.getMessage());
            }
        }
    }

    private MockResponse findMatchingResponse(AmqpMockEndpoint endpoint, String body,
                                              Map<String, String> properties) {
        if (endpoint.getForcedResponse() != null) {
            return endpoint.getForcedResponse();
        }

        List<MockResponse> responses = endpoint.getResponses().stream()
                .sorted((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))
                .toList();

        for (MockResponse response : responses) {
            if (matchesResponse(response, body, properties)) {
                return response;
            }
        }
        return null;
    }

    private boolean matchesResponse(MockResponse response, String body, Map<String, String> properties) {
        // Match on application properties (equivalent to HTTP headers)
        if (response.getMatchHeaders() != null && !response.getMatchHeaders().isEmpty()) {
            if (properties == null) return false;
            for (Map.Entry<String, String> entry : response.getMatchHeaders().entrySet()) {
                if (!entry.getValue().equals(properties.get(entry.getKey()))) {
                    return false;
                }
            }
        }

        // Match on message body
        if (response.getMatchBody() != null && !response.getMatchBody().isEmpty()) {
            if (body == null) return false;
            if (!body.contains(response.getMatchBody())) {
                return false;
            }
        }

        return true;
    }

    private void updateMetrics(AmqpMockEndpoint endpoint) {
        AmqpMockEndpoint managed = AmqpMockEndpoint.findById(endpoint.id);
        if (managed == null) return;
        managed.setTotalRequests(managed.getTotalRequests() + 1);
        managed.setMatchedRequests(managed.getMatchedRequests() + 1);
        managed.setLastRequestAt(LocalDateTime.now());
    }

    private Map<String, String> extractProperties(AmqpMessage message) {
        JsonObject props = message.applicationProperties();
        if (props == null || props.isEmpty()) return null;
        Map<String, String> result = new HashMap<>();
        props.forEach(entry -> result.put(entry.getKey(), String.valueOf(entry.getValue())));
        return result;
    }
}
