package nl.blockmock.service;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.TestScenario;
import nl.blockmock.domain.TriggerConfig;
import nl.blockmock.domain.TriggerType;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class TriggerService {

    private static final Logger LOG = Logger.getLogger(TriggerService.class);

    @Inject
    Scheduler scheduler;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    void onStart(@Observes StartupEvent event) {
        scheduleAllCronTriggers();
    }

    private void scheduleAllCronTriggers() {
        List<TriggerConfig> triggers = TriggerConfig.listAll();
        for (TriggerConfig trigger : triggers) {
            if (trigger.getType() == TriggerType.CRON && Boolean.TRUE.equals(trigger.getEnabled())) {
                scheduleCronTrigger(trigger);
            }
        }
    }

    private void scheduleCronTrigger(TriggerConfig trigger) {
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
        if (trigger.getType() == TriggerType.CRON && Boolean.TRUE.equals(trigger.getEnabled())
                && trigger.getCronExpression() != null) {
            scheduleCronTrigger(trigger);
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
        trigger.setType(updates.getType());
        trigger.setEnabled(updates.getEnabled());
        trigger.setHttpUrl(updates.getHttpUrl());
        trigger.setHttpMethod(updates.getHttpMethod());
        trigger.setHttpBody(updates.getHttpBody());
        trigger.setHttpHeaders(updates.getHttpHeaders());
        trigger.setCronExpression(updates.getCronExpression());

        if (updates.getTestScenario() != null && updates.getTestScenario().id != null) {
            trigger.setTestScenario(TestScenario.findById(updates.getTestScenario().id));
        } else {
            trigger.setTestScenario(null);
        }

        if (trigger.getType() == TriggerType.CRON && Boolean.TRUE.equals(trigger.getEnabled())
                && trigger.getCronExpression() != null) {
            scheduleCronTrigger(trigger);
        }
        return trigger;
    }

    @Transactional
    public void delete(Long id) {
        unscheduleCronTrigger(id);
        TriggerConfig trigger = TriggerConfig.findById(id);
        if (trigger != null) trigger.delete();
    }

    @Transactional
    public TriggerFireResult fire(Long id) {
        TriggerConfig trigger = TriggerConfig.findById(id);
        if (trigger == null) throw new IllegalArgumentException("Trigger not found: " + id);
        if (!Boolean.TRUE.equals(trigger.getEnabled())) {
            throw new IllegalStateException("Trigger is disabled");
        }

        // Execute HTTP call
        Integer responseStatus = null;
        String responseBody = null;
        String error = null;

        if (trigger.getType() == TriggerType.HTTP && trigger.getHttpUrl() != null) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(trigger.getHttpUrl()));

                if (trigger.getHttpHeaders() != null) {
                    for (Map.Entry<String, String> header : trigger.getHttpHeaders().entrySet()) {
                        builder.header(header.getKey(), header.getValue());
                    }
                }

                String method = trigger.getHttpMethod() != null ? trigger.getHttpMethod().toUpperCase() : "POST";
                String body = trigger.getHttpBody() != null ? trigger.getHttpBody() : "";

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
                LOG.infof("Trigger %s fired: %s %s -> %d", trigger.getName(), method, trigger.getHttpUrl(), responseStatus);
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                error = cause.getClass().getSimpleName() + ": " + (cause.getMessage() != null ? cause.getMessage() : e.getClass().getSimpleName());
                LOG.warnf("Trigger %s HTTP call failed: %s", trigger.getName(), error);
            }
        }

        trigger.setLastFiredAt(LocalDateTime.now());

        return new TriggerFireResult(responseStatus, responseBody, error);
    }

    public record TriggerFireResult(Integer responseStatus, String responseBody, String error) {}
}
