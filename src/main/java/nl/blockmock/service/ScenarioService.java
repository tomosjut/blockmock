package nl.blockmock.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.Scenario;
import nl.blockmock.domain.ScenarioAction;
import nl.blockmock.domain.ScenarioStep;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class ScenarioService {

    private static final Logger LOG = Logger.getLogger(ScenarioService.class);

    @Inject
    MockEndpointService mockEndpointService;

    public List<Scenario> findAll() {
        return Scenario.listAll();
    }

    public Scenario findById(Long id) {
        return Scenario.findById(id);
    }

    @Transactional
    public Scenario create(Scenario scenario) {
        // Set bidirectional relationships
        if (scenario.getSteps() != null) {
            for (ScenarioStep step : scenario.getSteps()) {
                step.setScenario(scenario);
            }
        }

        scenario.persist();
        return scenario;
    }

    @Transactional
    public Scenario update(Long id, Scenario updates) {
        Scenario scenario = Scenario.findById(id);
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario not found: " + id);
        }

        scenario.setName(updates.getName());
        scenario.setDescription(updates.getDescription());
        scenario.setColor(updates.getColor());

        // Update steps
        scenario.getSteps().clear();
        if (updates.getSteps() != null) {
            for (ScenarioStep step : updates.getSteps()) {
                step.setScenario(scenario);
                scenario.getSteps().add(step);
            }
        }

        scenario.persist();
        return scenario;
    }

    @Transactional
    public void delete(Long id) {
        Scenario scenario = Scenario.findById(id);
        if (scenario != null) {
            scenario.delete();
        }
    }

    /**
     * Execute a scenario - runs all steps in order
     */
    @Transactional
    public void executeScenario(Long scenarioId) {
        Scenario scenario = Scenario.findById(scenarioId);
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario not found: " + scenarioId);
        }

        LOG.info("Executing scenario: " + scenario.getName());

        for (ScenarioStep step : scenario.getSteps()) {
            try {
                executeStep(step);
            } catch (Exception e) {
                LOG.error("Error executing step " + step.getStepOrder() + " in scenario " + scenario.getName(), e);
                throw new RuntimeException("Failed to execute scenario step", e);
            }
        }

        LOG.info("Scenario execution completed: " + scenario.getName());
    }

    private void executeStep(ScenarioStep step) throws InterruptedException {
        LOG.info("Executing step " + step.getStepOrder() + ": " + step.getAction());

        switch (step.getAction()) {
            case ENABLE:
                if (step.getMockEndpoint() != null) {
                    mockEndpointService.toggleEnabled(step.getMockEndpoint().id, true);
                    LOG.info("Enabled endpoint: " + step.getMockEndpoint().getName());
                }
                break;

            case DISABLE:
                if (step.getMockEndpoint() != null) {
                    mockEndpointService.toggleEnabled(step.getMockEndpoint().id, false);
                    LOG.info("Disabled endpoint: " + step.getMockEndpoint().getName());
                }
                break;

            case DELAY:
                if (step.getDelayMs() != null && step.getDelayMs() > 0) {
                    LOG.info("Waiting " + step.getDelayMs() + "ms");
                    Thread.sleep(step.getDelayMs());
                }
                break;
        }
    }
}
