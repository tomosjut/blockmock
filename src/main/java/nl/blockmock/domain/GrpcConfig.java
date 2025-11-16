package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "grpc_config")
@Getter
@Setter
public class GrpcConfig extends PanacheEntity {

    @JsonBackReference("endpoint-grpcconfig")
    @OneToOne
    @JoinColumn(name = "mock_endpoint_id", nullable = false)
    private MockEndpoint mockEndpoint;

    @Column(nullable = false)
    private Integer port = 50051;

    @Column(name = "service_name", length = 255, nullable = false)
    private String serviceName;

    @Column(name = "method_name", length = 255, nullable = false)
    private String methodName;

    @Column(name = "package_name", length = 255)
    private String packageName;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;

    @Column(name = "response_delay_ms")
    private Integer responseDelayMs = 0;

    @Column(name = "is_client_streaming")
    private Boolean isClientStreaming = false;

    @Column(name = "is_server_streaming")
    private Boolean isServerStreaming = false;

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
