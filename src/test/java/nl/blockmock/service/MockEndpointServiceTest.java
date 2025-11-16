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
        // Clean up any existing test data
        MockEndpoint.deleteAll();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Clean up test data
        if (testEndpointId != null) {
            MockEndpoint.deleteById(testEndpointId);
        }
    }

    @Test
    void testCreateHttpEndpoint() {
        // Given
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("Test HTTP Endpoint");
        endpoint.setDescription("Test Description");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);

        HttpConfig httpConfig = new HttpConfig();
        httpConfig.setMethod(HttpMethod.GET);
        httpConfig.setPath("/test/api");
        httpConfig.setPathRegex(false);
        endpoint.setHttpConfig(httpConfig);

        // When
        MockEndpoint created = mockEndpointService.create(endpoint);
        testEndpointId = created.id;

        // Then
        assertNotNull(created.id);
        assertEquals("Test HTTP Endpoint", created.getName());
        assertEquals(ProtocolType.HTTP, created.getProtocol());
        assertNotNull(created.getHttpConfig());
        assertEquals(HttpMethod.GET, created.getHttpConfig().getMethod());
        assertEquals("/test/api", created.getHttpConfig().getPath());
        assertTrue(created.getEnabled());
    }

    @Test
    void testCreateSftpEndpoint() {
        // Given
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("Test SFTP Endpoint");
        endpoint.setProtocol(ProtocolType.SFTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);

        SftpConfig sftpConfig = new SftpConfig();
        sftpConfig.setPort(2222);
        sftpConfig.setOperation(SftpOperation.UPLOAD);
        sftpConfig.setPathPattern("/uploads/*");
        sftpConfig.setUsername("testuser");
        sftpConfig.setPassword("testpass");
        endpoint.setSftpConfig(sftpConfig);

        // When
        MockEndpoint created = mockEndpointService.create(endpoint);
        testEndpointId = created.id;

        // Then
        assertNotNull(created.id);
        assertEquals("Test SFTP Endpoint", created.getName());
        assertEquals(ProtocolType.SFTP, created.getProtocol());
        assertNotNull(created.getSftpConfig());
        assertEquals(2222, created.getSftpConfig().getPort());
        assertEquals(SftpOperation.UPLOAD, created.getSftpConfig().getOperation());
    }

    @Test
    void testFindById() {
        // Given
        MockEndpoint endpoint = createTestEndpoint();
        MockEndpoint created = mockEndpointService.create(endpoint);
        testEndpointId = created.id;

        // When
        Optional<MockEndpoint> found = mockEndpointService.findById(created.id);

        // Then
        assertTrue(found.isPresent());
        assertEquals(created.id, found.get().id);
        assertEquals("Test Endpoint", found.get().getName());
    }

    @Test
    void testFindAll() {
        // Given
        MockEndpoint endpoint1 = createTestEndpoint();
        endpoint1.setName("Endpoint 1");
        MockEndpoint created1 = mockEndpointService.create(endpoint1);

        MockEndpoint endpoint2 = createTestEndpoint();
        endpoint2.setName("Endpoint 2");
        MockEndpoint created2 = mockEndpointService.create(endpoint2);

        // When
        List<MockEndpoint> all = mockEndpointService.findAll();

        // Then
        assertTrue(all.size() >= 2);
        assertTrue(all.stream().anyMatch(e -> e.id.equals(created1.id)));
        assertTrue(all.stream().anyMatch(e -> e.id.equals(created2.id)));

        // Cleanup
        deleteEndpoint(created1.id);
        deleteEndpoint(created2.id);
    }

    @Test
    void testToggleEnabled() {
        // Given
        MockEndpoint endpoint = createTestEndpoint();
        endpoint.setEnabled(true);
        MockEndpoint created = mockEndpointService.create(endpoint);
        testEndpointId = created.id;

        // When
        MockEndpoint toggled = mockEndpointService.toggleEnabled(created.id);

        // Then
        assertNotNull(toggled);
        assertFalse(toggled.getEnabled());

        // Toggle again
        MockEndpoint toggledAgain = mockEndpointService.toggleEnabled(created.id);
        assertNotNull(toggledAgain);
        assertTrue(toggledAgain.getEnabled());
    }

    @Test
    void testToggleEnabledWithValue() {
        // Given
        MockEndpoint endpoint = createTestEndpoint();
        endpoint.setEnabled(true);
        MockEndpoint created = mockEndpointService.create(endpoint);
        testEndpointId = created.id;

        // When
        MockEndpoint disabled = mockEndpointService.toggleEnabled(created.id, false);

        // Then
        assertNotNull(disabled);
        assertFalse(disabled.getEnabled());

        // Enable
        MockEndpoint enabled = mockEndpointService.toggleEnabled(created.id, true);
        assertNotNull(enabled);
        assertTrue(enabled.getEnabled());
    }

    @Test
    void testDelete() {
        // Given
        MockEndpoint endpoint = createTestEndpoint();
        MockEndpoint created = mockEndpointService.create(endpoint);
        Long id = created.id;

        // When
        mockEndpointService.delete(id);

        // Then
        Optional<MockEndpoint> deleted = mockEndpointService.findById(id);
        assertFalse(deleted.isPresent());
        testEndpointId = null; // Already deleted
    }

    @Test
    void testFindByProtocol() {
        // Given
        MockEndpoint httpEndpoint = createTestEndpoint();
        httpEndpoint.setProtocol(ProtocolType.HTTP);
        MockEndpoint http = mockEndpointService.create(httpEndpoint);

        MockEndpoint sftpEndpoint = createTestEndpoint();
        sftpEndpoint.setProtocol(ProtocolType.SFTP);
        MockEndpoint sftp = mockEndpointService.create(sftpEndpoint);

        // When
        List<MockEndpoint> httpEndpoints = mockEndpointService.findByProtocol(ProtocolType.HTTP);
        List<MockEndpoint> sftpEndpoints = mockEndpointService.findByProtocol(ProtocolType.SFTP);

        // Then
        assertTrue(httpEndpoints.stream().anyMatch(e -> e.id.equals(http.id)));
        assertTrue(sftpEndpoints.stream().anyMatch(e -> e.id.equals(sftp.id)));

        // Cleanup
        deleteEndpoint(http.id);
        deleteEndpoint(sftp.id);
    }

    @Test
    void testCreateEndpointWithResponses() {
        // Given
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("Endpoint with Responses");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);

        HttpConfig httpConfig = new HttpConfig();
        httpConfig.setMethod(HttpMethod.GET);
        httpConfig.setPath("/test");
        endpoint.setHttpConfig(httpConfig);

        MockResponse response = new MockResponse();
        response.setName("Test Response");
        response.setPriority(0);
        response.setResponseStatusCode(200);
        response.setResponseBody("{\"status\": \"ok\"}");
        endpoint.addResponse(response);

        // When
        MockEndpoint created = mockEndpointService.create(endpoint);
        testEndpointId = created.id;

        // Then
        assertNotNull(created.getResponses());
        assertEquals(1, created.getResponses().size());
        assertEquals("Test Response", created.getResponses().get(0).getName());
        assertEquals(200, created.getResponses().get(0).getResponseStatusCode());
    }

    private MockEndpoint createTestEndpoint() {
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("Test Endpoint");
        endpoint.setDescription("Test Description");
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
    void deleteEndpoint(Long id) {
        MockEndpoint.deleteById(id);
    }
}
