package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "sftp_config")
@Getter
@Setter
public class SftpConfig extends PanacheEntity {

    @JsonBackReference("endpoint-sftpconfig")
    @OneToOne
    @JoinColumn(name = "mock_endpoint_id", nullable = false)
    private MockEndpoint mockEndpoint;

    @Column(nullable = false)
    private Integer port = 2222;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private SftpOperation operation;

    @Column(name = "path_pattern", nullable = false, length = 500)
    private String pathPattern;

    @Column(name = "path_is_regex", nullable = false)
    private Boolean pathIsRegex = false;

    // Authentication
    @Column(length = 255)
    private String username;

    @Column(length = 255)
    private String password;

    @Column(name = "allow_anonymous", nullable = false)
    private Boolean allowAnonymous = false;

    // Mock response content
    @Column(name = "mock_response_content", columnDefinition = "TEXT")
    private String mockResponseContent;

    @Column(nullable = false)
    private Boolean success = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
