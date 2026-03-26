package nl.blockmock.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "trigger_config_http")
@DiscriminatorValue("HTTP")
@Getter
@Setter
public class HttpTriggerConfig extends TriggerConfig {

    @Column(name = "http_url", length = 2000)
    private String httpUrl;

    @Column(name = "http_method", length = 20)
    private String httpMethod = "POST";

    @Column(name = "http_body", columnDefinition = "TEXT")
    private String httpBody;

    @Type(JsonBinaryType.class)
    @Column(name = "http_headers", columnDefinition = "jsonb")
    private Map<String, String> httpHeaders;
}
