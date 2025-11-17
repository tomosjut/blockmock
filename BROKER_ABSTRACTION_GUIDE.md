# Message Broker Abstraction Layer - Implementatiehandleiding

## Overzicht

BlockMock ondersteunt nu meerdere message brokers via een abstractielaag:
- **RabbitMQ** (AMQP 0.9.1)
- **Apache ActiveMQ Artemis** (JMS)
- **IBM MQ** (JMS)

## Architectuur

### 1. Core Interface
**`MessageBrokerClient`** (`nl.blockmock.broker.MessageBrokerClient`)

De centrale interface die alle broker-specifieke implementaties moeten volgen:

```java
public interface MessageBrokerClient {
    void connect(AmqpConfig config) throws Exception;
    void startConsuming(MessageHandler messageHandler) throws Exception;
    void publish(String message, String routingKey) throws Exception;
    void sendReply(String replyTo, String correlationId, String message) throws Exception;
    void close() throws Exception;
    boolean isConnected();
}
```

### 2. Implementaties

#### RabbitMqClient
- **Protocol**: AMQP 0.9.1
- **Dependency**: `com.rabbitmq:amqp-client:5.21.0`
- **Features**:
  - Native AMQP exchange types (DIRECT, FANOUT, TOPIC, HEADERS)
  - Virtual hosts
  - Queue bindings met routing patterns

#### ArtemisClient
- **Protocol**: JMS 2.0
- **Dependency**: `org.apache.activemq:artemis-jms-client:2.37.0`
- **Features**:
  - Topic en Queue destinations
  - Message selectors voor routing
  - JMS properties voor headers

#### IbmMqClient
- **Protocol**: JMS via IBM MQ native client
- **Dependency**: `com.ibm.mq:com.ibm.mq.allclient:9.4.1.0`
- **Features**:
  - Queue Manager support
  - Channel configuratie
  - IBM MQ specifieke properties

### 3. Factory Pattern
**`MessageBrokerClientFactory`** creëert de juiste client op basis van configuratie:

```java
MessageBrokerClient client = factory.createClient(config);
```

## Database Schema Wijzigingen

### Migratie V15
Voegt `broker_type` kolom toe aan `amqp_config` tabel:

```sql
ALTER TABLE amqp_config
ADD COLUMN broker_type VARCHAR(50) NOT NULL DEFAULT 'RABBITMQ';
```

Mogelijke waarden:
- `RABBITMQ`
- `ARTEMIS`
- `IBM_MQ`

## Domain Model

### AmqpConfig Updates
```java
@Enumerated(EnumType.STRING)
@Column(name = "broker_type", nullable = false, length = 50)
private MessageBrokerType brokerType = MessageBrokerType.RABBITMQ;
```

### MessageBrokerType Enum
```java
public enum MessageBrokerType {
    RABBITMQ,
    ARTEMIS,
    IBM_MQ
}
```

## Configuratie per Broker Type

### RabbitMQ
```json
{
  "brokerType": "RABBITMQ",
  "host": "localhost",
  "port": 5672,
  "virtualHost": "/",
  "exchangeName": "my.exchange",
  "exchangeType": "TOPIC",
  "queueName": "my.queue",
  "routingKey": "events.#",
  "username": "guest",
  "password": "guest"
}
```

### Apache Artemis
```json
{
  "brokerType": "ARTEMIS",
  "host": "localhost",
  "port": 61616,
  "exchangeName": "my.topic",
  "exchangeType": "FANOUT",
  "queueName": "my.queue",
  "username": "artemis",
  "password": "artemis"
}
```

**Opmerking**: Artemis gebruikt standaard poort 61616 voor JMS.

### IBM MQ
```json
{
  "brokerType": "IBM_MQ",
  "host": "localhost",
  "port": 1414,
  "virtualHost": "QM1",
  "exchangeName": "SYSTEM.DEFAULT.LOCAL.QUEUE",
  "queueName": "MY.QUEUE",
  "username": "mqm",
  "password": "passw0rd"
}
```

**Opmerkingen**:
- `virtualHost` wordt gebruikt voor Queue Manager naam
- Standaard poort is 1414
- Default channel: `SYSTEM.DEF.SVRCONN`

## Exchange Type Mapping

| AMQP Type | RabbitMQ | Artemis | IBM MQ |
|-----------|----------|---------|--------|
| DIRECT    | Direct Exchange | Queue | Queue |
| FANOUT    | Fanout Exchange | Topic | Topic |
| TOPIC     | Topic Exchange | Queue met Selector | Queue met Selector |
| HEADERS   | Headers Exchange | Queue met Properties | Queue met Properties |

## Gebruik in Code

### Endpoint Aanmaken via REST API

```bash
curl -X POST http://localhost:8080/api/endpoints \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Artemis Test Queue",
    "protocol": "AMQP",
    "enabled": true,
    "amqpConfig": {
      "brokerType": "ARTEMIS",
      "host": "localhost",
      "port": 61616,
      "operation": "CONSUME",
      "exchangeName": "test.queue",
      "exchangeType": "DIRECT",
      "queueName": "test.queue"
    }
  }'
```

### Service Gebruik

De `AmqpMockService` gebruikt automatisch de juiste client:

```java
@Inject
AmqpMockService amqpMockService;

// Start mock (detecteert automatisch broker type)
amqpMockService.startAmqpMock(endpoint);

// Publish message (werkt voor alle broker types)
amqpMockService.publishMockMessage(endpointId);

// Stop mock
amqpMockService.stopAmqpMock(endpointId);
```

## Message Handling

### Incoming Messages
Alle brokers leveren messages in hetzelfde formaat via `IncomingMessage`:

```java
record IncomingMessage(
    String body,
    String routingKey,
    String correlationId,
    String replyTo,
    Map<String, Object> headers
)
```

### Auto-Reply Functionaliteit
Werkt voor alle broker types:

```java
if (config.getAutoReply()) {
    client.sendReply(
        message.replyTo(),
        message.correlationId(),
        config.getMockMessageContent()
    );
}
```

## Dependencies

Alle drie broker clients zijn toegevoegd aan `pom.xml`:

```xml
<!-- RabbitMQ -->
<dependency>
    <groupId>com.rabbitmq</groupId>
    <artifactId>amqp-client</artifactId>
    <version>5.21.0</version>
</dependency>

<!-- Apache ActiveMQ Artemis -->
<dependency>
    <groupId>org.apache.activemq</groupId>
    <artifactId>artemis-jms-client</artifactId>
    <version>2.37.0</version>
</dependency>

<!-- IBM MQ -->
<dependency>
    <groupId>com.ibm.mq</groupId>
    <artifactId>com.ibm.mq.allclient</artifactId>
    <version>9.4.1.0</version>
</dependency>
```

## Migratie van Bestaande Configuraties

Bestaande AMQP configuraties krijgen automatisch `broker_type = 'RABBITMQ'` via de database migratie. Geen handmatige actie vereist.

## Testing

### Test met RabbitMQ (bestaand)
```bash
# Start RabbitMQ via Docker
docker run -d --name rabbitmq -p 5672:5672 rabbitmq:3-management
```

### Test met Artemis
```bash
# Start Artemis via Docker
docker run -d --name artemis \
  -p 61616:61616 \
  -e ARTEMIS_USERNAME=artemis \
  -e ARTEMIS_PASSWORD=artemis \
  quay.io/artemiscloud/activemq-artemis-broker:latest
```

### Test met IBM MQ
```bash
# Start IBM MQ via Docker
docker run -d --name ibmmq \
  -p 1414:1414 \
  -e LICENSE=accept \
  -e MQ_QMGR_NAME=QM1 \
  icr.io/ibm-messaging/mq:latest
```

## Troubleshooting

### RabbitMQ Connection Errors
- Controleer of poort 5672 open is
- Verify virtual host bestaat (`/` is default)
- Check username/password credentials

### Artemis Connection Errors
- Standaard poort is 61616 (niet 5672!)
- Artemis ondersteunt geen AMQP virtual hosts
- Check JMS destination naming conventions

### IBM MQ Connection Errors
- Queue Manager moet draaien en bereikbaar zijn
- Channel `SYSTEM.DEF.SVRCONN` moet beschikbaar zijn
- Controleer IBM MQ specifieke permissions
- `virtualHost` veld wordt gebruikt voor Queue Manager naam

## Best Practices

1. **Broker Type Selectie**: Kies de broker die je in productie gebruikt
2. **Connection Pooling**: Huidige implementatie hergebruikt connections per endpoint
3. **Error Handling**: Alle exceptions worden gelogd maar stoppen de applicatie niet
4. **Resource Cleanup**: Connections worden automatisch gesloten bij shutdown
5. **Routing Keys**: Gebruik consistent routing schema per broker type

## Toekomstige Uitbreidingen

Mogelijke toevoegingen:
- Apache Kafka adapter (non-JMS)
- Azure Service Bus
- AWS SQS/SNS
- Google Pub/Sub
- STOMP protocol support

## Support

Voor vragen of problemen, zie:
- RabbitMQ: https://www.rabbitmq.com/documentation.html
- Artemis: https://activemq.apache.org/components/artemis/
- IBM MQ: https://www.ibm.com/docs/en/ibm-mq
