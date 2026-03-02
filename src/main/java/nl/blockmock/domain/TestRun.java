package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_run")
@Getter
@Setter
public class TestRun extends PanacheEntity {

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "test_suite_id", nullable = false)
    private TestSuite testSuite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TestRunStatus status = TestRunStatus.RUNNING;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestExpectationResult> results = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }
}
