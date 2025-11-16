package nl.blockmock.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.*;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MockEndpointService {

    @Transactional
    public MockEndpoint create(MockEndpoint endpoint) {
        // Fix bidirectional relationships before persisting
        if (endpoint.getHttpConfig() != null) {
            endpoint.getHttpConfig().setMockEndpoint(endpoint);
        }

        if (endpoint.getSftpConfig() != null) {
            endpoint.getSftpConfig().setMockEndpoint(endpoint);
        }

        if (endpoint.getAmqpConfig() != null) {
            endpoint.getAmqpConfig().setMockEndpoint(endpoint);
        }

        if (endpoint.getSqlConfig() != null) {
            endpoint.getSqlConfig().setMockEndpoint(endpoint);
            if (endpoint.getSqlConfig().getQueryMocks() != null) {
                for (var queryMock : endpoint.getSqlConfig().getQueryMocks()) {
                    queryMock.setSqlConfig(endpoint.getSqlConfig());
                }
            }
        }

        if (endpoint.getResponses() != null) {
            for (MockResponse response : endpoint.getResponses()) {
                response.setMockEndpoint(endpoint);
            }
        }

        endpoint.persist();
        return endpoint;
    }

    @Transactional
    public MockEndpoint update(MockEndpoint endpoint) {
        // Fix bidirectional relationships before merging
        if (endpoint.getHttpConfig() != null) {
            endpoint.getHttpConfig().setMockEndpoint(endpoint);
        }

        if (endpoint.getSftpConfig() != null) {
            endpoint.getSftpConfig().setMockEndpoint(endpoint);
        }

        if (endpoint.getAmqpConfig() != null) {
            endpoint.getAmqpConfig().setMockEndpoint(endpoint);
        }

        if (endpoint.getSqlConfig() != null) {
            endpoint.getSqlConfig().setMockEndpoint(endpoint);
            if (endpoint.getSqlConfig().getQueryMocks() != null) {
                for (var queryMock : endpoint.getSqlConfig().getQueryMocks()) {
                    queryMock.setSqlConfig(endpoint.getSqlConfig());
                }
            }
        }

        if (endpoint.getResponses() != null) {
            for (MockResponse response : endpoint.getResponses()) {
                response.setMockEndpoint(endpoint);
            }
        }

        return MockEndpoint.getEntityManager().merge(endpoint);
    }

    @Transactional
    public void delete(Long id) {
        MockEndpoint.deleteById(id);
    }

    public Optional<MockEndpoint> findById(Long id) {
        return MockEndpoint.findByIdOptional(id);
    }

    public List<MockEndpoint> findAll() {
        return MockEndpoint.listAll();
    }

    public List<MockEndpoint> findByProtocol(ProtocolType protocol) {
        return MockEndpoint.list("protocol", protocol);
    }

    public List<MockEndpoint> findEnabledByProtocol(ProtocolType protocol) {
        return MockEndpoint.list("protocol = ?1 and enabled = true", protocol);
    }

    @Transactional
    public void toggleEnabled(Long id) {
        MockEndpoint endpoint = MockEndpoint.findById(id);
        if (endpoint != null) {
            endpoint.setEnabled(!endpoint.getEnabled());
        }
    }

    @Transactional
    public MockResponse addResponse(Long endpointId, MockResponse response) {
        MockEndpoint endpoint = MockEndpoint.findById(endpointId);
        if (endpoint == null) {
            throw new IllegalArgumentException("MockEndpoint not found with id: " + endpointId);
        }
        endpoint.addResponse(response);
        response.persist();
        return response;
    }

    @Transactional
    public void deleteResponse(Long responseId) {
        MockResponse.deleteById(responseId);
    }
}
