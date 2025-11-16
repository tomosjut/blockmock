package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "mock_response")
@Getter
@Setter
public class MockResponse extends PanacheEntity {

    @JsonBackReference("endpoint-responses")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_endpoint_id", nullable = false)
    private MockEndpoint mockEndpoint;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer priority = 0;

    // Matching criteria
    @Type(JsonBinaryType.class)
    @Column(name = "match_headers", columnDefinition = "jsonb")
    private Map<String, String> matchHeaders;

    @Column(name = "match_body", columnDefinition = "TEXT")
    private String matchBody;

    @Type(JsonBinaryType.class)
    @Column(name = "match_query_params", columnDefinition = "jsonb")
    private Map<String, String> matchQueryParams;

    @Column(name = "match_script", columnDefinition = "TEXT")
    private String matchScript;

    // Response configuration
    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @Type(JsonBinaryType.class)
    @Column(name = "response_headers", columnDefinition = "jsonb")
    private Map<String, String> responseHeaders;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_delay_ms")
    private Integer responseDelayMs = 0;

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
}
