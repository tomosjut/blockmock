package nl.blockmock.resource;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
class MetricsResourceTest {

    private Long testEndpointId;

    @BeforeEach
    @Transactional
    void setUp() {
        MockEndpoint.deleteAll();

        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("Test Endpoint");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);
        endpoint.setHttpMethod(HttpMethod.GET);
        endpoint.setHttpPath("/test");
        endpoint.setTotalRequests(100L);
        endpoint.setMatchedRequests(80L);
        endpoint.setUnmatchedRequests(20L);
        endpoint.setLastRequestAt(LocalDateTime.now());
        endpoint.setAverageResponseTimeMs(150);

        endpoint.persist();
        testEndpointId = endpoint.id;
    }

    @AfterEach
    @Transactional
    void tearDown() {
        if (testEndpointId != null) {
            MockEndpoint.deleteById(testEndpointId);
        }
    }

    @Test
    void testGetAllMetrics() {
        given()
        .when()
            .get("/api/metrics")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0))
            .body("[0].endpointId", notNullValue())
            .body("[0].endpointName", notNullValue())
            .body("[0].protocol", notNullValue())
            .body("[0].totalRequests", notNullValue())
            .body("[0].matchedRequests", notNullValue())
            .body("[0].unmatchedRequests", notNullValue())
            .body("[0].successRate", notNullValue());
    }

    @Test
    void testGetEndpointMetrics() {
        given()
            .pathParam("id", testEndpointId)
        .when()
            .get("/api/metrics/{id}")
        .then()
            .statusCode(200)
            .body("endpointId", equalTo(testEndpointId.intValue()))
            .body("endpointName", equalTo("Test Endpoint"))
            .body("protocol", equalTo("HTTP"))
            .body("totalRequests", equalTo(100))
            .body("matchedRequests", equalTo(80))
            .body("unmatchedRequests", equalTo(20))
            .body("successRate", equalTo("80.0%"))
            .body("averageResponseTimeMs", equalTo(150));
    }

    @Test
    void testGetNonExistentEndpointMetrics() {
        given()
            .pathParam("id", 99999)
        .when()
            .get("/api/metrics/{id}")
        .then()
            .statusCode(404);
    }

    @Test
    void testResetEndpointMetrics() {
        given()
            .pathParam("id", testEndpointId)
        .when()
            .post("/api/metrics/{id}/reset")
        .then()
            .statusCode(200);

        given()
            .pathParam("id", testEndpointId)
        .when()
            .get("/api/metrics/{id}")
        .then()
            .statusCode(200)
            .body("totalRequests", equalTo(0))
            .body("matchedRequests", equalTo(0))
            .body("unmatchedRequests", equalTo(0))
            .body("averageResponseTimeMs", equalTo(0))
            .body("lastRequestAt", nullValue());
    }

    @Test
    void testResetAllMetrics() {
        Long secondEndpointId = createTestEndpoint("Second Endpoint", 50L, 40L, 10L);

        given()
        .when()
            .post("/api/metrics/reset-all")
        .then()
            .statusCode(200);

        given()
            .pathParam("id", testEndpointId)
        .when()
            .get("/api/metrics/{id}")
        .then()
            .statusCode(200)
            .body("totalRequests", equalTo(0))
            .body("matchedRequests", equalTo(0));

        given()
            .pathParam("id", secondEndpointId)
        .when()
            .get("/api/metrics/{id}")
        .then()
            .statusCode(200)
            .body("totalRequests", equalTo(0))
            .body("matchedRequests", equalTo(0));

        deleteEndpoint(secondEndpointId);
    }

    @Test
    void testSuccessRateCalculation() {
        Long perfectEndpointId = createTestEndpoint("Perfect Endpoint", 100L, 100L, 0L);
        Long halfEndpointId = createTestEndpoint("Half Endpoint", 100L, 50L, 50L);
        Long zeroEndpointId = createTestEndpoint("Zero Requests", 0L, 0L, 0L);

        given()
            .pathParam("id", perfectEndpointId)
        .when()
            .get("/api/metrics/{id}")
        .then()
            .statusCode(200)
            .body("successRate", equalTo("100.0%"));

        given()
            .pathParam("id", halfEndpointId)
        .when()
            .get("/api/metrics/{id}")
        .then()
            .statusCode(200)
            .body("successRate", equalTo("50.0%"));

        given()
            .pathParam("id", zeroEndpointId)
        .when()
            .get("/api/metrics/{id}")
        .then()
            .statusCode(200)
            .body("successRate", equalTo("N/A"));

        deleteEndpoint(perfectEndpointId);
        deleteEndpoint(halfEndpointId);
        deleteEndpoint(zeroEndpointId);
    }

    @Transactional
    Long createTestEndpoint(String name, Long total, Long matched, Long unmatched) {
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName(name);
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);
        endpoint.setHttpMethod(HttpMethod.GET);
        endpoint.setHttpPath("/test");
        endpoint.setTotalRequests(total);
        endpoint.setMatchedRequests(matched);
        endpoint.setUnmatchedRequests(unmatched);
        endpoint.persist();
        return endpoint.id;
    }

    @Transactional
    void deleteEndpoint(Long id) {
        MockEndpoint.deleteById(id);
    }
}
