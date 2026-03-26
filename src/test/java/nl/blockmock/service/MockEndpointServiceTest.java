package nl.blockmock.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class MockEndpointServiceTest {

    @Inject
    MockEndpointService mockEndpointService;

    private Long testEndpointId;

    @BeforeEach
    @Transactional
    void setUp() {
        MockEndpoint.deleteAll();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        if (testEndpointId != null) {
            MockEndpoint.deleteById(testEndpointId);
        }
    }

    @Test
    void testCreateHttpEndpoint() {
        HttpMockEndpoint endpoint = new HttpMockEndpoint();
        endpoint.setName("Test HTTP Endpoint");
        endpoint.setDescription("Test Description");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);
        endpoint.setHttpMethod(HttpMethod.GET);
        endpoint.setHttpPath("/test/api");
        endpoint.setHttpPathRegex(false);

        MockEndpoint created = mockEndpointService.create(endpoint);
        testEndpointId = created.id;

        assertNotNull(created.id);
        assertEquals("Test HTTP Endpoint", created.getName());
        assertEquals(ProtocolType.HTTP, created.getProtocol());
        assertInstanceOf(HttpMockEndpoint.class, created);
        assertEquals(HttpMethod.GET, ((HttpMockEndpoint) created).getHttpMethod());
        assertEquals("/test/api", ((HttpMockEndpoint) created).getHttpPath());
        assertTrue(created.getEnabled());
    }

    @Test
    void testFindById() {
        MockEndpoint created = mockEndpointService.create(createTestEndpoint());
        testEndpointId = created.id;

        Optional<MockEndpoint> found = mockEndpointService.findById(created.id);

        assertTrue(found.isPresent());
        assertEquals(created.id, found.get().id);
        assertEquals("Test Endpoint", found.get().getName());
    }

    @Test
    void testFindAll() {
        MockEndpoint endpoint1 = createTestEndpoint();
        endpoint1.setName("Endpoint 1");
        MockEndpoint created1 = mockEndpointService.create(endpoint1);

        MockEndpoint endpoint2 = createTestEndpoint();
        endpoint2.setName("Endpoint 2");
        MockEndpoint created2 = mockEndpointService.create(endpoint2);

        List<MockEndpoint> all = mockEndpointService.findAll();

        assertTrue(all.size() >= 2);
        assertTrue(all.stream().anyMatch(e -> e.id.equals(created1.id)));
        assertTrue(all.stream().anyMatch(e -> e.id.equals(created2.id)));

        deleteEndpoint(created1.id);
        deleteEndpoint(created2.id);
    }

    @Test
    void testToggleEnabled() {
        MockEndpoint endpoint = createTestEndpoint();
        endpoint.setEnabled(true);
        MockEndpoint created = mockEndpointService.create(endpoint);
        testEndpointId = created.id;

        MockEndpoint toggled = mockEndpointService.toggleEnabled(created.id);
        assertNotNull(toggled);
        assertFalse(toggled.getEnabled());

        MockEndpoint toggledAgain = mockEndpointService.toggleEnabled(created.id);
        assertNotNull(toggledAgain);
        assertTrue(toggledAgain.getEnabled());
    }

    @Test
    void testToggleEnabledWithValue() {
        MockEndpoint created = mockEndpointService.create(createTestEndpoint());
        testEndpointId = created.id;

        MockEndpoint disabled = mockEndpointService.toggleEnabled(created.id, false);
        assertFalse(disabled.getEnabled());

        MockEndpoint enabled = mockEndpointService.toggleEnabled(created.id, true);
        assertTrue(enabled.getEnabled());
    }

    @Test
    void testDelete() {
        MockEndpoint created = mockEndpointService.create(createTestEndpoint());
        Long id = created.id;

        mockEndpointService.delete(id);

        assertFalse(mockEndpointService.findById(id).isPresent());
        testEndpointId = null;
    }

    @Test
    void testFindByProtocol() {
        MockEndpoint http1 = createTestEndpoint();
        MockEndpoint created1 = mockEndpointService.create(http1);

        MockEndpoint http2 = createTestEndpoint();
        http2.setName("HTTP Endpoint 2");
        MockEndpoint created2 = mockEndpointService.create(http2);

        List<MockEndpoint> httpEndpoints = mockEndpointService.findByProtocol(ProtocolType.HTTP);

        assertTrue(httpEndpoints.stream().anyMatch(e -> e.id.equals(created1.id)));
        assertTrue(httpEndpoints.stream().anyMatch(e -> e.id.equals(created2.id)));

        deleteEndpoint(created1.id);
        deleteEndpoint(created2.id);
    }

    @Test
    void testCreateEndpointWithResponses() {
        HttpMockEndpoint endpoint = new HttpMockEndpoint();
        endpoint.setName("Endpoint with Responses");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setHttpMethod(HttpMethod.GET);
        endpoint.setHttpPath("/test");

        MockResponse response = new MockResponse();
        response.setName("Test Response");
        response.setPriority(0);
        response.setResponseStatusCode(200);
        response.setResponseBody("{\"status\": \"ok\"}");
        endpoint.addResponse(response);

        MockEndpoint created = mockEndpointService.create(endpoint);
        testEndpointId = created.id;

        assertNotNull(created.getResponses());
        assertEquals(1, created.getResponses().size());
        assertEquals("Test Response", created.getResponses().get(0).getName());
        assertEquals(200, created.getResponses().get(0).getResponseStatusCode());
    }

    private HttpMockEndpoint createTestEndpoint() {
        HttpMockEndpoint endpoint = new HttpMockEndpoint();
        endpoint.setName("Test Endpoint");
        endpoint.setDescription("Test Description");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);
        endpoint.setHttpMethod(HttpMethod.GET);
        endpoint.setHttpPath("/test");
        return endpoint;
    }

    @Transactional
    void deleteEndpoint(Long id) {
        MockEndpoint.deleteById(id);
    }
}
