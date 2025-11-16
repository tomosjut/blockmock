package nl.blockmock.service;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import nl.blockmock.domain.*;
import org.jboss.logging.Logger;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.oracle.OracleContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SqlMockService {

    private static final Logger LOG = Logger.getLogger(SqlMockService.class);

    @Inject
    MockEndpointService mockEndpointService;

    @Inject
    RequestLogService requestLogService;

    private final Map<Long, JdbcDatabaseContainer<?>> containers = new ConcurrentHashMap<>();
    private final Map<Long, String> connectionStrings = new ConcurrentHashMap<>();

    void onStart(@Observes StartupEvent ev) {
        LOG.info("SQL Mock Service started - ready to configure database endpoints");
        initializeSqlMocks();
    }

    void onStop(@Observes ShutdownEvent ev) {
        containers.values().forEach(container -> {
            try {
                if (container.isRunning()) {
                    container.stop();
                }
            } catch (Exception e) {
                LOG.warn("Error stopping database container", e);
            }
        });
        LOG.info("SQL Mock Service stopped");
    }

    public void initializeSqlMocks() {
        var sqlEndpoints = mockEndpointService.findEnabledByProtocol(ProtocolType.SQL);
        for (MockEndpoint endpoint : sqlEndpoints) {
            try {
                startSqlMock(endpoint);
            } catch (Exception e) {
                LOG.error("Failed to start SQL mock for endpoint: " + endpoint.getName(), e);
            }
        }
    }

    public void startSqlMock(MockEndpoint endpoint) throws Exception {
        if (endpoint.getSqlConfig() == null) {
            LOG.warn("No SQL config found for endpoint: " + endpoint.getName());
            return;
        }

        SqlConfig config = endpoint.getSqlConfig();
        JdbcDatabaseContainer<?> container = createContainer(config);

        // Start container
        container.start();

        // Store connection info
        containers.put(endpoint.id, container);
        String jdbcUrl = container.getJdbcUrl();
        connectionStrings.put(endpoint.id, jdbcUrl);

        LOG.info("Started " + config.getDatabaseType() + " container for endpoint: " + endpoint.getName());
        LOG.info("JDBC URL: " + jdbcUrl);
        LOG.info("Username: " + container.getUsername());
        LOG.info("Password: " + container.getPassword());

        // Run initialization script if provided
        if (config.getInitScript() != null && !config.getInitScript().isEmpty()) {
            executeInitScript(container, config.getInitScript());
        }

        // Execute query mocks to seed data
        executeQueryMocks(container, config);
    }

    private JdbcDatabaseContainer<?> createContainer(SqlConfig config) {
        return switch (config.getDatabaseType()) {
            case POSTGRESQL -> new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName(config.getDatabaseName())
                    .withUsername(config.getUsername() != null ? config.getUsername() : "test")
                    .withPassword(config.getPassword() != null ? config.getPassword() : "test");

            case MYSQL -> new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName(config.getDatabaseName())
                    .withUsername(config.getUsername() != null ? config.getUsername() : "test")
                    .withPassword(config.getPassword() != null ? config.getPassword() : "test");

            case SQL_SERVER -> new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest")
                    .acceptLicense()
                    .withPassword(config.getPassword() != null ? config.getPassword() : "Test1234!");

            case ORACLE -> new OracleContainer("gvenzl/oracle-free:23-slim-faststart")
                    .withDatabaseName(config.getDatabaseName())
                    .withUsername(config.getUsername() != null ? config.getUsername() : "test")
                    .withPassword(config.getPassword() != null ? config.getPassword() : "test");

            case GENERIC -> new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName(config.getDatabaseName())
                    .withUsername(config.getUsername() != null ? config.getUsername() : "test")
                    .withPassword(config.getPassword() != null ? config.getPassword() : "test");
        };
    }

    private void executeInitScript(JdbcDatabaseContainer<?> container, String script) {
        try (Connection conn = DriverManager.getConnection(
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword());
             Statement stmt = conn.createStatement()) {

            // Split by semicolon and execute each statement
            String[] statements = script.split(";");
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    LOG.debug("Executing init SQL: " + trimmed);
                    stmt.execute(trimmed);
                }
            }

            LOG.info("Successfully executed initialization script");
        } catch (Exception e) {
            LOG.error("Error executing initialization script", e);
        }
    }

    private void executeQueryMocks(JdbcDatabaseContainer<?> container, SqlConfig config) {
        if (config.getQueryMocks() == null || config.getQueryMocks().isEmpty()) {
            return;
        }

        try (Connection conn = DriverManager.getConnection(
                container.getJdbcUrl(),
                container.getUsername(),
                container.getPassword());
             Statement stmt = conn.createStatement()) {

            for (SqlQueryMock mock : config.getQueryMocks()) {
                if (!mock.getEnabled()) {
                    continue;
                }

                try {
                    // Execute the query pattern (useful for CREATE TABLE, INSERT data, etc.)
                    if (!mock.getQueryPattern().trim().isEmpty()) {
                        LOG.debug("Executing query mock: " + mock.getQueryPattern());
                        stmt.execute(mock.getQueryPattern());
                    }
                } catch (Exception e) {
                    LOG.warn("Error executing query mock: " + mock.getQueryPattern(), e);
                }
            }

            LOG.info("Successfully executed " + config.getQueryMocks().size() + " query mocks");
        } catch (Exception e) {
            LOG.error("Error executing query mocks", e);
        }
    }

    public void stopSqlMock(Long endpointId) {
        JdbcDatabaseContainer<?> container = containers.remove(endpointId);
        if (container != null && container.isRunning()) {
            container.stop();
            LOG.info("Stopped database container for endpoint: " + endpointId);
        }
        connectionStrings.remove(endpointId);
    }

    public String getConnectionString(Long endpointId) {
        return connectionStrings.get(endpointId);
    }

    public JdbcDatabaseContainer<?> getContainer(Long endpointId) {
        return containers.get(endpointId);
    }
}
