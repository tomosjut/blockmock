package nl.blockmock.service;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import nl.blockmock.domain.*;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@ApplicationScoped
public class SftpMockService {

    private static final Logger LOG = Logger.getLogger(SftpMockService.class);

    @Inject
    MockEndpointService mockEndpointService;

    @Inject
    RequestLogService requestLogService;

    private SshServer sshServer;
    private Path sftpRoot;

    void onStart(@Observes StartupEvent ev) {
        try {
            // Create SFTP root directory
            sftpRoot = Files.createTempDirectory("blockmock-sftp");
            LOG.info("SFTP root directory created at: " + sftpRoot);

            // Initialize SFTP server
            startSftpServer();
        } catch (IOException e) {
            LOG.error("Failed to start SFTP server", e);
        }
    }

    void onStop(@Observes ShutdownEvent ev) {
        try {
            if (sshServer != null && sshServer.isStarted()) {
                sshServer.stop();
                LOG.info("SFTP server stopped");
            }
        } catch (IOException e) {
            LOG.error("Failed to stop SFTP server", e);
        }
    }

    private void startSftpServer() throws IOException {
        sshServer = SshServer.setUpDefaultServer();
        sshServer.setPort(2222); // Default SFTP port

        // Generate host key
        sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(sftpRoot.resolve("hostkey.ser")));

        // Set up password authenticator
        sshServer.setPasswordAuthenticator((username, password, session) -> {
            // Check if there's a mock endpoint that accepts this username/password
            List<MockEndpoint> sftpEndpoints = mockEndpointService.findEnabledByProtocol(ProtocolType.SFTP);

            for (MockEndpoint endpoint : sftpEndpoints) {
                SftpConfig config = endpoint.getSftpConfig();
                if (config != null) {
                    // Allow anonymous access if configured
                    if (config.getAllowAnonymous()) {
                        return true;
                    }

                    // Check username/password
                    if (config.getUsername() != null && config.getUsername().equals(username) &&
                        config.getPassword() != null && config.getPassword().equals(password)) {
                        return true;
                    }
                }
            }

            return false;
        });

        // Set up virtual file system
        sshServer.setFileSystemFactory(new VirtualFileSystemFactory(sftpRoot));

        // Enable SFTP subsystem
        sshServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));

        sshServer.start();
        LOG.info("SFTP server started on port " + sshServer.getPort());
    }

    public SftpConfig findMatchingEndpoint(String path, SftpOperation operation) {
        List<MockEndpoint> sftpEndpoints = mockEndpointService.findEnabledByProtocol(ProtocolType.SFTP);

        for (MockEndpoint endpoint : sftpEndpoints) {
            SftpConfig config = endpoint.getSftpConfig();
            if (config != null && config.getOperation() == operation) {
                if (matchesPath(path, config.getPathPattern(), config.getPathIsRegex())) {
                    return config;
                }
            }
        }

        return null;
    }

    private boolean matchesPath(String path, String pattern, boolean isRegex) {
        if (isRegex) {
            try {
                return Pattern.compile(pattern).matcher(path).matches();
            } catch (Exception e) {
                LOG.error("Invalid regex pattern: " + pattern, e);
                return false;
            }
        } else {
            // Simple wildcard matching: * matches any characters
            String regexPattern = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
            return Pattern.compile(regexPattern).matcher(path).matches();
        }
    }

    public void logRequest(String operation, String path, String username, boolean matched) {
        RequestLog log = new RequestLog();
        log.setProtocol(ProtocolType.SFTP);
        log.setRequestMethod(operation);
        log.setRequestPath(path);
        log.setClientIp(username); // Store username in clientIp field
        log.setMatched(matched);
        requestLogService.log(log);
    }
}
