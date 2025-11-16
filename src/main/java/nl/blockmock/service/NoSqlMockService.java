package nl.blockmock.service;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
@Startup
public class NoSqlMockService {

    private static final Logger LOG = Logger.getLogger(NoSqlMockService.class);

    public NoSqlMockService() {
        LOG.info("NoSQL Mock Service started - ready to configure NoSQL database endpoints");
    }

    // TODO: Implement MongoDB, Redis, Cassandra container management
    // For now, this is a placeholder to allow the application to compile and run
}
