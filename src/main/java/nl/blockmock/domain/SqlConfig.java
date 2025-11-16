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
@Table(name = "sql_config")
@Getter
@Setter
public class SqlConfig extends PanacheEntity {

    @JsonBackReference("endpoint-sqlconfig")
    @OneToOne
    @JoinColumn(name = "mock_endpoint_id", nullable = false)
    private MockEndpoint mockEndpoint;

    // Connection settings
    @Enumerated(EnumType.STRING)
    @Column(name = "database_type", nullable = false, length = 50)
    private SqlDatabaseType databaseType = SqlDatabaseType.GENERIC;

    @Column(nullable = false)
    private String host = "localhost";

    @Column(nullable = false)
    private Integer port = 9092;

    @Column(name = "database_name", nullable = false)
    private String databaseName = "mockdb";

    // Authentication
    @Column(length = 255)
    private String username;

    @Column(length = 255)
    private String password;

    @Column(name = "allow_anonymous", nullable = false)
    private Boolean allowAnonymous = false;

    // Initialization SQL
    @Column(name = "init_script", columnDefinition = "TEXT")
    private String initScript;

    // Query mocks
    @JsonManagedReference("sqlconfig-queries")
    @OneToMany(mappedBy = "sqlConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SqlQueryMock> queryMocks = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addQueryMock(SqlQueryMock queryMock) {
        queryMocks.add(queryMock);
        queryMock.setSqlConfig(this);
    }

    public void removeQueryMock(SqlQueryMock queryMock) {
        queryMocks.remove(queryMock);
        queryMock.setSqlConfig(null);
    }
}
