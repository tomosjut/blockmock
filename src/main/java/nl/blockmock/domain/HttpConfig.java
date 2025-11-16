package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "http_config")
@Getter
@Setter
public class HttpConfig extends PanacheEntity {

    @JsonBackReference("endpoint-httpconfig")
    @OneToOne
    @JoinColumn(name = "mock_endpoint_id", nullable = false)
    private MockEndpoint mockEndpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private HttpMethod method;

    @Column(nullable = false, length = 1000)
    private String path;

    @Column(name = "path_regex", nullable = false)
    private Boolean pathRegex = false;
}
