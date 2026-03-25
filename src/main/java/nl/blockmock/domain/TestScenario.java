package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_scenario")
@Getter
@Setter
public class TestScenario extends PanacheEntity {

    @JsonBackReference("suite-scenarios")
    @ManyToOne
    @JoinColumn(name = "test_suite_id", nullable = false)
    private TestSuite testSuite;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @JsonManagedReference("scenario-expectations")
    @OneToMany(mappedBy = "testScenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestExpectation> expectations = new ArrayList<>();

    @JsonManagedReference("scenario-overrides")
    @OneToMany(mappedBy = "testScenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScenarioResponseOverride> responseOverrides = new ArrayList<>();

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
