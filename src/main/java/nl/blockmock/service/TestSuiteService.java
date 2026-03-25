package nl.blockmock.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.*;
import org.jboss.logging.Logger;
import java.util.Set;

import java.time.LocalDateTime;
import java.util.*;

@ApplicationScoped
public class TestSuiteService {

    private static final Logger LOG = Logger.getLogger(TestSuiteService.class);

    @Inject
    BlockService blockService;

    @Inject
    RequestLogService requestLogService;

    // -------------------------------------------------------------------------
    // Suite CRUD
    // -------------------------------------------------------------------------

    public List<TestSuite> findAll() {
        return TestSuite.listAll();
    }

    public TestSuite findById(Long id) {
        return TestSuite.findById(id);
    }

    @Transactional
    public TestSuite create(TestSuite suite) {
        if (suite.getScenarios() != null) {
            for (TestScenario scenario : suite.getScenarios()) {
                scenario.setTestSuite(suite);
                for (TestExpectation exp : scenario.getExpectations()) {
                    exp.setTestScenario(scenario);
                }
            }
        }
        suite.persist();
        return suite;
    }

    @Transactional
    public TestSuite update(Long id, TestSuite updates) {
        TestSuite suite = TestSuite.findById(id);
        if (suite == null) throw new IllegalArgumentException("TestSuite not found: " + id);

        suite.setName(updates.getName());
        suite.setDescription(updates.getDescription());
        suite.setColor(updates.getColor());

        suite.getBlocks().clear();
        if (updates.getBlocks() != null) {
            for (Block block : updates.getBlocks()) {
                Block managed = Block.findById(block.id);
                if (managed != null) suite.getBlocks().add(managed);
            }
        }

        suite.persist();
        return suite;
    }

    @Transactional
    public void delete(Long id) {
        TestSuite suite = TestSuite.findById(id);
        if (suite != null) suite.delete();
    }

    // -------------------------------------------------------------------------
    // Scenario CRUD
    // -------------------------------------------------------------------------

    public List<TestScenario> findScenarios(Long suiteId) {
        return TestScenario.list("testSuite.id = ?1", suiteId);
    }

    public TestScenario findScenario(Long suiteId, Long scenarioId) {
        TestScenario scenario = TestScenario.findById(scenarioId);
        if (scenario == null || !scenario.getTestSuite().id.equals(suiteId)) return null;
        return scenario;
    }

    @Transactional
    public TestScenario createScenario(Long suiteId, TestScenario scenario) {
        TestSuite suite = TestSuite.findById(suiteId);
        if (suite == null) throw new IllegalArgumentException("TestSuite not found: " + suiteId);
        scenario.setTestSuite(suite);
        for (TestExpectation exp : scenario.getExpectations()) {
            exp.setTestScenario(scenario);
            resolveEndpoint(exp);
        }
        for (ScenarioResponseOverride override : scenario.getResponseOverrides()) {
            override.setTestScenario(scenario);
            resolveOverride(override);
        }
        scenario.persist();
        return scenario;
    }

    @Transactional
    public TestScenario updateScenario(Long suiteId, Long scenarioId, TestScenario updates) {
        TestScenario scenario = findScenario(suiteId, scenarioId);
        if (scenario == null) throw new IllegalArgumentException("TestScenario not found: " + scenarioId);

        scenario.setName(updates.getName());
        scenario.setDescription(updates.getDescription());

        scenario.getExpectations().clear();
        for (TestExpectation exp : updates.getExpectations()) {
            exp.setTestScenario(scenario);
            resolveEndpoint(exp);
            scenario.getExpectations().add(exp);
        }

        scenario.getResponseOverrides().clear();
        for (ScenarioResponseOverride override : updates.getResponseOverrides()) {
            override.setTestScenario(scenario);
            resolveOverride(override);
            scenario.getResponseOverrides().add(override);
        }

        scenario.persist();
        return scenario;
    }

    private void resolveEndpoint(TestExpectation exp) {
        if (exp.getMockEndpoint() != null && exp.getMockEndpoint().id != null) {
            exp.setMockEndpoint(MockEndpoint.findById(exp.getMockEndpoint().id));
        }
    }

    private void resolveOverride(ScenarioResponseOverride override) {
        if (override.getMockEndpoint() != null && override.getMockEndpoint().id != null) {
            override.setMockEndpoint(MockEndpoint.findById(override.getMockEndpoint().id));
        }
        if (override.getMockResponse() != null && override.getMockResponse().id != null) {
            override.setMockResponse(MockResponse.findById(override.getMockResponse().id));
        }
    }

    @Transactional
    public void deleteScenario(Long suiteId, Long scenarioId) {
        TestScenario scenario = findScenario(suiteId, scenarioId);
        if (scenario != null) scenario.delete();
    }

    // -------------------------------------------------------------------------
    // Runs
    // -------------------------------------------------------------------------

    @Transactional
    public TestRun startRun(Long suiteId, Long scenarioId) {
        TestScenario scenario = findScenario(suiteId, scenarioId);
        if (scenario == null) throw new IllegalArgumentException("TestScenario not found: " + scenarioId);

        // Enable all blocks in the suite
        for (Block block : scenario.getTestSuite().getBlocks()) {
            blockService.startBlock(block.id);
        }

        // Clear existing forced responses, then apply this scenario's overrides
        applyResponseOverrides(scenario);

        TestRun run = new TestRun();
        run.setTestScenario(scenario);
        run.setStatus(TestRunStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.persist();

        LOG.infof("Started run %d for scenario '%s'", run.id, scenario.getName());
        return run;
    }

    public List<TestRun> findRuns(Long suiteId, Long scenarioId) {
        TestScenario scenario = findScenario(suiteId, scenarioId);
        if (scenario == null) return List.of();
        return TestRun.list("testScenario.id = ?1", scenarioId);
    }

    public TestRun findRun(Long suiteId, Long scenarioId, Long runId) {
        TestRun run = TestRun.findById(runId);
        if (run == null || !run.getTestScenario().id.equals(scenarioId)) return null;
        return run;
    }

    @Transactional
    public TestRun completeRun(Long suiteId, Long scenarioId, Long runId) {
        TestRun run = TestRun.findById(runId);
        if (run == null || !run.getTestScenario().id.equals(scenarioId))
            throw new IllegalArgumentException("TestRun not found");
        if (run.getStatus() != TestRunStatus.RUNNING)
            throw new IllegalStateException("TestRun is not in RUNNING state");

        run.setCompletedAt(LocalDateTime.now());

        TestScenario scenario = run.getTestScenario();
        Map<Long, List<RequestLog>> logsByExpId = new HashMap<>();
        Map<Long, TestExpectationResult> resultsByExpId = new HashMap<>();

        for (TestExpectation expectation : scenario.getExpectations()) {
            List<RequestLog> logs = getMatchingLogs(expectation, run.getStartedAt(), run.getCompletedAt());
            TestExpectationResult result = evaluateExpectation(expectation, logs);
            result.setTestRun(run);
            result.persist();
            run.getResults().add(result);
            logsByExpId.put(expectation.id, logs);
            resultsByExpId.put(expectation.id, result);
        }

        checkSequenceOrder(scenario.getExpectations(), logsByExpId, resultsByExpId);

        boolean allPassed = run.getResults().stream().allMatch(TestExpectationResult::getPassed);
        run.setStatus(allPassed ? TestRunStatus.COMPLETED : TestRunStatus.FAILED);
        run.persist();

        clearResponseOverrides(scenario);
        stopBlocksIfNoActiveRuns(scenario, run.id);

        LOG.infof("Completed run %d with status: %s", run.id, run.getStatus());
        return run;
    }

    @Transactional
    public void cancelRun(Long suiteId, Long scenarioId, Long runId) {
        TestRun run = TestRun.findById(runId);
        if (run == null || !run.getTestScenario().id.equals(scenarioId))
            throw new IllegalArgumentException("TestRun not found");
        run.setStatus(TestRunStatus.CANCELLED);
        run.setCompletedAt(LocalDateTime.now());
        run.persist();

        clearResponseOverrides(run.getTestScenario());
        stopBlocksIfNoActiveRuns(run.getTestScenario(), run.id);
    }

    @Transactional
    public int clearCompletedRuns(Long suiteId, Long scenarioId) {
        List<TestRun> completed = TestRun.list(
                "testScenario.id = ?1 and status in ?2", scenarioId,
                List.of(TestRunStatus.COMPLETED, TestRunStatus.FAILED, TestRunStatus.CANCELLED));
        int count = completed.size();
        completed.forEach(r -> r.delete());
        return count;
    }

    public String generateJUnitXml(Long runId) {
        TestRun run = TestRun.findById(runId);
        if (run == null) throw new IllegalArgumentException("TestRun not found: " + runId);

        List<TestExpectationResult> results = run.getResults();
        long failures = results.stream().filter(r -> !r.getPassed()).count();
        double durationSeconds = 0;
        if (run.getCompletedAt() != null && run.getStartedAt() != null) {
            durationSeconds = java.time.Duration.between(run.getStartedAt(), run.getCompletedAt()).toMillis() / 1000.0;
        }

        String suiteName = run.getTestScenario().getTestSuite().getName()
                + " / " + run.getTestScenario().getName();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append(String.format(
            "<testsuite name=\"%s\" tests=\"%d\" failures=\"%d\" time=\"%.1f\">\n",
            escapeXml(suiteName), results.size(), failures, durationSeconds));

        for (TestExpectationResult result : results) {
            String name = result.getTestExpectation() != null
                    ? result.getTestExpectation().getName() : "Unknown";
            xml.append(String.format("    <testcase name=\"%s\" classname=\"blockmock\"",
                    escapeXml("Expectation: " + name)));
            if (result.getPassed()) {
                xml.append("/>\n");
            } else {
                xml.append(">\n");
                xml.append(String.format("        <failure message=\"%s\">%s</failure>\n",
                        escapeXml(result.getFailureReason() != null ? result.getFailureReason() : "Failed"),
                        escapeXml(result.getFailureReason() != null ? result.getFailureReason() : "")));
                xml.append("    </testcase>\n");
            }
        }
        xml.append("</testsuite>\n");
        return xml.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<RequestLog> getMatchingLogs(TestExpectation exp, LocalDateTime start, LocalDateTime end) {
        if (exp.getMockEndpoint() == null) return List.of();
        List<RequestLog> logs = RequestLog.list(
            "mockEndpoint.id = ?1 and receivedAt >= ?2 and receivedAt <= ?3",
            exp.getMockEndpoint().id, start, end);

        if (exp.getRequiredBodyContains() != null && !exp.getRequiredBodyContains().isEmpty()) {
            String required = exp.getRequiredBodyContains();
            logs = logs.stream()
                .filter(l -> l.getRequestBody() != null && l.getRequestBody().contains(required))
                .toList();
        }
        if (exp.getRequiredHeaders() != null && !exp.getRequiredHeaders().isEmpty()) {
            logs = logs.stream().filter(l -> {
                if (l.getRequestHeaders() == null) return false;
                for (var e : exp.getRequiredHeaders().entrySet()) {
                    String v = l.getRequestHeaders().get(e.getKey());
                    if (v == null || !v.equals(e.getValue())) return false;
                }
                return true;
            }).toList();
        }
        return logs;
    }

    private TestExpectationResult evaluateExpectation(TestExpectation exp, List<RequestLog> logs) {
        TestExpectationResult result = new TestExpectationResult();
        result.setTestExpectation(exp);
        if (exp.getMockEndpoint() == null) {
            result.setPassed(false);
            result.setActualCallCount(0);
            result.setFailureReason("No mock endpoint configured");
            return result;
        }
        int count = logs.size();
        result.setActualCallCount(count);
        if (count < exp.getMinCallCount()) {
            result.setPassed(false);
            result.setFailureReason("Expected min " + exp.getMinCallCount() + " calls, got " + count);
            return result;
        }
        if (exp.getMaxCallCount() != null && count > exp.getMaxCallCount()) {
            result.setPassed(false);
            result.setFailureReason("Expected max " + exp.getMaxCallCount() + " calls, got " + count);
            return result;
        }
        result.setPassed(true);
        return result;
    }

    private void checkSequenceOrder(List<TestExpectation> expectations,
            Map<Long, List<RequestLog>> logsByExpId,
            Map<Long, TestExpectationResult> resultsByExpId) {
        List<TestExpectation> ordered = expectations.stream()
            .filter(e -> e.getExpectationOrder() != null)
            .sorted(Comparator.comparingInt(TestExpectation::getExpectationOrder))
            .toList();
        if (ordered.isEmpty()) return;

        Map<Long, LocalDateTime> firstCallTimes = new HashMap<>();
        for (TestExpectation exp : ordered) {
            List<RequestLog> logs = logsByExpId.get(exp.id);
            if (logs != null && !logs.isEmpty()) {
                logs.stream().map(RequestLog::getReceivedAt).filter(t -> t != null)
                    .min(LocalDateTime::compareTo)
                    .ifPresent(t -> firstCallTimes.put(exp.id, t));
            }
        }

        TreeMap<Integer, List<TestExpectation>> groups = new TreeMap<>();
        for (TestExpectation exp : ordered) {
            groups.computeIfAbsent(exp.getExpectationOrder(), k -> new ArrayList<>()).add(exp);
        }

        LocalDateTime prevLatest = null;
        int prevStep = -1;
        for (var entry : groups.entrySet()) {
            int step = entry.getKey();
            List<TestExpectation> group = entry.getValue();
            LocalDateTime groupEarliest = null, groupLatest = null;
            for (TestExpectation exp : group) {
                LocalDateTime ft = firstCallTimes.get(exp.id);
                if (ft != null) {
                    if (groupEarliest == null || ft.isBefore(groupEarliest)) groupEarliest = ft;
                    if (groupLatest == null || ft.isAfter(groupLatest)) groupLatest = ft;
                }
            }
            if (prevLatest != null && groupEarliest != null && groupEarliest.isBefore(prevLatest)) {
                for (TestExpectation exp : group) {
                    TestExpectationResult result = resultsByExpId.get(exp.id);
                    if (result != null) {
                        result.setPassed(false);
                        LocalDateTime ft = firstCallTimes.get(exp.id);
                        result.setFailureReason(String.format(
                            "Volgorde-fout: eerste aanroep (%s) was vóór stap %d",
                            ft != null ? ft : "onbekend", prevStep));
                    }
                }
            }
            if (groupLatest != null) prevLatest = groupLatest;
            prevStep = step;
        }
    }

    private void applyResponseOverrides(TestScenario scenario) {
        // Collect all endpoint IDs in this suite's blocks
        Set<Long> suiteEndpointIds = new java.util.HashSet<>();
        for (Block block : scenario.getTestSuite().getBlocks()) {
            for (MockEndpoint ep : blockService.getBlockEndpoints(block.id)) {
                suiteEndpointIds.add(ep.id);
            }
        }

        // Clear forced responses for all suite endpoints
        for (Long epId : suiteEndpointIds) {
            MockEndpoint ep = MockEndpoint.findById(epId);
            if (ep != null && ep.getForcedResponse() != null) {
                ep.setForcedResponse(null);
                ep.persist();
            }
        }

        // Apply overrides for this scenario
        for (ScenarioResponseOverride override : scenario.getResponseOverrides()) {
            MockEndpoint ep = MockEndpoint.findById(override.getMockEndpoint().id);
            MockResponse resp = MockResponse.findById(override.getMockResponse().id);
            if (ep != null && resp != null) {
                ep.setForcedResponse(resp);
                ep.persist();
            }
        }
    }

    private void clearResponseOverrides(TestScenario scenario) {
        for (Block block : scenario.getTestSuite().getBlocks()) {
            for (MockEndpoint ep : blockService.getBlockEndpoints(block.id)) {
                MockEndpoint managed = MockEndpoint.findById(ep.id);
                if (managed != null && managed.getForcedResponse() != null) {
                    managed.setForcedResponse(null);
                    managed.persist();
                }
            }
        }
    }

    private void stopBlocksIfNoActiveRuns(TestScenario scenario, Long excludeRunId) {
        long otherRunning = TestRun.count(
            "testScenario.testSuite.id = ?1 and status = ?2 and id != ?3",
            scenario.getTestSuite().id, TestRunStatus.RUNNING, excludeRunId);
        if (otherRunning == 0) {
            for (Block block : scenario.getTestSuite().getBlocks()) {
                blockService.stopBlock(block.id);
            }
        }
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
