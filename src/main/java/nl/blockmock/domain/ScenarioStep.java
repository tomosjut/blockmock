package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "scenario_step")
@Getter
@Setter
public class ScenarioStep extends PanacheEntity {

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ScenarioAction action;

    @ManyToOne
    @JoinColumn(name = "mock_endpoint_id")
    private MockEndpoint mockEndpoint;

    @Column(name = "delay_ms")
    private Integer delayMs = 0;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
