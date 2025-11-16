package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "block")
@Getter
@Setter
public class Block extends PanacheEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 7)
    private String color = "#667eea";

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "block_endpoint",
        joinColumns = @JoinColumn(name = "block_id"),
        inverseJoinColumns = @JoinColumn(name = "mock_endpoint_id")
    )
    private Set<MockEndpoint> endpoints = new HashSet<>();

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

    public void addEndpoint(MockEndpoint endpoint) {
        endpoints.add(endpoint);
    }

    public void removeEndpoint(MockEndpoint endpoint) {
        endpoints.remove(endpoint);
    }

    @Transient
    public int getEndpointCount() {
        return endpoints.size();
    }

    @Transient
    public long getActiveEndpointCount() {
        return endpoints.stream().filter(MockEndpoint::getEnabled).count();
    }
}
