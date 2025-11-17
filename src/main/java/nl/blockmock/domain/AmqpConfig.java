package nl.blockmock.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "amqp_config")
@Getter
@Setter
public class AmqpConfig extends PanacheEntity {

    @JsonBackReference("endpoint-amqpconfig")
    @OneToOne
    @JoinColumn(name = "mock_endpoint_id", nullable = false)
    private MockEndpoint mockEndpoint;

    // Connection settings
    @Column(nullable = false)
    private String host = "localhost";

    @Column(nullable = false)
    private Integer port = 5672;

    @Column(name = "virtual_host")
    private String virtualHost = "/";

    // Exchange configuration
    @Column(name = "exchange_name", nullable = false)
    private String exchangeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_type", nullable = false, length = 50)
    private AmqpExchangeType exchangeType = AmqpExchangeType.DIRECT;

    @Column(name = "exchange_durable", nullable = false)
    private Boolean exchangeDurable = true;

    @Column(name = "exchange_auto_delete", nullable = false)
    private Boolean exchangeAutoDelete = false;

    // Queue configuration
    @Column(name = "queue_name")
    private String queueName;

    @Column(name = "queue_durable", nullable = false)
    private Boolean queueDurable = true;

    @Column(name = "queue_exclusive", nullable = false)
    private Boolean queueExclusive = false;

    @Column(name = "queue_auto_delete", nullable = false)
    private Boolean queueAutoDelete = false;

    // Routing
    @Column(name = "routing_key")
    private String routingKey;

    @Column(name = "binding_pattern")
    private String bindingPattern;

    // Authentication
    @Column(length = 255)
    private String username;

    @Column(length = 255)
    private String password;

    // Broker type
    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false, length = 50)
    private MessageBrokerType brokerType = MessageBrokerType.RABBITMQ;

    // Mock behavior
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AmqpOperation operation;

    @Column(name = "mock_message_content", columnDefinition = "TEXT")
    private String mockMessageContent;

    @Column(name = "mock_message_headers", columnDefinition = "TEXT")
    private String mockMessageHeaders;

    @Column(name = "auto_reply", nullable = false)
    private Boolean autoReply = false;

    @Column(name = "reply_delay_ms")
    private Integer replyDelayMs = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
