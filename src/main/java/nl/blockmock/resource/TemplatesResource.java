package nl.blockmock.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import nl.blockmock.domain.*;
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

        // REST API Template
        templates.add(createTemplate(
            "rest-api",
            "REST API",
            "Standard REST API endpoint with GET/POST/PUT/DELETE operations",
            "HTTP",
            createRestApiEndpoint()
        ));

        // GraphQL Template
        templates.add(createTemplate(
            "graphql",
            "GraphQL API",
            "GraphQL endpoint with query and mutation support",
            "HTTP",
            createGraphQLEndpoint()
        ));

        // OAuth2 Template
        templates.add(createTemplate(
            "oauth2",
            "OAuth2 Server",
            "OAuth2 token endpoint for authentication",
            "HTTP",
            createOAuth2Endpoint()
        ));

        // Webhook Template
        templates.add(createTemplate(
            "webhook",
            "Webhook Receiver",
            "Webhook endpoint that receives and logs events",
            "HTTP",
            createWebhookEndpoint()
        ));

        // SFTP File Server Template
        templates.add(createTemplate(
            "sftp-server",
            "SFTP File Server",
            "SFTP server for file upload/download testing",
            "SFTP",
            createSftpEndpoint()
        ));

        // Message Queue Template
        templates.add(createTemplate(
            "message-queue",
            "Message Queue",
            "AMQP/RabbitMQ message queue for testing",
            "AMQP",
            createAmqpEndpoint()
        ));

        // SQL Database Template
        templates.add(createTemplate(
            "sql-database",
            "SQL Database",
            "PostgreSQL database with sample schema",
            "SQL",
            createSqlEndpoint()
        ));

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
            case "sftp-server" -> createSftpEndpoint();
            case "message-queue" -> createAmqpEndpoint();
            case "sql-database" -> createSqlEndpoint();
            default -> throw new NotFoundException("Template not found: " + templateId);
        };
    }

    private Map<String, Object> createTemplate(String id, String name, String description, String protocol, MockEndpoint endpoint) {
        Map<String, Object> template = new HashMap<>();
        template.put("id", id);
        template.put("name", name);
        template.put("description", description);
        template.put("protocol", protocol);
        template.put("endpoint", endpoint);
        return template;
    }

    private MockEndpoint createRestApiEndpoint() {
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("REST API Mock");
        endpoint.setDescription("Standard REST API with CRUD operations");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);

        HttpConfig httpConfig = new HttpConfig();
        httpConfig.setMethod(nl.blockmock.domain.HttpMethod.GET);
        httpConfig.setPath("/api/users");
        httpConfig.setPathRegex(false);
        endpoint.setHttpConfig(httpConfig);

        List<MockResponse> responses = new ArrayList<>();

        // Success response
        MockResponse response = new MockResponse();
        response.setName("Success Response");
        response.setPriority(0);
        response.setResponseStatusCode(200);
        response.setResponseBody("{\"users\": [{\"id\": 1, \"name\": \"John Doe\"}]}");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setResponseHeaders(headers);
        response.setResponseDelayMs(0);
        responses.add(response);

        endpoint.setResponses(responses);
        return endpoint;
    }

    private MockEndpoint createGraphQLEndpoint() {
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("GraphQL API Mock");
        endpoint.setDescription("GraphQL endpoint with query support");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);

        HttpConfig httpConfig = new HttpConfig();
        httpConfig.setMethod(nl.blockmock.domain.HttpMethod.POST);
        httpConfig.setPath("/graphql");
        httpConfig.setPathRegex(false);
        endpoint.setHttpConfig(httpConfig);

        List<MockResponse> responses = new ArrayList<>();

        MockResponse response = new MockResponse();
        response.setName("GraphQL Response");
        response.setPriority(0);
        response.setResponseStatusCode(200);
        response.setResponseBody("{\"data\": {\"user\": {\"id\": \"1\", \"name\": \"John Doe\"}}}");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setResponseHeaders(headers);
        responses.add(response);

        endpoint.setResponses(responses);
        return endpoint;
    }

    private MockEndpoint createOAuth2Endpoint() {
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("OAuth2 Token Endpoint");
        endpoint.setDescription("OAuth2 token endpoint for authentication testing");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);

        HttpConfig httpConfig = new HttpConfig();
        httpConfig.setMethod(nl.blockmock.domain.HttpMethod.POST);
        httpConfig.setPath("/oauth/token");
        httpConfig.setPathRegex(false);
        endpoint.setHttpConfig(httpConfig);

        List<MockResponse> responses = new ArrayList<>();

        MockResponse response = new MockResponse();
        response.setName("Token Response");
        response.setPriority(0);
        response.setResponseStatusCode(200);
        response.setResponseBody("{\"access_token\": \"mock_token_12345\", \"token_type\": \"Bearer\", \"expires_in\": 3600}");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setResponseHeaders(headers);
        responses.add(response);

        endpoint.setResponses(responses);
        return endpoint;
    }

    private MockEndpoint createWebhookEndpoint() {
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("Webhook Receiver");
        endpoint.setDescription("Receives and logs webhook events");
        endpoint.setProtocol(ProtocolType.HTTP);
        endpoint.setPattern(PatternType.FIRE_FORGET);
        endpoint.setEnabled(true);

        HttpConfig httpConfig = new HttpConfig();
        httpConfig.setMethod(nl.blockmock.domain.HttpMethod.POST);
        httpConfig.setPath("/webhook/events");
        httpConfig.setPathRegex(false);
        endpoint.setHttpConfig(httpConfig);

        List<MockResponse> responses = new ArrayList<>();

        MockResponse response = new MockResponse();
        response.setName("Webhook Accepted");
        response.setPriority(0);
        response.setResponseStatusCode(202);
        response.setResponseBody("{\"status\": \"accepted\", \"message\": \"Event received\"}");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        response.setResponseHeaders(headers);
        responses.add(response);

        endpoint.setResponses(responses);
        return endpoint;
    }

    private MockEndpoint createSftpEndpoint() {
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("SFTP File Server");
        endpoint.setDescription("SFTP server for file operations testing");
        endpoint.setProtocol(ProtocolType.SFTP);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);

        SftpConfig sftpConfig = new SftpConfig();
        sftpConfig.setPort(2222);
        sftpConfig.setOperation(SftpOperation.UPLOAD);
        sftpConfig.setPathPattern("/uploads/*");
        sftpConfig.setPathIsRegex(false);
        sftpConfig.setUsername("testuser");
        sftpConfig.setPassword("testpass");
        sftpConfig.setAllowAnonymous(false);
        sftpConfig.setSuccess(true);
        endpoint.setSftpConfig(sftpConfig);

        endpoint.setResponses(new ArrayList<>());
        return endpoint;
    }

    private MockEndpoint createAmqpEndpoint() {
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("Message Queue");
        endpoint.setDescription("AMQP message queue for testing");
        endpoint.setProtocol(ProtocolType.AMQP);
        endpoint.setPattern(PatternType.PUB_SUB);
        endpoint.setEnabled(true);

        AmqpConfig amqpConfig = new AmqpConfig();
        amqpConfig.setHost("localhost");
        amqpConfig.setPort(5672);
        amqpConfig.setVirtualHost("/");
        amqpConfig.setOperation(AmqpOperation.CONSUME);
        amqpConfig.setExchangeName("test.exchange");
        amqpConfig.setExchangeType(AmqpExchangeType.TOPIC);
        amqpConfig.setExchangeDurable(true);
        amqpConfig.setQueueName("test.queue");
        amqpConfig.setQueueDurable(true);
        amqpConfig.setRoutingKey("test.#");
        amqpConfig.setAutoReply(false);
        endpoint.setAmqpConfig(amqpConfig);

        endpoint.setResponses(new ArrayList<>());
        return endpoint;
    }

    private MockEndpoint createSqlEndpoint() {
        MockEndpoint endpoint = new MockEndpoint();
        endpoint.setName("SQL Database");
        endpoint.setDescription("PostgreSQL database with sample schema");
        endpoint.setProtocol(ProtocolType.SQL);
        endpoint.setPattern(PatternType.REQUEST_REPLY);
        endpoint.setEnabled(true);

        SqlConfig sqlConfig = new SqlConfig();
        sqlConfig.setDatabaseType(SqlDatabaseType.POSTGRESQL);
        sqlConfig.setDatabaseName("testdb");
        sqlConfig.setUsername("testuser");
        sqlConfig.setPassword("testpass");

        String initScript = """
            CREATE TABLE users (
                id SERIAL PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            INSERT INTO users (name, email) VALUES
            ('John Doe', 'john@example.com'),
            ('Jane Smith', 'jane@example.com');

            CREATE OR REPLACE FUNCTION get_user_count()
            RETURNS INTEGER AS $$
            BEGIN
                RETURN (SELECT COUNT(*) FROM users);
            END;
            $$ LANGUAGE plpgsql;
            """;

        sqlConfig.setInitScript(initScript);
        sqlConfig.setQueryMocks(new ArrayList<>());
        endpoint.setSqlConfig(sqlConfig);

        endpoint.setResponses(new ArrayList<>());
        return endpoint;
    }
}
