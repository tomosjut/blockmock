package nl.blockmock.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.*;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.*;

@ApplicationScoped
public class TestSuiteExportService {

    private static final Logger LOG = Logger.getLogger(TestSuiteExportService.class);
    private static final String VERSION = "1";

    @Inject
    BlockService blockService;

    // -------------------------------------------------------------------------
    // Export
    // -------------------------------------------------------------------------

    public TestSuiteExport export(Long suiteId) {
        TestSuite suite = TestSuite.findById(suiteId);
        if (suite == null) throw new IllegalArgumentException("TestSuite not found: " + suiteId);

        // Collect all endpoints referenced by this suite (via blocks + expectations)
        Map<Long, MockEndpoint> endpointMap = new LinkedHashMap<>();
        for (Block block : suite.getBlocks()) {
            Set<MockEndpoint> blockEndpoints = blockService.getBlockEndpoints(block.id);
            for (MockEndpoint ep : blockEndpoints) {
                endpointMap.put(ep.id, ep);
            }
        }
        // Also endpoints from expectations that may not be in a block
        for (TestExpectation exp : suite.getExpectations()) {
            if (exp.getMockEndpoint() != null) {
                endpointMap.put(exp.getMockEndpoint().id, exp.getMockEndpoint());
            }
        }

        List<TestSuiteExport.EndpointExport> endpoints = endpointMap.values().stream()
                .map(this::toEndpointExport)
                .toList();

        List<TestSuiteExport.BlockExport> blocks = suite.getBlocks().stream()
                .map(block -> {
                    List<String> epKeys = blockService.getBlockEndpoints(block.id).stream()
                            .map(ep -> ep.getHttpMethod().name() + ":" + ep.getHttpPath())
                            .toList();
                    return new TestSuiteExport.BlockExport(block.getName(), block.getDescription(), block.getColor(), epKeys);
                })
                .toList();

        List<TestSuiteExport.ExpectationExport> expectations = suite.getExpectations().stream()
                .map(exp -> new TestSuiteExport.ExpectationExport(
                        exp.getName(),
                        exp.getMockEndpoint() != null ? exp.getMockEndpoint().getHttpMethod().name() : null,
                        exp.getMockEndpoint() != null ? exp.getMockEndpoint().getHttpPath() : null,
                        exp.getMinCallCount(),
                        exp.getMaxCallCount(),
                        exp.getRequiredBodyContains(),
                        exp.getRequiredHeaders(),
                        exp.getExpectationOrder()
                ))
                .toList();

        List<TestSuiteExport.TriggerExport> triggers = TriggerConfig.<TriggerConfig>list("testSuite", suite)
                .stream()
                .map(t -> new TestSuiteExport.TriggerExport(
                        t.getName(), t.getDescription(), t.getType().name(),
                        t.getHttpUrl(), t.getHttpMethod(), t.getHttpBody(), t.getHttpHeaders(),
                        t.getCronExpression(), t.getEnabled()
                ))
                .toList();

        return new TestSuiteExport(
                VERSION,
                LocalDateTime.now().toString(),
                new TestSuiteExport.SuiteExport(suite.getName(), suite.getDescription(), suite.getColor()),
                endpoints,
                blocks,
                expectations,
                triggers
        );
    }

    private TestSuiteExport.EndpointExport toEndpointExport(MockEndpoint ep) {
        List<TestSuiteExport.ResponseExport> responses = ep.getResponses().stream()
                .map(r -> new TestSuiteExport.ResponseExport(
                        r.getName(), r.getPriority(), r.getResponseStatusCode(),
                        r.getResponseBody(), r.getResponseDelayMs(),
                        r.getMatchBody(), r.getMatchHeaders(), r.getMatchQueryParams(),
                        r.getResponseHeaders()
                ))
                .toList();
        return new TestSuiteExport.EndpointExport(
                ep.getName(), ep.getDescription(),
                ep.getHttpMethod().name(), ep.getHttpPath(),
                Boolean.TRUE.equals(ep.getHttpPathRegex()),
                ep.getPattern().name(),
                responses
        );
    }

    // -------------------------------------------------------------------------
    // Import
    // -------------------------------------------------------------------------

    @Transactional
    public ImportResult importSuite(TestSuiteExport export) {
        ImportResult result = new ImportResult();

        // 1. Endpoints — match by (method + path), create if missing
        Map<String, MockEndpoint> endpointByKey = new HashMap<>();
        for (TestSuiteExport.EndpointExport epExport : export.endpoints()) {
            String key = epExport.httpMethod() + ":" + epExport.httpPath();
            MockEndpoint existing = findEndpointByKey(epExport.httpMethod(), epExport.httpPath());
            if (existing != null) {
                endpointByKey.put(key, existing);
                result.endpointsLinked++;
                LOG.infof("Linked existing endpoint: %s %s", epExport.httpMethod(), epExport.httpPath());
            } else {
                MockEndpoint created = createEndpoint(epExport);
                endpointByKey.put(key, created);
                result.endpointsCreated++;
                LOG.infof("Created endpoint: %s %s", epExport.httpMethod(), epExport.httpPath());
            }
        }

        // 2. Blocks — match by name, create if missing, always sync endpoints
        Map<String, Block> blockByName = new HashMap<>();
        for (TestSuiteExport.BlockExport blockExport : export.blocks()) {
            Block block = Block.find("name", blockExport.name()).firstResult();
            if (block == null) {
                block = new Block();
                block.setName(blockExport.name());
                block.setDescription(blockExport.description());
                block.setColor(blockExport.color() != null ? blockExport.color() : "#667eea");
                block.persist();
                result.blocksCreated++;
                LOG.infof("Created block: %s", blockExport.name());
            } else {
                result.blocksLinked++;
                LOG.infof("Linked existing block: %s", blockExport.name());
            }
            // Add endpoints to block (idempotent via Set)
            for (String epKey : blockExport.endpointKeys()) {
                MockEndpoint ep = endpointByKey.get(epKey);
                if (ep != null) {
                    blockService.addEndpointToBlock(block.id, ep.id);
                }
            }
            blockByName.put(blockExport.name(), block);
        }

        // 3. Test suite — match by name, create or update
        TestSuite suite = TestSuite.find("name", export.testSuite().name()).firstResult();
        boolean suiteCreated = suite == null;
        if (suite == null) {
            suite = new TestSuite();
            suite.setName(export.testSuite().name());
            result.suiteCreated = true;
        }
        suite.setDescription(export.testSuite().description());
        suite.setColor(export.testSuite().color() != null ? export.testSuite().color() : "#667eea");

        // Set blocks
        suite.getBlocks().clear();
        for (Block block : blockByName.values()) {
            suite.getBlocks().add(block);
        }

        // Set expectations
        suite.getExpectations().clear();
        for (TestSuiteExport.ExpectationExport expExport : export.expectations()) {
            TestExpectation exp = new TestExpectation();
            exp.setName(expExport.name());
            exp.setMinCallCount(expExport.minCallCount() != null ? expExport.minCallCount() : 1);
            exp.setMaxCallCount(expExport.maxCallCount());
            exp.setRequiredBodyContains(expExport.requiredBodyContains());
            exp.setRequiredHeaders(expExport.requiredHeaders());
            exp.setExpectationOrder(expExport.expectationOrder());
            exp.setTestSuite(suite);
            if (expExport.endpointMethod() != null && expExport.endpointPath() != null) {
                String key = expExport.endpointMethod() + ":" + expExport.endpointPath();
                exp.setMockEndpoint(endpointByKey.get(key));
            }
            suite.getExpectations().add(exp);
        }

        if (suiteCreated) {
            suite.persist();
        }

        // 4. Triggers — recreate (URLs are environment-specific, skip duplicates by name)
        for (TestSuiteExport.TriggerExport tExport : export.triggers()) {
            long existing = TriggerConfig.count("testSuite = ?1 and name = ?2", suite, tExport.name());
            if (existing > 0) {
                result.triggersSkipped++;
                continue;
            }
            TriggerConfig trigger = new TriggerConfig();
            trigger.setName(tExport.name());
            trigger.setDescription(tExport.description());
            trigger.setType(TriggerType.valueOf(tExport.type()));
            trigger.setHttpUrl(tExport.httpUrl());
            trigger.setHttpMethod(tExport.httpMethod());
            trigger.setHttpBody(tExport.httpBody());
            trigger.setHttpHeaders(tExport.httpHeaders());
            trigger.setCronExpression(tExport.cronExpression());
            trigger.setEnabled(tExport.enabled() != null ? tExport.enabled() : true);
            trigger.setTestSuite(suite);
            trigger.persist();
            result.triggersCreated++;
        }

        result.suiteName = suite.getName();
        result.suiteId = suite.id;
        return result;
    }

    private MockEndpoint findEndpointByKey(String method, String path) {
        return MockEndpoint.find("httpMethod = ?1 and httpPath = ?2",
                HttpMethod.valueOf(method), path).firstResult();
    }

    private MockEndpoint createEndpoint(TestSuiteExport.EndpointExport epExport) {
        MockEndpoint ep = new MockEndpoint();
        ep.setName(epExport.name());
        ep.setDescription(epExport.description());
        ep.setProtocol(ProtocolType.HTTP);
        ep.setPattern(PatternType.valueOf(epExport.pattern() != null ? epExport.pattern() : "REQUEST_REPLY"));
        ep.setEnabled(true);
        ep.setHttpMethod(HttpMethod.valueOf(epExport.httpMethod()));
        ep.setHttpPath(epExport.httpPath());
        ep.setHttpPathRegex(epExport.httpPathRegex());

        for (TestSuiteExport.ResponseExport rExport : epExport.responses()) {
            MockResponse response = new MockResponse();
            response.setName(rExport.name());
            response.setPriority(rExport.priority() != null ? rExport.priority() : 0);
            response.setResponseStatusCode(rExport.responseStatusCode() != null ? rExport.responseStatusCode() : 200);
            response.setResponseBody(rExport.responseBody());
            response.setResponseDelayMs(rExport.responseDelayMs());
            response.setMatchBody(rExport.matchBody());
            response.setMatchHeaders(rExport.matchHeaders());
            response.setMatchQueryParams(rExport.matchQueryParams());
            response.setResponseHeaders(rExport.responseHeaders());
            ep.addResponse(response);
        }
        ep.persist();
        return ep;
    }

    // -------------------------------------------------------------------------
    // DTOs
    // -------------------------------------------------------------------------

    public record TestSuiteExport(
            String version,
            String exportedAt,
            SuiteExport testSuite,
            List<EndpointExport> endpoints,
            List<BlockExport> blocks,
            List<ExpectationExport> expectations,
            List<TriggerExport> triggers
    ) {
        public record SuiteExport(String name, String description, String color) {}

        public record EndpointExport(
                String name, String description,
                String httpMethod, String httpPath, boolean httpPathRegex,
                String pattern,
                List<ResponseExport> responses
        ) {}

        public record ResponseExport(
                String name, Integer priority, Integer responseStatusCode,
                String responseBody, Integer responseDelayMs,
                String matchBody, Map<String, String> matchHeaders,
                Map<String, String> matchQueryParams, Map<String, String> responseHeaders
        ) {}

        public record BlockExport(String name, String description, String color, List<String> endpointKeys) {}

        public record ExpectationExport(
                String name,
                String endpointMethod, String endpointPath,
                Integer minCallCount, Integer maxCallCount,
                String requiredBodyContains, Map<String, String> requiredHeaders,
                Integer expectationOrder
        ) {}

        public record TriggerExport(
                String name, String description, String type,
                String httpUrl, String httpMethod, String httpBody,
                Map<String, String> httpHeaders,
                String cronExpression, Boolean enabled
        ) {}
    }

    public static class ImportResult {
        public Long suiteId;
        public String suiteName;
        public boolean suiteCreated;
        public int endpointsCreated;
        public int endpointsLinked;
        public int blocksCreated;
        public int blocksLinked;
        public int triggersCreated;
        public int triggersSkipped;
    }
}
