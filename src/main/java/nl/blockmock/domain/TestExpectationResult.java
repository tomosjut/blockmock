package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_expectation_result")
@Getter
@Setter
public class TestExpectationResult extends PanacheEntity {

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRun testRun;

    @ManyToOne
    @JoinColumn(name = "test_expectation_id", nullable = false)
    private TestExpectation testExpectation;

    @Column(nullable = false)
    private Boolean passed;

    @Column(name = "actual_call_count", nullable = false)
    private Integer actualCallCount = 0;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
