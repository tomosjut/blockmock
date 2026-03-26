package nl.blockmock.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "mock_endpoint_amqp")
@DiscriminatorValue("AMQP")
@Getter
@Setter
public class AmqpMockEndpoint extends MockEndpoint {

    @Column(name = "amqp_address", length = 500)
    private String amqpAddress;

    // RECEIVE, PUBLISH, REQUEST_REPLY
    @Column(name = "amqp_pattern", length = 20)
    private String amqpPattern;

    // ANYCAST (queue) or MULTICAST (topic)
    @Column(name = "amqp_routing_type", length = 10, nullable = false)
    private String amqpRoutingType = "ANYCAST";
}
