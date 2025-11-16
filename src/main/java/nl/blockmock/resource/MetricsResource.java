package nl.blockmock.resource;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.blockmock.domain.MockEndpoint;
import nl.blockmock.service.MockEndpointService;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/api/metrics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MetricsResource {

    private static final Logger LOG = Logger.getLogger(MetricsResource.class);

    @Inject
    MockEndpointService mockEndpointService;

    @GET
    public List<Map<String, Object>> getAllMetrics() {
        List<MockEndpoint> endpoints = mockEndpointService.findAll();

        return endpoints.stream()
                .map(this::endpointToMetrics)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Map<String, Object> getEndpointMetrics(@PathParam("id") Long id) {
        MockEndpoint endpoint = mockEndpointService.findById(id)
                .orElseThrow(() -> new NotFoundException("Endpoint not found"));

        return endpointToMetrics(endpoint);
    }

    @POST
    @Path("/{id}/reset")
    @Transactional
    public Response resetEndpointMetrics(@PathParam("id") Long id) {
        MockEndpoint endpoint = MockEndpoint.findById(id);
        if (endpoint == null) {
            throw new NotFoundException("Endpoint not found");
        }

        endpoint.setTotalRequests(0L);
        endpoint.setMatchedRequests(0L);
        endpoint.setUnmatchedRequests(0L);
        endpoint.setLastRequestAt(null);
        endpoint.setAverageResponseTimeMs(0);
        endpoint.persist();

        LOG.info("Reset metrics for endpoint: " + endpoint.getName());
        return Response.ok("{\"status\": \"Metrics reset\"}").build();
    }

    @POST
    @Path("/reset-all")
    @Transactional
    public Response resetAllMetrics() {
        List<MockEndpoint> endpoints = MockEndpoint.listAll();

        for (MockEndpoint endpoint : endpoints) {
            endpoint.setTotalRequests(0L);
            endpoint.setMatchedRequests(0L);
            endpoint.setUnmatchedRequests(0L);
            endpoint.setLastRequestAt(null);
            endpoint.setAverageResponseTimeMs(0);
            endpoint.persist();
        }

        LOG.info("Reset metrics for all endpoints");
        return Response.ok("{\"status\": \"All metrics reset\"}").build();
    }

    private Map<String, Object> endpointToMetrics(MockEndpoint endpoint) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("endpointId", endpoint.id);
        metrics.put("endpointName", endpoint.getName());
        metrics.put("protocol", endpoint.getProtocol());
        metrics.put("enabled", endpoint.getEnabled());
        metrics.put("totalRequests", endpoint.getTotalRequests());
        metrics.put("matchedRequests", endpoint.getMatchedRequests());
        metrics.put("unmatchedRequests", endpoint.getUnmatchedRequests());
        metrics.put("lastRequestAt", endpoint.getLastRequestAt());
        metrics.put("averageResponseTimeMs", endpoint.getAverageResponseTimeMs());

        // Calculate success rate
        if (endpoint.getTotalRequests() > 0) {
            double successRate = (endpoint.getMatchedRequests().doubleValue() / endpoint.getTotalRequests().doubleValue()) * 100.0;
            metrics.put("successRate", String.format("%.1f%%", successRate));
        } else {
            metrics.put("successRate", "N/A");
        }

        return metrics;
    }
}
