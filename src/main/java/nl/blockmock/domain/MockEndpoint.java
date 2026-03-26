package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base for mock endpoints. Concrete subtypes ({@link HttpMockEndpoint}, {@link AmqpMockEndpoint})
 * are stored in separate joined tables and distinguished by the {@code protocol} discriminant.
 * Tracks aggregate metrics (total/matched/unmatched requests) and holds an optional
 * {@code forcedResponse} used during test runs to override normal response selection.
 */
@Entity
@Table(name = "mock_endpoint")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING, length = 50)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "protocol", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = HttpMockEndpoint.class, name = "HTTP"),
    @JsonSubTypes.Type(value = AmqpMockEndpoint.class, name = "AMQP"),
    @JsonSubTypes.Type(value = AmqpMockEndpoint.class, name = "AMQPS"),
})
@Getter
@Setter
public abstract class MockEndpoint extends PanacheEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProtocolType protocol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PatternType pattern;

    @Column(nullable = false)
    private Boolean enabled = true;

    // Metrics/Statistics
    @Column(name = "total_requests")
    private Long totalRequests = 0L;

    @Column(name = "matched_requests")
    private Long matchedRequests = 0L;

    @Column(name = "unmatched_requests")
    private Long unmatchedRequests = 0L;

    @Column(name = "last_request_at")
    private LocalDateTime lastRequestAt;

    @Column(name = "average_response_time_ms")
    private Integer averageResponseTimeMs = 0;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forced_response_id")
    private MockResponse forcedResponse;

    @JsonManagedReference("endpoint-responses")
    @OneToMany(mappedBy = "mockEndpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MockResponse> responses = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addResponse(MockResponse response) {
        responses.add(response);
        response.setMockEndpoint(this);
    }

    public void removeResponse(MockResponse response) {
        responses.remove(response);
        response.setMockEndpoint(null);
    }
}
