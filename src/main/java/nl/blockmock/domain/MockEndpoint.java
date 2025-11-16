package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mock_endpoint")
@Getter
@Setter
public class MockEndpoint extends PanacheEntity {

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

    @JsonManagedReference("endpoint-httpconfig")
    @OneToOne(mappedBy = "mockEndpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private HttpConfig httpConfig;

    @JsonManagedReference("endpoint-sftpconfig")
    @OneToOne(mappedBy = "mockEndpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private SftpConfig sftpConfig;

    @JsonManagedReference("endpoint-amqpconfig")
    @OneToOne(mappedBy = "mockEndpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private AmqpConfig amqpConfig;

    @JsonManagedReference("endpoint-sqlconfig")
    @OneToOne(mappedBy = "mockEndpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    private SqlConfig sqlConfig;

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
