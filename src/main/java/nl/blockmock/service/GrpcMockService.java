package nl.blockmock.service;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
@Startup
public class GrpcMockService {

    private static final Logger LOG = Logger.getLogger(GrpcMockService.class);

    public GrpcMockService() {
        LOG.info("gRPC Mock Service started - ready to configure gRPC services");
    }

    // TODO: Implement gRPC server management
    // For now, this is a placeholder to allow the application to compile and run
}
