package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "nosql_config")
@Getter
@Setter
public class NoSqlConfig extends PanacheEntity {

    @JsonBackReference("endpoint-nosqlconfig")
    @OneToOne
    @JoinColumn(name = "mock_endpoint_id", nullable = false)
    private MockEndpoint mockEndpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "database_type", nullable = false, length = 50)
    private NoSqlDatabaseType databaseType;

    // Connection settings
    @Column(length = 255)
    private String host = "localhost";

    @Column
    private Integer port = 27017;

    @Column(name = "database_name", length = 100, nullable = false)
    private String databaseName;

    @Column(name = "collection_name", length = 100)
    private String collectionName; // For MongoDB

    // Authentication
    @Column(length = 100)
    private String username;

    @Column(length = 255)
    private String password;

    @Column(name = "auth_database", length = 100)
    private String authDatabase; // For MongoDB authentication

    // Redis specific
    @Column(name = "redis_db_index")
    private Integer redisDbIndex = 0;

    // MongoDB specific
    @Column(name = "replica_set", length = 255)
    private String replicaSet;

    // Init data/script
    @Column(name = "init_data", columnDefinition = "TEXT")
    private String initData; // JSON data to insert on startup

    @Column(name = "init_script", columnDefinition = "TEXT")
    private String initScript; // Commands to execute on startup

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
