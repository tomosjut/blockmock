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
    private static final String VERSION = "2";

    @Inject
    BlockService blockService;

    // -------------------------------------------------------------------------
    // Export
    // -------------------------------------------------------------------------

    public TestSuiteExport export(Long suiteId) {
        TestSuite suite = TestSuite.findById(suiteId);
        if (suite == null) throw new IllegalArgumentException("TestSuite not found: " + suiteId);

        // Collect all endpoints referenced by this suite (via blocks + scenario expectations)
        Map<Long, MockEndpoint> endpointMap = new LinkedHashMap<>();
        for (Block block : suite.getBlocks()) {
            for (MockEndpoint ep : blockService.getBlockEndpoints(block.id)) {
                endpointMap.put(ep.id, ep);
            }
        }
        for (TestScenario scenario : suite.getScenarios()) {
            for (TestExpectation exp : scenario.getExpectations()) {
                if (exp.getMockEndpoint() != null) {
                    endpointMap.put(exp.getMockEndpoint().id, exp.getMockEndpoint());
                }
            }
        }

        List<TestSuiteExport.EndpointExport> endpoints = endpointMap.values().stream()
                .map(this::toEndpointExport).toList();

        List<TestSuiteExport.BlockExport> blocks = suite.getBlocks().stream()
                .map(block -> {
                    List<String> epKeys = blockService.getBlockEndpoints(block.id).stream()
                            .map(ep -> ep.getHttpMethod().name() + ":" + ep.getHttpPath())
                            .toList();
                    return new TestSuiteExport.BlockExport(block.getName(), block.getDescription(), block.getColor(), epKeys);
                }).toList();

        List<TestSuiteExport.ScenarioExport> scenarios = suite.getScenarios().stream()
                .map(scenario -> {
                    List<TestSuiteExport.ExpectationExport> expectations = scenario.getExpectations().stream()
                            .map(exp -> new TestSuiteExport.ExpectationExport(
                                    exp.getName(),
                                    exp.getMockEndpoint() != null ? exp.getMockEndpoint().getHttpMethod().name() : null,
                                    exp.getMockEndpoint() != null ? exp.getMockEndpoint().getHttpPath() : null,
                                    exp.getMinCallCount(), exp.getMaxCallCount(),
                                    exp.getRequiredBodyContains(), exp.getRequiredHeaders(),
                                    exp.getExpectationOrder()
                            )).toList();

                    List<TestSuiteExport.TriggerExport> triggers =
                            TriggerConfig.<TriggerConfig>list("testScenario", scenario).stream()
                            .map(t -> new TestSuiteExport.TriggerExport(
                                    t.getName(), t.getDescription(), t.getType().name(),
                                    t.getHttpUrl(), t.getHttpMethod(), t.getHttpBody(),
                                    t.getHttpHeaders(), t.getCronExpression(), t.getEnabled()
                            )).toList();

                    List<TestSuiteExport.OverrideExport> overrides = scenario.getResponseOverrides().stream()
                            .filter(o -> o.getMockEndpoint() != null && o.getMockResponse() != null)
                            .map(o -> new TestSuiteExport.OverrideExport(
                                    o.getMockEndpoint().getHttpMethod().name(),
                                    o.getMockEndpoint().getHttpPath(),
                                    o.getMockResponse().getName()
                            )).toList();

                    return new TestSuiteExport.ScenarioExport(
                            scenario.getName(), scenario.getDescription(), expectations, triggers, overrides);
                }).toList();

        return new TestSuiteExport(
                VERSION,
                LocalDateTime.now().toString(),
                new TestSuiteExport.SuiteExport(suite.getName(), suite.getDescription(), suite.getColor()),
                endpoints,
                blocks,
                scenarios
        );
    }

    private TestSuiteExport.EndpointExport toEndpointExport(MockEndpoint ep) {
        List<TestSuiteExport.ResponseExport> responses = ep.getResponses().stream()
                .map(r -> new TestSuiteExport.ResponseExport(
                        r.getName(), r.getPriority(), r.getResponseStatusCode(),
                        r.getResponseBody(), r.getResponseDelayMs(),
                        r.getMatchBody(), r.getMatchHeaders(), r.getMatchQueryParams(),
                        r.getResponseHeaders()
                )).toList();
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

        // 1. Endpoints
        Map<String, MockEndpoint> endpointByKey = new HashMap<>();
        for (TestSuiteExport.EndpointExport epExport : export.endpoints()) {
            String key = epExport.httpMethod() + ":" + epExport.httpPath();
            MockEndpoint existing = findEndpointByKey(epExport.httpMethod(), epExport.httpPath());
            if (existing != null) {
                endpointByKey.put(key, existing);
                result.endpointsLinked++;
            } else {
                MockEndpoint created = createEndpoint(epExport);
                endpointByKey.put(key, created);
                result.endpointsCreated++;
            }
        }

        // 2. Blocks
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
            } else {
                result.blocksLinked++;
            }
            for (String epKey : blockExport.endpointKeys()) {
                MockEndpoint ep = endpointByKey.get(epKey);
                if (ep != null) blockService.addEndpointToBlock(block.id, ep.id);
            }
            blockByName.put(blockExport.name(), block);
        }

        // 3. Test suite
        TestSuite suite = TestSuite.find("name", export.testSuite().name()).firstResult();
        boolean suiteCreated = suite == null;
        if (suite == null) {
            suite = new TestSuite();
            suite.setName(export.testSuite().name());
            result.suiteCreated = true;
        }
        suite.setDescription(export.testSuite().description());
        suite.setColor(export.testSuite().color() != null ? export.testSuite().color() : "#667eea");
        suite.getBlocks().clear();
        for (Block block : blockByName.values()) suite.getBlocks().add(block);
        suite.getScenarios().clear();

        // 4. Scenarios
        for (TestSuiteExport.ScenarioExport scenarioExport : export.scenarios()) {
            TestScenario scenario = new TestScenario();
            scenario.setName(scenarioExport.name());
            scenario.setDescription(scenarioExport.description());
            scenario.setTestSuite(suite);

            for (TestSuiteExport.ExpectationExport expExport : scenarioExport.expectations()) {
                TestExpectation exp = new TestExpectation();
                exp.setName(expExport.name());
                exp.setMinCallCount(expExport.minCallCount() != null ? expExport.minCallCount() : 1);
                exp.setMaxCallCount(expExport.maxCallCount());
                exp.setRequiredBodyContains(expExport.requiredBodyContains());
                exp.setRequiredHeaders(expExport.requiredHeaders());
                exp.setExpectationOrder(expExport.expectationOrder());
                exp.setTestScenario(scenario);
                if (expExport.endpointMethod() != null && expExport.endpointPath() != null) {
                    exp.setMockEndpoint(endpointByKey.get(expExport.endpointMethod() + ":" + expExport.endpointPath()));
                }
                scenario.getExpectations().add(exp);
            }

            suite.getScenarios().add(scenario);
        }

        if (suiteCreated) suite.persist();

        // 5. Triggers + overrides — per scenario
        for (TestSuiteExport.ScenarioExport scenarioExport : export.scenarios()) {
            TestScenario scenario = suite.getScenarios().stream()
                    .filter(s -> s.getName().equals(scenarioExport.name()))
                    .findFirst().orElse(null);
            if (scenario == null || scenario.id == null) continue;

            for (TestSuiteExport.TriggerExport tExport : scenarioExport.triggers()) {
                long existing = TriggerConfig.count("testScenario = ?1 and name = ?2", scenario, tExport.name());
                if (existing > 0) { result.triggersSkipped++; continue; }
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
                trigger.setTestScenario(scenario);
                trigger.persist();
                result.triggersCreated++;
            }

            for (TestSuiteExport.OverrideExport oExport : scenarioExport.responseOverrides()) {
                MockEndpoint ep = findEndpointByKey(oExport.endpointMethod(), oExport.endpointPath());
                if (ep == null) continue;
                MockResponse resp = ep.getResponses().stream()
                        .filter(r -> oExport.responseName().equals(r.getName()))
                        .findFirst().orElse(null);
                if (resp == null) continue;
                ScenarioResponseOverride override = new ScenarioResponseOverride();
                override.setTestScenario(scenario);
                override.setMockEndpoint(ep);
                override.setMockResponse(resp);
                override.persist();
            }
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
            List<ScenarioExport> scenarios
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

        public record ScenarioExport(
                String name, String description,
                List<ExpectationExport> expectations,
                List<TriggerExport> triggers,
                List<OverrideExport> responseOverrides
        ) {}

        public record OverrideExport(
                String endpointMethod, String endpointPath, String responseName
        ) {}

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
