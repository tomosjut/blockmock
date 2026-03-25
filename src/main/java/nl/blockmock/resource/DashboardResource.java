package nl.blockmock.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import nl.blockmock.domain.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api/dashboard")
@Produces(MediaType.APPLICATION_JSON)
public class DashboardResource {

    @GET
    public DashboardStats get() {
        // Endpoints
        List<MockEndpoint> endpoints = MockEndpoint.listAll();
        long matched = endpoints.stream().mapToLong(e -> e.getMatchedRequests() != null ? e.getMatchedRequests() : 0).sum();
        long unmatched = endpoints.stream().mapToLong(e -> e.getUnmatchedRequests() != null ? e.getUnmatchedRequests() : 0).sum();
        long active = endpoints.stream().filter(e -> Boolean.TRUE.equals(e.getEnabled())).count();

        // Suites + scenarios + last run per scenario
        List<TestSuite> suites = TestSuite.listAll();
        List<SuiteSummary> suiteSummaries = suites.stream().map(suite -> {
            List<TestScenario> scenarios = TestScenario.list("testSuite", suite);
            List<ScenarioSummary> scenarioSummaries = scenarios.stream().map(sc -> {
                TestRun last = TestRun.find("testScenario = ?1 order by startedAt desc", sc).firstResult();
                boolean activeRun = TestRun.count("testScenario = ?1 and status = ?2", sc, TestRunStatus.RUNNING) > 0;
                int passed = 0, total = 0;
                if (last != null && last.getResults() != null) {
                    total = last.getResults().size();
                    passed = (int) last.getResults().stream().filter(r -> Boolean.TRUE.equals(r.getPassed())).count();
                }
                return new ScenarioSummary(
                        sc.id, sc.getName(),
                        last != null ? last.getStatus().name() : null,
                        last != null ? last.getCompletedAt() != null ? last.getCompletedAt().toString() : last.getStartedAt().toString() : null,
                        activeRun, passed, total
                );
            }).toList();
            return new SuiteSummary(suite.id, suite.getName(), suite.getColor(), scenarioSummaries);
        }).toList();

        // Recent runs (last 20 across all scenarios)
        List<TestRun> recent = TestRun.find("order by startedAt desc").page(0, 20).list();
        List<RecentRun> recentRuns = recent.stream().map(run -> {
            TestScenario sc = run.getTestScenario();
            TestSuite suite = sc != null ? sc.getTestSuite() : null;
            int passed = 0, total = 0;
            if (run.getResults() != null) {
                total = run.getResults().size();
                passed = (int) run.getResults().stream().filter(r -> Boolean.TRUE.equals(r.getPassed())).count();
            }
            return new RecentRun(
                    run.id,
                    suite != null ? suite.id : null,
                    suite != null ? suite.getName() : "?",
                    suite != null ? suite.getColor() : null,
                    sc != null ? sc.id : null,
                    sc != null ? sc.getName() : "?",
                    run.getStatus().name(),
                    run.getStartedAt() != null ? run.getStartedAt().toString() : null,
                    run.getCompletedAt() != null ? run.getCompletedAt().toString() : null,
                    passed, total
            );
        }).toList();

        // Recent trigger fires
        List<TriggerConfig> triggers = TriggerConfig.find(
                "lastFiredAt is not null order by lastFiredAt desc").page(0, 10).list();
        List<RecentFire> recentFires = triggers.stream().map(t -> {
            String scenarioName = t.getTestScenario() != null ? t.getTestScenario().getName() : null;
            return new RecentFire(t.id, t.getName(), t.getType().name(), scenarioName,
                    t.getLastFiredAt() != null ? t.getLastFiredAt().toString() : null);
        }).toList();

        return new DashboardStats(
                endpoints.size(), (int) active, (int) matched, (int) unmatched,
                suiteSummaries, recentRuns, recentFires
        );
    }

    public record DashboardStats(
            int endpointCount, int activeEndpointCount,
            long matchedRequests, long unmatchedRequests,
            List<SuiteSummary> suites,
            List<RecentRun> recentRuns,
            List<RecentFire> recentFires
    ) {}

    public record SuiteSummary(Long id, String name, String color, List<ScenarioSummary> scenarios) {}

    public record ScenarioSummary(
            Long id, String name,
            String lastRunStatus, String lastRunAt,
            boolean activeRun, int lastRunPassed, int lastRunTotal
    ) {}

    public record RecentRun(
            Long id,
            Long suiteId, String suiteName, String suiteColor,
            Long scenarioId, String scenarioName,
            String status,
            String startedAt, String completedAt,
            int passed, int total
    ) {}

    public record RecentFire(
            Long id, String name, String type,
            String scenarioName, String firedAt
    ) {}
}
