package nl.blockmock.service;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
@Startup
public class KafkaMockService {

    private static final Logger LOG = Logger.getLogger(KafkaMockService.class);

    public KafkaMockService() {
        LOG.info("Kafka Mock Service started - ready to configure Kafka topics");
    }

    // TODO: Implement Kafka container and topic management
    // For now, this is a placeholder to allow the application to compile and run
}
