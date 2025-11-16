package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "websocket_config")
@Getter
@Setter
public class WebSocketConfig extends PanacheEntity {

    @JsonBackReference("endpoint-websocketconfig")
    @OneToOne
    @JoinColumn(name = "mock_endpoint_id", nullable = false)
    private MockEndpoint mockEndpoint;

    @Column(length = 500, nullable = false)
    private String path;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private WebSocketOperation operation;

    @Column(name = "auto_response_enabled")
    private Boolean autoResponseEnabled = false;

    @Column(name = "auto_response_message", columnDefinition = "TEXT")
    private String autoResponseMessage;

    @Column(name = "auto_response_delay_ms")
    private Integer autoResponseDelayMs = 0;

    @Column(name = "max_connections")
    private Integer maxConnections = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_format", length = 50)
    private WebSocketMessageFormat messageFormat = WebSocketMessageFormat.TEXT;

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
