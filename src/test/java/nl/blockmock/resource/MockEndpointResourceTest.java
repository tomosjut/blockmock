package nl.blockmock.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
class MockEndpointResourceTest {

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up any existing test data
        MockEndpoint.deleteAll();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Clean up test data
        MockEndpoint.deleteAll();
    }

    @Test
    void testCreateHttpEndpoint() {
        Map<String, Object> endpoint = new HashMap<>();
        endpoint.put("name", "Test HTTP Mock");
        endpoint.put("description", "Test Description");
        endpoint.put("protocol", "HTTP");
        endpoint.put("pattern", "REQUEST_REPLY");
        endpoint.put("enabled", true);

        Map<String, Object> httpConfig = new HashMap<>();
        httpConfig.put("method", "GET");
        httpConfig.put("path", "/api/test");
        httpConfig.put("pathRegex", false);
        endpoint.put("httpConfig", httpConfig);

        given()
            .contentType(ContentType.JSON)
            .body(endpoint)
        .when()
            .post("/api/endpoints")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("name", equalTo("Test HTTP Mock"))
            .body("protocol", equalTo("HTTP"))
            .body("httpConfig.method", equalTo("GET"))
            .body("httpConfig.path", equalTo("/api/test"));
    }

    @Test
    void testGetAllEndpoints() {
        // Create test endpoint first
        createTestEndpoint("Test Endpoint 1");
        createTestEndpoint("Test Endpoint 2");

        given()
        .when()
            .get("/api/endpoints")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(1));
    }

    @Test
    void testGetEndpointById() {
        // Create test endpoint
        Long id = createTestEndpoint("Test Endpoint");

        given()
            .pathParam("id", id)
        .when()
            .get("/api/endpoints/{id}")
        .then()
            .statusCode(200)
            .body("id", equalTo(id.intValue()))
            .body("name", equalTo("Test Endpoint"));
    }

    @Test
    void testGetNonExistentEndpoint() {
        given()
            .pathParam("id", 99999)
        .when()
            .get("/api/endpoints/{id}")
        .then()
            .statusCode(404);
    }

    @Test
    void testUpdateEndpoint() {
        // Create test endpoint
        Long id = createTestEndpoint("Original Name");

        Map<String, Object> update = new HashMap<>();
        update.put("id", id);
        update.put("name", "Updated Name");
        update.put("description", "Updated Description");
        update.put("protocol", "HTTP");
        update.put("pattern", "REQUEST_REPLY");
        update.put("enabled", true);

        Map<String, Object> httpConfig = new HashMap<>();
        httpConfig.put("method", "GET");
        httpConfig.put("path", "/updated");
        update.put("httpConfig", httpConfig);

        given()
            .contentType(ContentType.JSON)
            .pathParam("id", id)
            .body(update)
        .when()
            .put("/api/endpoints/{id}")
        .then()
            .statusCode(200)
            .body("name", equalTo("Updated Name"))
            .body("description", equalTo("Updated Description"));
    }

    @Test
    void testDeleteEndpoint() {
        // Create test endpoint
        Long id = createTestEndpoint("To Delete");

        given()
            .pathParam("id", id)
        .when()
            .delete("/api/endpoints/{id}")
        .then()
            .statusCode(204);

        // Verify deletion
        given()
            .pathParam("id", id)
        .when()
            .get("/api/endpoints/{id}")
        .then()
            .statusCode(404);
    }

    @Test
    void testToggleEndpoint() {
        // Create test endpoint (enabled by default)
        Long id = createTestEndpoint("To Toggle");

        // Toggle to disabled
        given()
            .pathParam("id", id)
        .when()
            .post("/api/endpoints/{id}/toggle")
        .then()
            .statusCode(200);

        // Verify it's disabled
        given()
            .pathParam("id", id)
        .when()
            .get("/api/endpoints/{id}")
        .then()
            .statusCode(200)
            .body("enabled", equalTo(false));

        // Toggle back to enabled
        given()
            .pathParam("id", id)
        .when()
            .post("/api/endpoints/{id}/toggle")
        .then()
            .statusCode(200);

        // Verify it's enabled
        given()
            .pathParam("id", id)
        .when()
            .get("/api/endpoints/{id}")
        .then()
            .statusCode(200)
            .body("enabled", equalTo(true));
    }

    @Test
    void testGetEndpointsByProtocol() {
        // Create endpoints with different protocols
        createTestEndpoint("HTTP Endpoint", ProtocolType.HTTP);
        createTestEndpoint("SFTP Endpoint", ProtocolType.SFTP);

        given()
            .queryParam("protocol", "HTTP")
        .when()
            .get("/api/endpoints")
        .then()
            .statusCode(200)
            .body("findAll { it.protocol == 'HTTP' }.size()", greaterThan(0));
    }

    @Transactional
    Long createTestEndpoint(String name) {
        return createTestEndpoint(name, ProtocolType.HTTP);
    }

    @Transactional
    Long createTestEndpoint(String name, ProtocolType protocol) {
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName(name);
        endpoint.setDescription("Test Description");
        endpoint.setProtocol(protocol);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);

        if (protocol == ProtocolType.HTTP) {
            HttpConfig httpConfig = new HttpConfig();
            httpConfig.setMethod(HttpMethod.GET);
            httpConfig.setPath("/test");
            httpConfig.setPathRegex(false);
            httpConfig.setMockEndpoint(endpoint);
            endpoint.setHttpConfig(httpConfig);
        }

        endpoint.persist();
        return endpoint.id;
    }
}
