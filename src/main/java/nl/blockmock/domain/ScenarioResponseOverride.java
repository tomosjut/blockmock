package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "scenario_response_override")
@Getter
@Setter
public class ScenarioResponseOverride extends PanacheEntity {

    @JsonBackReference("scenario-overrides")
    @ManyToOne
    @JoinColumn(name = "test_scenario_id", nullable = false)
    private TestScenario testScenario;

    @JsonIgnoreProperties({"responses", "forcedResponse", "totalRequests", "matchedRequests",
            "unmatchedRequests", "lastRequestAt", "averageResponseTimeMs", "createdAt", "updatedAt"})
    @ManyToOne
    @JoinColumn(name = "mock_endpoint_id", nullable = false)
    private MockEndpoint mockEndpoint;

    @JsonIgnoreProperties({"matchHeaders", "matchBody", "matchQueryParams", "matchScript",
            "responseHeaders", "responseBody", "responseDelayMs", "createdAt", "updatedAt"})
    @ManyToOne
    @JoinColumn(name = "mock_response_id", nullable = false)
    private MockResponse mockResponse;
}
