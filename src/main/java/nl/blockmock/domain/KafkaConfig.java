package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "kafka_config")
@Getter
@Setter
public class KafkaConfig extends PanacheEntity {

    @JsonBackReference("endpoint-kafkaconfig")
    @OneToOne
    @JoinColumn(name = "mock_endpoint_id", nullable = false)
    private MockEndpoint mockEndpoint;

    @Column(name = "bootstrap_servers", length = 500)
    private String bootstrapServers = "localhost:9092";

    @Column(name = "topic_name", length = 255, nullable = false)
    private String topicName;

    @Column(name = "num_partitions")
    private Integer numPartitions = 1;

    @Column(name = "replication_factor")
    private Integer replicationFactor = 1;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private KafkaOperation operation;

    @Column(name = "group_id", length = 255)
    private String groupId;

    @Column(name = "auto_offset_reset", length = 50)
    private String autoOffsetReset = "earliest";

    @Column(name = "enable_auto_commit")
    private Boolean enableAutoCommit = true;

    @Column(name = "message_key", length = 500)
    private String messageKey;

    @Column(name = "message_value", columnDefinition = "TEXT")
    private String messageValue;

    @Column(name = "message_headers", columnDefinition = "TEXT")
    private String messageHeaders;

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
