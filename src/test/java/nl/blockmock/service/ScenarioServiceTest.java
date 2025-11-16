package nl.blockmock.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ScenarioServiceTest {

    @Inject
    ScenarioService scenarioService;

    @Inject
    MockEndpointService mockEndpointService;

    private Long testScenarioId;
    private Long testEndpointId;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing test data
        Scenario.deleteAll();
        MockEndpoint.deleteAll();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        if (testScenarioId != null) {
            Scenario.deleteById(testScenarioId);
        }
        if (testEndpointId != null) {
            MockEndpoint.deleteById(testEndpointId);
        }
    }

    @Test
    void testCreateScenario() {
        // Given
        MockEndpoint endpoint = createTestEndpoint();
        MockEndpoint created = mockEndpointService.create(endpoint);
        testEndpointId = created.id;

        Scenario scenario = new Scenario();
        scenario.setName("Test Scenario");
        scenario.setDescription("Test Description");
        scenario.setColor("#667eea");

        List<ScenarioStep> steps = new ArrayList<>();
        ScenarioStep step1 = new ScenarioStep();
        step1.setStepOrder(0);
        step1.setAction(ScenarioAction.ENABLE);
        step1.setMockEndpoint(created);
        steps.add(step1);

        ScenarioStep step2 = new ScenarioStep();
        step2.setStepOrder(1);
        step2.setAction(ScenarioAction.DELAY);
        step2.setDelayMs(1000);
        steps.add(step2);

        ScenarioStep step3 = new ScenarioStep();
        step3.setStepOrder(2);
        step3.setAction(ScenarioAction.DISABLE);
        step3.setMockEndpoint(created);
        steps.add(step3);

        scenario.setSteps(steps);

        // When
        Scenario createdScenario = scenarioService.create(scenario);
        testScenarioId = createdScenario.id;

        // Then
        assertNotNull(createdScenario.id);
        assertEquals("Test Scenario", createdScenario.getName());
        assertEquals(3, createdScenario.getSteps().size());
        assertEquals(ScenarioAction.ENABLE, createdScenario.getSteps().get(0).getAction());
        assertEquals(ScenarioAction.DELAY, createdScenario.getSteps().get(1).getAction());
        assertEquals(ScenarioAction.DISABLE, createdScenario.getSteps().get(2).getAction());
    }

    @Test
    void testExecuteScenarioEnableDisable() throws InterruptedException {
        // Given
        MockEndpoint endpoint = createTestEndpoint();
        endpoint.setEnabled(false); // Start disabled
        MockEndpoint created = mockEndpointService.create(endpoint);
        testEndpointId = created.id;

        Scenario scenario = new Scenario();
        scenario.setName("Enable/Disable Test");

        List<ScenarioStep> steps = new ArrayList<>();

        // Step 1: Enable endpoint
        ScenarioStep step1 = new ScenarioStep();
        step1.setStepOrder(0);
        step1.setAction(ScenarioAction.ENABLE);
        step1.setMockEndpoint(created);
        steps.add(step1);

        // Step 2: Small delay
        ScenarioStep step2 = new ScenarioStep();
        step2.setStepOrder(1);
        step2.setAction(ScenarioAction.DELAY);
        step2.setDelayMs(100);
        steps.add(step2);

        // Step 3: Disable endpoint
        ScenarioStep step3 = new ScenarioStep();
        step3.setStepOrder(2);
        step3.setAction(ScenarioAction.DISABLE);
        step3.setMockEndpoint(created);
        steps.add(step3);

        scenario.setSteps(steps);
        Scenario createdScenario = scenarioService.create(scenario);
        testScenarioId = createdScenario.id;

        // When
        scenarioService.executeScenario(createdScenario.id);

        // Then
        MockEndpoint finalState = mockEndpointService.findById(created.id).orElseThrow();
        assertFalse(finalState.getEnabled(), "Endpoint should be disabled after scenario execution");
    }

    @Test
    void testExecuteScenarioWithDelay() {
        // Given
        Scenario scenario = new Scenario();
        scenario.setName("Delay Test");

        List<ScenarioStep> steps = new ArrayList<>();

        ScenarioStep delayStep = new ScenarioStep();
        delayStep.setStepOrder(0);
        delayStep.setAction(ScenarioAction.DELAY);
        delayStep.setDelayMs(500);
        steps.add(delayStep);

        scenario.setSteps(steps);
        Scenario createdScenario = scenarioService.create(scenario);
        testScenarioId = createdScenario.id;

        // When
        long startTime = System.currentTimeMillis();
        scenarioService.executeScenario(createdScenario.id);
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        assertTrue(duration >= 500, "Delay should be at least 500ms, was: " + duration);
    }

    @Test
    void testFindAll() {
        // Given
        Scenario scenario1 = new Scenario();
        scenario1.setName("Scenario 1");
        scenario1.setSteps(new ArrayList<>());
        Scenario created1 = scenarioService.create(scenario1);

        Scenario scenario2 = new Scenario();
        scenario2.setName("Scenario 2");
        scenario2.setSteps(new ArrayList<>());
        Scenario created2 = scenarioService.create(scenario2);

        // When
        List<Scenario> all = scenarioService.findAll();

        // Then
        assertTrue(all.size() >= 2);
        assertTrue(all.stream().anyMatch(s -> s.id.equals(created1.id)));
        assertTrue(all.stream().anyMatch(s -> s.id.equals(created2.id)));

        // Cleanup
        deleteScenario(created1.id);
        deleteScenario(created2.id);
    }

    @Test
    void testFindById() {
        // Given
        Scenario scenario = new Scenario();
        scenario.setName("Test Scenario");
        scenario.setSteps(new ArrayList<>());
        Scenario created = scenarioService.create(scenario);
        testScenarioId = created.id;

        // When
        Scenario found = scenarioService.findById(created.id);

        // Then
        assertNotNull(found);
        assertEquals(created.id, found.id);
        assertEquals("Test Scenario", found.getName());
    }

    @Test
    void testUpdate() {
        // Given
        Scenario scenario = new Scenario();
        scenario.setName("Original Name");
        scenario.setSteps(new ArrayList<>());
        Scenario created = scenarioService.create(scenario);
        testScenarioId = created.id;

        // When
        created.setName("Updated Name");
        created.setDescription("New Description");
        Scenario updated = scenarioService.update(created.id, created);

        // Then
        assertEquals("Updated Name", updated.getName());
        assertEquals("New Description", updated.getDescription());
    }

    @Test
    void testDelete() {
        // Given
        Scenario scenario = new Scenario();
        scenario.setName("To Delete");
        scenario.setSteps(new ArrayList<>());
        Scenario created = scenarioService.create(scenario);
        Long id = created.id;

        // When
        scenarioService.delete(id);

        // Then
        Scenario deleted = scenarioService.findById(id);
        assertNull(deleted);
        testScenarioId = null; // Already deleted
    }

    private MockEndpoint createTestEndpoint() {
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("Test Endpoint");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);

        HttpConfig httpConfig = new HttpConfig();
        httpConfig.setMethod(HttpMethod.GET);
        httpConfig.setPath("/test");
        endpoint.setHttpConfig(httpConfig);

        return endpoint;
    }

    @Transactional
    void deleteScenario(Long id) {
        Scenario.deleteById(id);
    }
}
