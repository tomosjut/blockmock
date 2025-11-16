package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sql_query_mock")
@Getter
@Setter
public class SqlQueryMock extends PanacheEntity {

    @JsonBackReference("sqlconfig-queries")
    @ManyToOne
    @JoinColumn(name = "sql_config_id", nullable = false)
    private SqlConfig sqlConfig;

    // Query matching
    @Column(name = "query_pattern", columnDefinition = "TEXT", nullable = false)
    private String queryPattern;

    @Column(name = "query_is_regex", nullable = false)
    private Boolean queryIsRegex = false;

    @Column(name = "query_type", length = 50)
    private String queryType; // SELECT, INSERT, UPDATE, DELETE, etc.

    // Mock response
    @Column(name = "mock_resultset", columnDefinition = "TEXT")
    private String mockResultset; // JSON array of rows

    @Column(name = "affected_rows")
    private Integer affectedRows = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "execution_delay_ms")
    private Integer executionDelayMs = 0;

    // Metadata
    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
