package nl.blockmock.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.blockmock.domain.*;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.*;

@ApplicationScoped
public class TestSuiteService {

    private static final Logger LOG = Logger.getLogger(TestSuiteService.class);

    @Inject
    BlockService blockService;

    @Inject
    RequestLogService requestLogService;

    public List<TestSuite> findAll() {
        return TestSuite.listAll();
    }

    public TestSuite findById(Long id) {
        return TestSuite.findById(id);
    }

    @Transactional
    public TestSuite create(TestSuite suite) {
        if (suite.getExpectations() != null) {
            for (TestExpectation expectation : suite.getExpectations()) {
                expectation.setTestSuite(suite);
            }
        }
        suite.persist();
        return suite;
    }

    @Transactional
    public TestSuite update(Long id, TestSuite updates) {
        TestSuite suite = TestSuite.findById(id);
        if (suite == null) {
            throw new IllegalArgumentException("TestSuite not found: " + id);
        }

        suite.setName(updates.getName());
        suite.setDescription(updates.getDescription());
        suite.setColor(updates.getColor());

        // Update blocks
        suite.getBlocks().clear();
        if (updates.getBlocks() != null) {
            for (Block block : updates.getBlocks()) {
                Block managed = Block.findById(block.id);
                if (managed != null) {
                    suite.getBlocks().add(managed);
                }
            }
        }

        // Update expectations
        suite.getExpectations().clear();
        if (updates.getExpectations() != null) {
            for (TestExpectation expectation : updates.getExpectations()) {
                expectation.setTestSuite(suite);
                suite.getExpectations().add(expectation);
            }
        }

        suite.persist();
        return suite;
    }

    @Transactional
    public void delete(Long id) {
        TestSuite suite = TestSuite.findById(id);
        if (suite != null) {
            suite.delete();
        }
    }

    @Transactional
    public TestRun startRun(Long suiteId) {
        TestSuite suite = TestSuite.findById(suiteId);
        if (suite == null) {
            throw new IllegalArgumentException("TestSuite not found: " + suiteId);
        }

        // Enable all blocks in this suite
        for (Block block : suite.getBlocks()) {
            blockService.startBlock(block.id);
        }

        // Create TestRun
        TestRun run = new TestRun();
        run.setTestSuite(suite);
        run.setStatus(TestRunStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.persist();

        LOG.info("Started test run " + run.id + " for suite: " + suite.getName());
        return run;
    }

    @Transactional
    public TestRun completeRun(Long suiteId, Long runId) {
        TestRun run = TestRun.findById(runId);
        if (run == null) {
            throw new IllegalArgumentException("TestRun not found: " + runId);
        }
        if (!run.getTestSuite().id.equals(suiteId)) {
            throw new IllegalArgumentException("TestRun does not belong to suite: " + suiteId);
        }
        if (run.getStatus() != TestRunStatus.RUNNING) {
            throw new IllegalStateException("TestRun is not in RUNNING state");
        }

        run.setCompletedAt(LocalDateTime.now());

        TestSuite suite = run.getTestSuite();
        Map<Long, List<RequestLog>> logsByExpId = new HashMap<>();
        Map<Long, TestExpectationResult> resultsByExpId = new HashMap<>();

        for (TestExpectation expectation : suite.getExpectations()) {
            List<RequestLog> logs = getMatchingLogs(expectation, run.getStartedAt(), run.getCompletedAt());
            TestExpectationResult result = evaluateExpectation(expectation, logs);
            result.setTestRun(run);
            result.persist();
            run.getResults().add(result);
            logsByExpId.put(expectation.id, logs);
            resultsByExpId.put(expectation.id, result);
        }

        checkSequenceOrder(suite.getExpectations(), logsByExpId, resultsByExpId);

        boolean allPassed = run.getResults().stream().allMatch(TestExpectationResult::getPassed);
        run.setStatus(allPassed ? TestRunStatus.COMPLETED : TestRunStatus.FAILED);
        run.persist();

        LOG.info("Completed test run " + run.id + " with status: " + run.getStatus());
        return run;
    }

    private List<RequestLog> getMatchingLogs(TestExpectation expectation, LocalDateTime start, LocalDateTime end) {
        if (expectation.getMockEndpoint() == null) {
            return List.of();
        }

        List<RequestLog> logs = RequestLog.list(
            "mockEndpoint.id = ?1 and receivedAt >= ?2 and receivedAt <= ?3",
            expectation.getMockEndpoint().id, start, end
        );

        // Apply body filter if configured
        if (expectation.getRequiredBodyContains() != null && !expectation.getRequiredBodyContains().isEmpty()) {
            String required = expectation.getRequiredBodyContains();
            logs = logs.stream()
                .filter(log -> log.getRequestBody() != null && log.getRequestBody().contains(required))
                .toList();
        }

        // Apply header filter if configured
        if (expectation.getRequiredHeaders() != null && !expectation.getRequiredHeaders().isEmpty()) {
            logs = logs.stream()
                .filter(log -> {
                    if (log.getRequestHeaders() == null) return false;
                    for (var entry : expectation.getRequiredHeaders().entrySet()) {
                        String actualValue = log.getRequestHeaders().get(entry.getKey());
                        if (actualValue == null || !actualValue.equals(entry.getValue())) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
        }

        return logs;
    }

    private TestExpectationResult evaluateExpectation(TestExpectation expectation, List<RequestLog> logs) {
        TestExpectationResult result = new TestExpectationResult();
        result.setTestExpectation(expectation);

        if (expectation.getMockEndpoint() == null) {
            result.setPassed(false);
            result.setActualCallCount(0);
            result.setFailureReason("No mock endpoint configured for this expectation");
            return result;
        }

        int callCount = logs.size();
        result.setActualCallCount(callCount);

        // Evaluate min call count
        if (callCount < expectation.getMinCallCount()) {
            result.setPassed(false);
            result.setFailureReason(
                "Expected min " + expectation.getMinCallCount() + " calls, got " + callCount
            );
            return result;
        }

        // Evaluate max call count
        if (expectation.getMaxCallCount() != null && callCount > expectation.getMaxCallCount()) {
            result.setPassed(false);
            result.setFailureReason(
                "Expected max " + expectation.getMaxCallCount() + " calls, got " + callCount
            );
            return result;
        }

        result.setPassed(true);
        return result;
    }

    private void checkSequenceOrder(
            List<TestExpectation> expectations,
            Map<Long, List<RequestLog>> logsByExpId,
            Map<Long, TestExpectationResult> resultsByExpId) {

        // Only consider expectations with an order value
        List<TestExpectation> ordered = expectations.stream()
            .filter(e -> e.getExpectationOrder() != null)
            .sorted(Comparator.comparingInt(TestExpectation::getExpectationOrder))
            .toList();

        if (ordered.isEmpty()) return;

        // Build firstCallTimes map: expectation.id → earliest receivedAt from matching logs
        Map<Long, LocalDateTime> firstCallTimes = new HashMap<>();
        for (TestExpectation exp : ordered) {
            List<RequestLog> logs = logsByExpId.get(exp.id);
            if (logs != null && !logs.isEmpty()) {
                logs.stream()
                    .map(RequestLog::getReceivedAt)
                    .filter(t -> t != null)
                    .min(LocalDateTime::compareTo)
                    .ifPresent(t -> firstCallTimes.put(exp.id, t));
            }
        }

        // Group by order value (TreeMap keeps ascending key order)
        TreeMap<Integer, List<TestExpectation>> groups = new TreeMap<>();
        for (TestExpectation exp : ordered) {
            groups.computeIfAbsent(exp.getExpectationOrder(), k -> new ArrayList<>()).add(exp);
        }

        LocalDateTime prevGroupLatestTime = null;
        int prevStep = -1;

        for (var entry : groups.entrySet()) {
            int step = entry.getKey();
            List<TestExpectation> group = entry.getValue();

            // Find earliest and latest first-call times within this group
            LocalDateTime groupEarliest = null;
            LocalDateTime groupLatest = null;

            for (TestExpectation exp : group) {
                LocalDateTime ft = firstCallTimes.get(exp.id);
                if (ft != null) {
                    if (groupEarliest == null || ft.isBefore(groupEarliest)) groupEarliest = ft;
                    if (groupLatest == null || ft.isAfter(groupLatest)) groupLatest = ft;
                }
            }

            // Constraint: earliest first-call of this group must be >= latest first-call of previous group
            if (prevGroupLatestTime != null && groupEarliest != null && groupEarliest.isBefore(prevGroupLatestTime)) {
                for (TestExpectation exp : group) {
                    TestExpectationResult result = resultsByExpId.get(exp.id);
                    if (result != null) {
                        result.setPassed(false);
                        LocalDateTime ft = firstCallTimes.get(exp.id);
                        result.setFailureReason(String.format(
                            "Volgorde-fout: eerste aanroep (%s) was vóór stap %d",
                            ft != null ? ft.toString() : "onbekend",
                            prevStep
                        ));
                    }
                }
            }

            if (groupLatest != null) {
                prevGroupLatestTime = groupLatest;
            }
            prevStep = step;
        }
    }

    public TestRun findRun(Long suiteId, Long runId) {
        TestRun run = TestRun.findById(runId);
        if (run == null || !run.getTestSuite().id.equals(suiteId)) {
            return null;
        }
        return run;
    }

    public List<TestRun> findRunsForSuite(Long suiteId) {
        return TestRun.list("testSuite.id = ?1", suiteId);
    }

    @Transactional
    public void cancelRun(Long suiteId, Long runId) {
        TestRun run = TestRun.findById(runId);
        if (run == null || !run.getTestSuite().id.equals(suiteId)) {
            throw new IllegalArgumentException("TestRun not found");
        }
        run.setStatus(TestRunStatus.CANCELLED);
        run.setCompletedAt(LocalDateTime.now());
        run.persist();
    }

    public String generateJUnitXml(Long runId) {
        TestRun run = TestRun.findById(runId);
        if (run == null) {
            throw new IllegalArgumentException("TestRun not found: " + runId);
        }

        List<TestExpectationResult> results = run.getResults();
        long failures = results.stream().filter(r -> !r.getPassed()).count();

        double durationSeconds = 0;
        if (run.getCompletedAt() != null && run.getStartedAt() != null) {
            durationSeconds = java.time.Duration.between(run.getStartedAt(), run.getCompletedAt()).toMillis() / 1000.0;
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append(String.format(
            "<testsuite name=\"%s\" tests=\"%d\" failures=\"%d\" time=\"%.1f\">\n",
            escapeXml(run.getTestSuite().getName()),
            results.size(),
            failures,
            durationSeconds
        ));

        for (TestExpectationResult result : results) {
            String expectationName = result.getTestExpectation() != null
                ? result.getTestExpectation().getName()
                : "Unknown";

            xml.append(String.format(
                "    <testcase name=\"%s\" classname=\"blockmock\"",
                escapeXml("Expectation: " + expectationName)
            ));

            if (result.getPassed()) {
                xml.append("/>\n");
            } else {
                xml.append(">\n");
                xml.append(String.format(
                    "        <failure message=\"%s\">%s</failure>\n",
                    escapeXml(result.getFailureReason() != null ? result.getFailureReason() : "Failed"),
                    escapeXml(result.getFailureReason() != null ? result.getFailureReason() : "")
                ));
                xml.append("    </testcase>\n");
            }
        }

        xml.append("</testsuite>\n");
        return xml.toString();
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
