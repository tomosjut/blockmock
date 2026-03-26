package nl.blockmock.service;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.AmqpTriggerConfig;
import nl.blockmock.domain.CronTriggerConfig;
import nl.blockmock.domain.HttpTriggerConfig;
import nl.blockmock.domain.TestScenario;
import nl.blockmock.domain.TriggerConfig;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CRUD, scheduling, and execution of triggers. Cron triggers are registered with the
 * Quarkus scheduler on create/update and unregistered on delete or disable.
 * HTTP, CRON, and AMQP trigger types are each executed differently via {@link #fire}.
 */
@ApplicationScoped
public class TriggerService {

    private static final Logger LOG = Logger.getLogger(TriggerService.class);

    @Inject
    Scheduler scheduler;

    @Inject
    AmqpConnectionService amqpConnectionService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    void onStart(@Observes StartupEvent event) {
        scheduleAllCronTriggers();
    }

    private void scheduleAllCronTriggers() {
        List<CronTriggerConfig> triggers = CronTriggerConfig.list("enabled", true);
        for (CronTriggerConfig trigger : triggers) {
            scheduleCronTrigger(trigger);
        }
    }

    private void scheduleCronTrigger(CronTriggerConfig trigger) {
        String jobId = jobId(trigger.id);
        try {
            scheduler.newJob(jobId)
                    .setCron(trigger.getCronExpression())
                    .setTask(ctx -> {
                        LOG.infof("Cron trigger fired: %s", trigger.getName());
                        fire(trigger.id);
                    })
                    .schedule();
        } catch (Exception e) {
            LOG.warnf("Could not schedule cron trigger %d: %s", trigger.id, e.getMessage());
        }
    }

    private void unscheduleCronTrigger(Long triggerId) {
        try {
            scheduler.unscheduleJob(jobId(triggerId));
        } catch (Exception ignored) {}
    }

    private String jobId(Long id) {
        return "trigger-" + id;
    }

    public List<TriggerConfig> findAll() {
        return TriggerConfig.listAll();
    }

    public TriggerConfig findById(Long id) {
        return TriggerConfig.findById(id);
    }

    @Transactional
    public TriggerConfig create(TriggerConfig trigger) {
        if (trigger.getTestScenario() != null && trigger.getTestScenario().id != null) {
            trigger.setTestScenario(TestScenario.findById(trigger.getTestScenario().id));
        }
        trigger.persist();
        if (trigger instanceof CronTriggerConfig cron
                && Boolean.TRUE.equals(cron.getEnabled())
                && cron.getCronExpression() != null) {
            scheduleCronTrigger(cron);
        }
        return trigger;
    }

    @Transactional
    public TriggerConfig update(Long id, TriggerConfig updates) {
        TriggerConfig trigger = TriggerConfig.findById(id);
        if (trigger == null) throw new IllegalArgumentException("Trigger not found: " + id);

        unscheduleCronTrigger(id);

        trigger.setName(updates.getName());
        trigger.setDescription(updates.getDescription());
        trigger.setEnabled(updates.getEnabled());

        if (updates.getTestScenario() != null && updates.getTestScenario().id != null) {
            trigger.setTestScenario(TestScenario.findById(updates.getTestScenario().id));
        } else {
            trigger.setTestScenario(null);
        }

        if (trigger instanceof HttpTriggerConfig http && updates instanceof HttpTriggerConfig httpU) {
            http.setHttpUrl(httpU.getHttpUrl());
            http.setHttpMethod(httpU.getHttpMethod());
            http.setHttpBody(httpU.getHttpBody());
            http.setHttpHeaders(httpU.getHttpHeaders());
        } else if (trigger instanceof CronTriggerConfig cron && updates instanceof CronTriggerConfig cronU) {
            cron.setCronExpression(cronU.getCronExpression());
        } else if (trigger instanceof AmqpTriggerConfig amqp && updates instanceof AmqpTriggerConfig amqpU) {
            amqp.setAmqpAddress(amqpU.getAmqpAddress());
            amqp.setAmqpBody(amqpU.getAmqpBody());
            amqp.setAmqpProperties(amqpU.getAmqpProperties());
            amqp.setAmqpRoutingType(amqpU.getAmqpRoutingType() != null ? amqpU.getAmqpRoutingType() : "ANYCAST");
        }

        if (trigger instanceof CronTriggerConfig cron
                && Boolean.TRUE.equals(cron.getEnabled())
                && cron.getCronExpression() != null) {
            scheduleCronTrigger(cron);
        }
        return trigger;
    }

    @Transactional
    public void delete(Long id) {
        unscheduleCronTrigger(id);
        TriggerConfig trigger = TriggerConfig.findById(id);
        if (trigger != null) trigger.delete();
    }

    /**
     * Fires a trigger immediately. HTTP triggers make a synchronous outbound HTTP call;
     * AMQP triggers publish a message to the configured address; CRON triggers are not
     * fired via this method. Updates {@code lastFiredAt} regardless of outcome.
     *
     * @throws IllegalStateException if the trigger is disabled
     */
    @Transactional
    public TriggerFireResult fire(Long id) {
        TriggerConfig trigger = TriggerConfig.findById(id);
        if (trigger == null) throw new IllegalArgumentException("Trigger not found: " + id);
        if (!Boolean.TRUE.equals(trigger.getEnabled())) {
            throw new IllegalStateException("Trigger is disabled");
        }

        Integer responseStatus = null;
        String responseBody = null;
        String error = null;
        String messageId = null;

        if (trigger instanceof HttpTriggerConfig http && http.getHttpUrl() != null) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(http.getHttpUrl()));

                if (http.getHttpHeaders() != null) {
                    for (Map.Entry<String, String> header : http.getHttpHeaders().entrySet()) {
                        builder.header(header.getKey(), header.getValue());
                    }
                }

                String method = http.getHttpMethod() != null ? http.getHttpMethod().toUpperCase() : "POST";
                String body = http.getHttpBody() != null ? http.getHttpBody() : "";

                if (method.equals("GET") || method.equals("DELETE")) {
                    builder.method(method, HttpRequest.BodyPublishers.noBody());
                } else {
                    if (!builder.build().headers().map().containsKey("Content-Type")) {
                        builder.header("Content-Type", "application/json");
                    }
                    builder.method(method, HttpRequest.BodyPublishers.ofString(body));
                }

                HttpResponse<String> response = httpClient.send(builder.build(),
                        HttpResponse.BodyHandlers.ofString());
                responseStatus = response.statusCode();
                responseBody = response.body();
                LOG.infof("Trigger %s fired: %s %s -> %d", trigger.getName(), method, http.getHttpUrl(), responseStatus);
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                error = cause.getClass().getSimpleName() + ": " + (cause.getMessage() != null ? cause.getMessage() : e.getClass().getSimpleName());
                LOG.warnf("Trigger %s HTTP call failed: %s", trigger.getName(), error);
            }
        } else if (trigger instanceof AmqpTriggerConfig amqp) {
            try {
                messageId = amqpConnectionService.publish(
                        amqp.getAmqpAddress(),
                        amqp.getAmqpBody(),
                        amqp.getAmqpProperties(),
                        amqp.getAmqpRoutingType()
                );
                LOG.infof("Trigger %s fired AMQP message to: %s (id: %s)",
                        trigger.getName(), amqp.getAmqpAddress(), messageId);
            } catch (Exception e) {
                error = e.getMessage();
                LOG.warnf("Trigger %s AMQP publish failed: %s", trigger.getName(), error);
            }
        }

        trigger.setLastFiredAt(LocalDateTime.now());

        return new TriggerFireResult(responseStatus, responseBody, error, messageId, trigger.getLastFiredAt());
    }

    public record TriggerFireResult(
            Integer responseStatus,
            String responseBody,
            String error,
            String messageId,
            LocalDateTime firedAt
    ) {}
}
