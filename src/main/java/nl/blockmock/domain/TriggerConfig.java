package nl.blockmock.domain;

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
@Table(name = "trigger_config")
@Getter
@Setter
public class TriggerConfig extends PanacheEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TriggerType type;

    @JsonIgnoreProperties({"blocks", "expectations", "triggers", "createdAt", "updatedAt"})
    @ManyToOne
    @JoinColumn(name = "test_suite_id")
    private TestSuite testSuite;

    // HTTP trigger fields
    @Column(name = "http_url", length = 2000)
    private String httpUrl;

    @Column(name = "http_method", length = 20)
    private String httpMethod = "POST";

    @Column(name = "http_body", columnDefinition = "TEXT")
    private String httpBody;

    @Type(JsonBinaryType.class)
    @Column(name = "http_headers", columnDefinition = "jsonb")
    private Map<String, String> httpHeaders;

    // Cron trigger fields
    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "last_fired_at")
    private LocalDateTime lastFiredAt;

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
