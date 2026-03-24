package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "test_expectation")
@Getter
@Setter
public class TestExpectation extends PanacheEntity {

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "test_suite_id", nullable = false)
    private TestSuite testSuite;

    @Column(nullable = false)
    private String name;

    @JsonIgnoreProperties({"responses", "totalRequests", "matchedRequests", "unmatchedRequests",
            "lastRequestAt", "averageResponseTimeMs", "createdAt", "updatedAt"})
    @ManyToOne
    @JoinColumn(name = "mock_endpoint_id")
    private MockEndpoint mockEndpoint;

    @Column(name = "min_call_count", nullable = false)
    private Integer minCallCount = 1;

    @Column(name = "max_call_count")
    private Integer maxCallCount;

    @Column(name = "required_body_contains", columnDefinition = "TEXT")
    private String requiredBodyContains;

    @Type(JsonBinaryType.class)
    @Column(name = "required_headers", columnDefinition = "jsonb")
    private Map<String, String> requiredHeaders;

    @Column(name = "expectation_order")
    private Integer expectationOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
