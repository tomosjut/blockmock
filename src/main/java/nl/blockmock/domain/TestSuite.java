package nl.blockmock.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "test_suite")
@Getter
@Setter
public class TestSuite extends PanacheEntity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String color = "#667eea";

    @ManyToMany
    @JoinTable(
        name = "test_suite_block",
        joinColumns = @JoinColumn(name = "test_suite_id"),
        inverseJoinColumns = @JoinColumn(name = "block_id")
    )
    private Set<Block> blocks = new HashSet<>();

    @OneToMany(mappedBy = "testSuite", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TestExpectation> expectations = new ArrayList<>();

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
