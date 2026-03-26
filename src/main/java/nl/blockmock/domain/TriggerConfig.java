package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "trigger_config")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING, length = 50)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.EXISTING_PROPERTY, visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = HttpTriggerConfig.class, name = "HTTP"),
    @JsonSubTypes.Type(value = CronTriggerConfig.class, name = "CRON"),
    @JsonSubTypes.Type(value = AmqpTriggerConfig.class, name = "AMQP"),
})
@Getter
@Setter
public abstract class TriggerConfig extends PanacheEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TriggerType type;

    @JsonIgnoreProperties({"expectations", "responseOverrides", "testSuite", "createdAt", "updatedAt"})
    @ManyToOne
    @JoinColumn(name = "test_scenario_id")
    private TestScenario testScenario;

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
