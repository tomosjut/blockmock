package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "request_log")
@Getter
@Setter
public class RequestLog extends PanacheEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_endpoint_id")
    private MockEndpoint mockEndpoint;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_response_id")
    private MockResponse mockResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProtocolType protocol;

    // Request details
    @Column(name = "request_method", length = 50)
    private String requestMethod;

    @Column(name = "request_path", length = 1000)
    private String requestPath;

    @Type(JsonBinaryType.class)
    @Column(name = "request_headers", columnDefinition = "jsonb")
    private Map<String, String> requestHeaders;

    @Type(JsonBinaryType.class)
    @Column(name = "request_query_params", columnDefinition = "jsonb")
    private Map<String, String> requestQueryParams;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    // Response details
    @Column(name = "response_status_code")
    private Integer responseStatusCode;

    @Type(JsonBinaryType.class)
    @Column(name = "response_headers", columnDefinition = "jsonb")
    private Map<String, String> responseHeaders;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_delay_ms")
    private Integer responseDelayMs;

    // Metadata
    @Column(nullable = false)
    private Boolean matched = false;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}
