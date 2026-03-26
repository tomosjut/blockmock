package nl.blockmock.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import nl.blockmock.domain.HttpMethod;
import nl.blockmock.domain.HttpMockEndpoint;
import nl.blockmock.domain.MockEndpoint;
import nl.blockmock.domain.MockResponse;
import nl.blockmock.domain.PatternType;
import nl.blockmock.domain.ProtocolType;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/api/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TemplatesResource {

    private static final Logger LOG = Logger.getLogger(TemplatesResource.class);

    @GET
    public List<Map<String, Object>> getTemplates() {
        List<Map<String, Object>> templates = new ArrayList<>();

        templates.add(createTemplate("rest-api", "REST API",
                "Standard REST API endpoint with GET/POST/PUT/DELETE operations", "HTTP",
                createRestApiEndpoint()));

        templates.add(createTemplate("graphql", "GraphQL API",
                "GraphQL endpoint with query and mutation support", "HTTP",
                createGraphQLEndpoint()));

        templates.add(createTemplate("oauth2", "OAuth2 Server",
                "OAuth2 token endpoint for authentication", "HTTP",
                createOAuth2Endpoint()));

        templates.add(createTemplate("webhook", "Webhook Receiver",
                "Webhook endpoint that receives and logs events", "HTTP",
                createWebhookEndpoint()));

        return templates;
    }

    @GET
    @Path("/{templateId}")
    public MockEndpoint getTemplate(@PathParam("templateId") String templateId) {
        return switch (templateId) {
            case "rest-api" -> createRestApiEndpoint();
            case "graphql" -> createGraphQLEndpoint();
            case "oauth2" -> createOAuth2Endpoint();
            case "webhook" -> createWebhookEndpoint();
            default -> throw new NotFoundException("Template not found: " + templateId);
        };
    }

    private Map<String, Object> createTemplate(String id, String name, String description,
                                               String protocol, MockEndpoint endpoint) {
        Map<String, Object> template = new HashMap<>();
        template.put("id", id);
        template.put("name", name);
        template.put("description", description);
        template.put("protocol", protocol);
        template.put("endpoint", endpoint);
        return template;
    }

    private HttpMockEndpoint createRestApiEndpoint() {
        HttpMockEndpoint endpoint = new HttpMockEndpoint();
        endpoint.setName("REST API Mock");
        endpoint.setDescription("Standard REST API with CRUD operations");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);
        endpoint.setHttpMethod(HttpMethod.GET);
        endpoint.setHttpPath("/api/users");
        endpoint.setHttpPathRegex(false);

        MockResponse response = new MockResponse();
        response.setName("Success Response");
        response.setPriority(0);
        response.setResponseStatusCode(200);
        response.setResponseBody("{\"users\": [{\"id\": 1, \"name\": \"John Doe\"}]}");
        response.setResponseHeaders(Map.of("Content-Type", "application/json"));
        response.setResponseDelayMs(0);
        endpoint.setResponses(new ArrayList<>(List.of(response)));
        return endpoint;
    }

    private HttpMockEndpoint createGraphQLEndpoint() {
        HttpMockEndpoint endpoint = new HttpMockEndpoint();
        endpoint.setName("GraphQL API Mock");
        endpoint.setDescription("GraphQL endpoint with query support");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);
        endpoint.setHttpMethod(HttpMethod.POST);
        endpoint.setHttpPath("/graphql");
        endpoint.setHttpPathRegex(false);

        MockResponse response = new MockResponse();
        response.setName("GraphQL Response");
        response.setPriority(0);
        response.setResponseStatusCode(200);
        response.setResponseBody("{\"data\": {\"user\": {\"id\": \"1\", \"name\": \"John Doe\"}}}");
        response.setResponseHeaders(Map.of("Content-Type", "application/json"));
        endpoint.setResponses(new ArrayList<>(List.of(response)));
        return endpoint;
    }

    private HttpMockEndpoint createOAuth2Endpoint() {
        HttpMockEndpoint endpoint = new HttpMockEndpoint();
        endpoint.setName("OAuth2 Token Endpoint");
        endpoint.setDescription("OAuth2 token endpoint for authentication testing");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);
        endpoint.setHttpMethod(HttpMethod.POST);
        endpoint.setHttpPath("/oauth/token");
        endpoint.setHttpPathRegex(false);

        MockResponse response = new MockResponse();
        response.setName("Token Response");
        response.setPriority(0);
        response.setResponseStatusCode(200);
        response.setResponseBody("{\"access_token\": \"mock_token_12345\", \"token_type\": \"Bearer\", \"expires_in\": 3600}");
        response.setResponseHeaders(Map.of("Content-Type", "application/json"));
        endpoint.setResponses(new ArrayList<>(List.of(response)));
        return endpoint;
    }

    private HttpMockEndpoint createWebhookEndpoint() {
        HttpMockEndpoint endpoint = new HttpMockEndpoint();
        endpoint.setName("Webhook Receiver");
        endpoint.setDescription("Receives and logs webhook events");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.FIRE_FORGET);
        endpoint.setEnabled(true);
        endpoint.setHttpMethod(HttpMethod.POST);
        endpoint.setHttpPath("/webhook/events");
        endpoint.setHttpPathRegex(false);

        MockResponse response = new MockResponse();
        response.setName("Webhook Accepted");
        response.setPriority(0);
        response.setResponseStatusCode(202);
        response.setResponseBody("{\"status\": \"accepted\", \"message\": \"Event received\"}");
        response.setResponseHeaders(Map.of("Content-Type", "application/json"));
        endpoint.setResponses(new ArrayList<>(List.of(response)));
        return endpoint;
    }
}
