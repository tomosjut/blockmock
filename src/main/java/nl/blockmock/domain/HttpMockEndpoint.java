package nl.blockmock.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mock_endpoint_http")
@DiscriminatorValue("HTTP")
@Getter
@Setter
public class HttpMockEndpoint extends MockEndpoint {

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", length = 50)
    private HttpMethod httpMethod;

    @Column(name = "http_path", length = 1000)
    private String httpPath;

    @Column(name = "http_path_regex")
    private Boolean httpPathRegex = false;
}
