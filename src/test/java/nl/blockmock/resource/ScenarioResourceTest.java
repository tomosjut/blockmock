package nl.blockmock.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
class ScenarioResourceTest {

    private Long testEndpointId;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing test data
        Scenario.deleteAll();
        MockEndpoint.deleteAll();

        // Create test endpoint
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("Test Endpoint");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);

        HttpConfig httpConfig = new HttpConfig();
        httpConfig.setMethod(HttpMethod.GET);
        httpConfig.setPath("/test");
        httpConfig.setMockEndpoint(endpoint);
        endpoint.setHttpConfig(httpConfig);

        endpoint.persist();
        testEndpointId = endpoint.id;
    }

    @AfterEach
    @Transactional
    void tearDown() {
        Scenario.deleteAll();
        if (testEndpointId != null) {
            MockEndpoint.deleteById(testEndpointId);
        }
    }

    @Test
    void testCreateScenario() {
        Map<String, Object> scenario = new HashMap<>();
        scenario.put("name", "Test Scenario");
        scenario.put("description", "Test Description");
        scenario.put("color", "#667eea");

        List<Map<String, Object>> steps = new ArrayList<>();

        Map<String, Object> step1 = new HashMap<>();
        step1.put("stepOrder", 0);
        step1.put("action", "ENABLE");
        step1.put("mockEndpoint", Map.of("id", testEndpointId));
        steps.add(step1);

        Map<String, Object> step2 = new HashMap<>();
        step2.put("stepOrder", 1);
        step2.put("action", "DELAY");
        step2.put("delayMs", 1000);
        steps.add(step2);

        scenario.put("steps", steps);

        given()
            .contentType(ContentType.JSON)
            .body(scenario)
        .when()
            .post("/api/scenarios")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("Test Scenario"))
            .body("description", equalTo("Test Description"))
            .body("steps.size()", equalTo(2));
    }

    @Test
    void testGetAllScenarios() {
        // Create test scenarios
        createTestScenario("Scenario 1");
        createTestScenario("Scenario 2");

        given()
        .when()
            .get("/api/scenarios")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(1));
    }

    @Test
    void testGetScenarioById() {
        Long id = createTestScenario("Test Scenario");

        given()
            .pathParam("id", id)
        .when()
            .get("/api/scenarios/{id}")
        .then()
            .statusCode(200)
            .body("id", equalTo(id.intValue()))
            .body("name", equalTo("Test Scenario"));
    }

    @Test
    void testUpdateScenario() {
        Long id = createTestScenario("Original Name");

        Map<String, Object> update = new HashMap<>();
        update.put("id", id);
        update.put("name", "Updated Name");
        update.put("description", "Updated Description");
        update.put("color", "#ff0000");
        update.put("steps", new ArrayList<>());

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", id)
            .body(update)
        .when()
            .put("/api/scenarios/{id}")
        .then()
            .statusCode(200)
            .body("name", equalTo("Updated Name"))
            .body("description", equalTo("Updated Description"))
            .body("color", equalTo("#ff0000"));
    }

    @Test
    void testDeleteScenario() {
        Long id = createTestScenario("To Delete");

        given()
            .pathParam("id", id)
        .when()
            .delete("/api/scenarios/{id}")
        .then()
            .statusCode(204);

        // Verify deletion
        given()
            .pathParam("id", id)
        .when()
            .get("/api/scenarios/{id}")
        .then()
            .statusCode(404);
    }

    @Test
    void testExecuteScenario() {
        Long id = createTestScenario("Execute Test");

        given()
            .pathParam("id", id)
        .when()
            .post("/api/scenarios/{id}/execute")
        .then()
            .statusCode(200);
    }

    @Test
    void testExecuteNonExistentScenario() {
        given()
            .pathParam("id", 99999)
        .when()
            .post("/api/scenarios/{id}/execute")
        .then()
            .statusCode(404);
    }

    @Transactional
    Long createTestScenario(String name) {
        Scenario scenario = new Scenario();
        scenario.setName(name);
        scenario.setDescription("Test Description");
        scenario.setColor("#667eea");

        List<ScenarioStep> steps = new ArrayList<>();

        ScenarioStep step = new ScenarioStep();
        step.setStepOrder(0);
        step.setAction(ScenarioAction.DELAY);
        step.setDelayMs(100);
        step.setScenario(scenario);
        steps.add(step);

        scenario.setSteps(steps);
        scenario.persist();
        return scenario.id;
    }
}
