package nl.blockmock.service;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
@Startup
public class WebSocketMockService {

    private static final Logger LOG = Logger.getLogger(WebSocketMockService.class);

    public WebSocketMockService() {
        LOG.info("WebSocket Mock Service started - ready to configure WebSocket endpoints");
    }

    // TODO: Implement WebSocket endpoint management
    // For now, this is a placeholder to allow the application to compile and run
}
