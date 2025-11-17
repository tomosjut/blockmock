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
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.mina.MinaServiceFactoryFactory;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    // Cache SFTP credentials to avoid Hibernate deadlock during authentication
    private final Map<String, SftpCredential> sftpCredentialsCache = new ConcurrentHashMap<>();

    private static class SftpCredential {
        final String username;
        final String password;
        final boolean allowAnonymous;

        SftpCredential(String username, String password, boolean allowAnonymous) {
            this.username = username;
            this.password = password;
            this.allowAnonymous = allowAnonymous;
        }
    }

    void onStart(@Observes StartupEvent ev) {
        try {
            // Enable Apache SSHD debug logging
            System.setProperty("org.apache.sshd.common.util.logging", "TRACE");

            // Create SFTP root directory
            sftpRoot = Files.createTempDirectory("blockmock-sftp");
            LOG.info("SFTP root directory created at: " + sftpRoot);

            // Pre-cache SFTP credentials to avoid database queries during authentication
            cacheSftpCredentials();

            // Initialize SFTP server
            startSftpServer();
        } catch (IOException e) {
            LOG.error("Failed to start SFTP server", e);
        }
    }

    private void cacheSftpCredentials() {
        try {
            List<MockEndpoint> sftpEndpoints = mockEndpointService.findEnabledByProtocol(ProtocolType.SFTP);
            LOG.info("Caching credentials for " + sftpEndpoints.size() + " SFTP endpoints");

            for (MockEndpoint endpoint : sftpEndpoints) {
                SftpConfig config = endpoint.getSftpConfig();
                if (config != null) {
                    String username = config.getUsername() != null ? config.getUsername() : "anonymous";
                    SftpCredential credential = new SftpCredential(
                        config.getUsername(),
                        config.getPassword(),
                        config.getAllowAnonymous()
                    );
                    sftpCredentialsCache.put(username, credential);
                    LOG.info("Cached credentials for SFTP user: " + username);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to cache SFTP credentials", e);
        }
    }

    public void refreshCredentialsCache() {
        sftpCredentialsCache.clear();
        cacheSftpCredentials();
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
        try {
            sshServer = SshServer.setUpDefaultServer();
            sshServer.setPort(2222); // Default SFTP port
            sshServer.setHost("0.0.0.0"); // Listen on all interfaces

            // Use MINA acceptor instead of NIO2 for better stability
            sshServer.setIoServiceFactoryFactory(new MinaServiceFactoryFactory());
            LOG.info("Using MINA acceptor for SFTP server");

            // Generate host key - force regeneration to avoid corruption
            Path hostKeyPath = sftpRoot.resolve("hostkey.ser");
            SimpleGeneratorHostKeyProvider hostKeyProvider = new SimpleGeneratorHostKeyProvider(hostKeyPath);
            hostKeyProvider.setAlgorithm("RSA");
            hostKeyProvider.setKeySize(2048);
            hostKeyProvider.setOverwriteAllowed(true);
            sshServer.setKeyPairProvider(hostKeyProvider);

            // Log the configuration for debugging
            LOG.info("Using host key at: " + hostKeyPath);
            LOG.info("SFTP server configured with RSA 2048-bit host key on 0.0.0.0:2222 using MINA");

        // Set up password authenticator using cached credentials (no DB queries)
        PasswordAuthenticator authenticator = (username, password, session) -> {
            LOG.info("Authenticating SFTP user: " + username);

            SftpCredential credential = sftpCredentialsCache.get(username);
            if (credential == null) {
                LOG.warn("Authentication failed: unknown user '" + username + "'");
                return false;
            }

            // Check for anonymous access
            if (credential.allowAnonymous) {
                LOG.info("Anonymous access allowed for user: " + username);
                return true;
            }

            // Check password
            if (credential.password != null && credential.password.equals(password)) {
                LOG.info("Authentication successful for user: " + username);
                return true;
            }

            LOG.warn("Authentication failed: invalid password for user '" + username + "'");
            return false;
        };

        sshServer.setPasswordAuthenticator(authenticator);

        // Set up virtual file system
        sshServer.setFileSystemFactory(new VirtualFileSystemFactory(sftpRoot));

        // Enable SFTP subsystem
        sshServer.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));

            sshServer.start();
            LOG.info("SFTP server successfully started on " + sshServer.getHost() + ":" + sshServer.getPort());
            LOG.info("SFTP server is " + (sshServer.isStarted() ? "RUNNING" : "NOT RUNNING"));
            LOG.info("SFTP server acceptor: " + sshServer.getIoServiceFactoryFactory().getClass().getSimpleName());
        } catch (Exception e) {
            LOG.error("Failed to start SFTP server", e);
            throw e;
        }
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
