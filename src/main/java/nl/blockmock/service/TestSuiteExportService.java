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
                            .map(ep -> {
                                if (ep instanceof HttpMockEndpoint http) {
                                    return "HTTP:" + http.getHttpMethod().name() + ":" + http.getHttpPath();
                                } else if (ep instanceof AmqpMockEndpoint amqp) {
                                    return "AMQP:" + amqp.getAmqpAddress();
                                }
                                return null;
                            })
                            .filter(java.util.Objects::nonNull)
                            .toList();
                    return new TestSuiteExport.BlockExport(block.getName(), block.getDescription(), block.getColor(), epKeys);
                }).toList();

        List<TestSuiteExport.ScenarioExport> scenarios = suite.getScenarios().stream()
                .map(scenario -> {
                    List<TestSuiteExport.ExpectationExport> expectations = scenario.getExpectations().stream()
                            .map(exp -> {
                                MockEndpoint ep = exp.getMockEndpoint();
                                String epMethod  = (ep instanceof HttpMockEndpoint http) ? http.getHttpMethod().name() : null;
                                String epPath    = (ep instanceof HttpMockEndpoint http) ? http.getHttpPath() : null;
                                String epAddress = (ep instanceof AmqpMockEndpoint amqp) ? amqp.getAmqpAddress() : null;
                                return new TestSuiteExport.ExpectationExport(
                                        exp.getName(), epMethod, epPath, epAddress,
                                        exp.getMinCallCount(), exp.getMaxCallCount(),
                                        exp.getRequiredBodyContains(), exp.getRequiredHeaders(),
                                        exp.getExpectationOrder()
                                );
                            }).toList();

                    List<TestSuiteExport.TriggerExport> triggers =
                            TriggerConfig.<TriggerConfig>list("testScenario", scenario).stream()
                            .map(t -> {
                                String httpUrl          = t instanceof HttpTriggerConfig h ? h.getHttpUrl() : null;
                                String httpMethod       = t instanceof HttpTriggerConfig h ? h.getHttpMethod() : null;
                                String httpBody         = t instanceof HttpTriggerConfig h ? h.getHttpBody() : null;
                                Map<String, String> httpHeaders = t instanceof HttpTriggerConfig h ? h.getHttpHeaders() : null;
                                String cronExpr         = t instanceof CronTriggerConfig c ? c.getCronExpression() : null;
                                String amqpAddress      = t instanceof AmqpTriggerConfig a ? a.getAmqpAddress() : null;
                                String amqpBody         = t instanceof AmqpTriggerConfig a ? a.getAmqpBody() : null;
                                Map<String, String> amqpProperties = t instanceof AmqpTriggerConfig a ? a.getAmqpProperties() : null;
                                String amqpRoutingType  = t instanceof AmqpTriggerConfig a ? a.getAmqpRoutingType() : null;
                                return new TestSuiteExport.TriggerExport(
                                        t.getName(), t.getDescription(), t.getType().name(),
                                        httpUrl, httpMethod, httpBody, httpHeaders,
                                        cronExpr, amqpAddress, amqpBody, amqpProperties, amqpRoutingType, t.getEnabled()
                                );
                            }).toList();

                    List<TestSuiteExport.OverrideExport> overrides = scenario.getResponseOverrides().stream()
                            .filter(o -> o.getMockEndpoint() instanceof HttpMockEndpoint && o.getMockResponse() != null)
                            .map(o -> {
                                HttpMockEndpoint http = (HttpMockEndpoint) o.getMockEndpoint();
                                return new TestSuiteExport.OverrideExport(
                                        http.getHttpMethod().name(), http.getHttpPath(),
                                        o.getMockResponse().getName()
                                );
                            }).toList();

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

        if (ep instanceof HttpMockEndpoint http) {
            return new TestSuiteExport.EndpointExport(
                    http.getName(), http.getDescription(),
                    "HTTP",
                    http.getHttpMethod().name(), http.getHttpPath(),
                    Boolean.TRUE.equals(http.getHttpPathRegex()),
                    null, null, null,
                    http.getPattern().name(), responses
            );
        }
        if (ep instanceof AmqpMockEndpoint amqp) {
            return new TestSuiteExport.EndpointExport(
                    amqp.getName(), amqp.getDescription(),
                    "AMQP",
                    null, null, false,
                    amqp.getAmqpAddress(),
                    amqp.getAmqpPattern(),
                    amqp.getAmqpRoutingType(),
                    amqp.getPattern().name(), responses
            );
        }
        return new TestSuiteExport.EndpointExport(
                ep.getName(), ep.getDescription(),
                null, null, null, false, null, null, null,
                ep.getPattern().name(), responses
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
            String key = endpointKey(epExport);
            MockEndpoint existing = findEndpointByExport(epExport);
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
                // Support both old format (METHOD:path) and new format (PROTOCOL:METHOD:path or AMQP:address)
                String normalizedKey = epKey.startsWith("HTTP:") || epKey.startsWith("AMQP:") ? epKey : "HTTP:" + epKey;
                MockEndpoint ep = endpointByKey.get(normalizedKey);
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
                    exp.setMockEndpoint(endpointByKey.get("HTTP:" + expExport.endpointMethod() + ":" + expExport.endpointPath()));
                } else if (expExport.endpointAmqpAddress() != null) {
                    exp.setMockEndpoint(endpointByKey.get("AMQP:" + expExport.endpointAmqpAddress()));
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

                TriggerType type = TriggerType.valueOf(tExport.type());
                TriggerConfig trigger = switch (type) {
                    case HTTP -> {
                        HttpTriggerConfig http = new HttpTriggerConfig();
                        http.setHttpUrl(tExport.httpUrl());
                        http.setHttpMethod(tExport.httpMethod());
                        http.setHttpBody(tExport.httpBody());
                        http.setHttpHeaders(tExport.httpHeaders());
                        yield http;
                    }
                    case CRON -> {
                        CronTriggerConfig cron = new CronTriggerConfig();
                        cron.setCronExpression(tExport.cronExpression());
                        yield cron;
                    }
                    case AMQP -> {
                        AmqpTriggerConfig amqp = new AmqpTriggerConfig();
                        amqp.setAmqpAddress(tExport.amqpAddress());
                        amqp.setAmqpBody(tExport.amqpBody());
                        amqp.setAmqpProperties(tExport.amqpProperties());
                        amqp.setAmqpRoutingType(tExport.amqpRoutingType() != null ? tExport.amqpRoutingType() : "ANYCAST");
                        yield amqp;
                    }
                };
                trigger.setName(tExport.name());
                trigger.setDescription(tExport.description());
                trigger.setType(type);
                trigger.setEnabled(tExport.enabled() != null ? tExport.enabled() : true);
                trigger.setTestScenario(scenario);
                trigger.persist();
                result.triggersCreated++;
            }

            for (TestSuiteExport.OverrideExport oExport : scenarioExport.responseOverrides()) {
                MockEndpoint ep = findHttpEndpointByKey(oExport.endpointMethod(), oExport.endpointPath());
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

    private String endpointKey(TestSuiteExport.EndpointExport ep) {
        if ("AMQP".equals(ep.protocol()) || "AMQPS".equals(ep.protocol())) {
            return "AMQP:" + ep.amqpAddress();
        }
        return "HTTP:" + ep.httpMethod() + ":" + ep.httpPath();
    }

    private MockEndpoint findEndpointByExport(TestSuiteExport.EndpointExport ep) {
        if ("AMQP".equals(ep.protocol()) || "AMQPS".equals(ep.protocol())) {
            return AmqpMockEndpoint.find("amqpAddress = ?1", ep.amqpAddress()).firstResult();
        }
        return findHttpEndpointByKey(ep.httpMethod(), ep.httpPath());
    }

    private MockEndpoint findHttpEndpointByKey(String method, String path) {
        return HttpMockEndpoint.find("httpMethod = ?1 and httpPath = ?2",
                HttpMethod.valueOf(method), path).firstResult();
    }

    private MockEndpoint createEndpoint(TestSuiteExport.EndpointExport epExport) {
        PatternType pattern = PatternType.valueOf(epExport.pattern() != null ? epExport.pattern() : "REQUEST_REPLY");
        List<TestSuiteExport.ResponseExport> responseExports = epExport.responses() != null ? epExport.responses() : List.of();

        if ("AMQP".equals(epExport.protocol()) || "AMQPS".equals(epExport.protocol())) {
            AmqpMockEndpoint ep = new AmqpMockEndpoint();
            ep.setName(epExport.name());
            ep.setDescription(epExport.description());
            ep.setProtocol(ProtocolType.AMQP);
            ep.setPattern(pattern);
            ep.setEnabled(true);
            ep.setAmqpAddress(epExport.amqpAddress());
            ep.setAmqpPattern(epExport.amqpPattern());
            ep.setAmqpRoutingType(epExport.amqpRoutingType() != null ? epExport.amqpRoutingType() : "ANYCAST");
            ep.persist();
            return ep;
        }

        HttpMockEndpoint ep = new HttpMockEndpoint();
        ep.setName(epExport.name());
        ep.setDescription(epExport.description());
        ep.setProtocol(ProtocolType.HTTP);
        ep.setPattern(pattern);
        ep.setEnabled(true);
        ep.setHttpMethod(HttpMethod.valueOf(epExport.httpMethod()));
        ep.setHttpPath(epExport.httpPath());
        ep.setHttpPathRegex(epExport.httpPathRegex());
        for (TestSuiteExport.ResponseExport rExport : responseExports) {
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
                String protocol,
                String httpMethod, String httpPath, boolean httpPathRegex,
                String amqpAddress, String amqpPattern, String amqpRoutingType,
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
                String endpointAmqpAddress,
                Integer minCallCount, Integer maxCallCount,
                String requiredBodyContains, Map<String, String> requiredHeaders,
                Integer expectationOrder
        ) {}

        public record TriggerExport(
                String name, String description, String type,
                String httpUrl, String httpMethod, String httpBody,
                Map<String, String> httpHeaders,
                String cronExpression,
                String amqpAddress, String amqpBody,
                Map<String, String> amqpProperties,
                String amqpRoutingType,
                Boolean enabled
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
