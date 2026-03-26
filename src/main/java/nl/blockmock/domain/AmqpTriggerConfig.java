package nl.blockmock.domain;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.Map;

@Entity
@Table(name = "trigger_config_amqp")
@DiscriminatorValue("AMQP")
@Getter
@Setter
public class AmqpTriggerConfig extends TriggerConfig {

    @Column(name = "amqp_address", length = 500)
    private String amqpAddress;

    @Column(name = "amqp_body", columnDefinition = "TEXT")
    private String amqpBody;

    @Type(JsonBinaryType.class)
    @Column(name = "amqp_properties", columnDefinition = "jsonb")
    private Map<String, String> amqpProperties;

    // ANYCAST (queue) or MULTICAST (topic)
    @Column(name = "amqp_routing_type", length = 10, nullable = false)
    private String amqpRoutingType = "ANYCAST";
}
