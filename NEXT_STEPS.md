# Next Steps

## AMQP/Artemis mock support

Doel: BlockMock gedraagt zich als een Artemis broker. Applicaties die normaal AMQP-berichten sturen naar Artemis, sturen ze naar BlockMock. BlockMock legt ze vast en evalueert expectations — net als bij HTTP.

### Architectuur

**Artemis draait in Docker — BlockMock is een client.**

Docker is al een prerequisite (voor PostgreSQL), dus we spinnen Artemis op als extra service in `docker-compose.yml`. BlockMock maakt als AMQP-client verbinding met die broker en subscribet op geconfigureerde adressen.

De applicatie-onder-test én BlockMock praten allebei naar dezelfde Artemis-container — precies zoals beide nu naar dezelfde PostgreSQL praten. Dit patroon is herbruikbaar voor elk ander protocol (Kafka, Redis, etc.): altijd een Docker-service erbij, BlockMock als observerende client.

Voor de AMQP-client in Quarkus gebruiken we **Vert.x AMQP client** — die zit al in de Quarkus-stack en spreekt AMQP 1.0.

**Ontvangen vs. verzenden?**

Een AMQP-endpoint kan twee richtingen op:
- **RECEIVE** — app publiceert naar BlockMock (meest voorkomend: BlockMock legt berichten vast)
- **PUBLISH** — BlockMock publiceert naar app (app is consumer; BlockMock stuurt een bericht als trigger)
- **REQUEST_REPLY** — app stuurt bericht, BlockMock antwoordt op `reply-to` address

Dit bepaalt hoe `Block enable/disable` werkt en wat een Trigger doet bij AMQP.

---

### Stap 1 — Docker + Dependencies

**`docker-compose.yml`** — Artemis service toevoegen:
```yaml
artemis:
  image: apache/activemq-artemis:2.37.0
  container_name: blockmock-artemis
  ports:
    - "5672:5672"   # AMQP
    - "5671:5671"   # AMQPS
    - "8161:8161"   # Artemis web console
  environment:
    ARTEMIS_USER: artemis
    ARTEMIS_PASSWORD: artemis
  volumes:
    - artemis-data:/var/lib/artemis-instance
```

**`pom.xml`** — Vert.x AMQP client (al beschikbaar via Quarkus):
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-reactive-messaging-amqp</artifactId>
</dependency>
```

Of direct de Vert.x client als meer controle nodig is:
```xml
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-amqp-client</artifactId>
</dependency>
```

Configuratie in `application.properties`:
```properties
blockmock.amqp.host=${BLOCKMOCK_AMQP_HOST:localhost}
blockmock.amqp.port=${BLOCKMOCK_AMQP_PORT:5672}
blockmock.amqp.user=${BLOCKMOCK_AMQP_USER:artemis}
blockmock.amqp.password=${BLOCKMOCK_AMQP_PASSWORD:artemis}
blockmock.amqp.tls=${BLOCKMOCK_AMQP_TLS:false}
```

---

### Stap 2 — Domain model

**`ProtocolType`**
```java
public enum ProtocolType { HTTP, AMQP, AMQPS }
```

**`MockEndpoint`** — extra kolommen:
```sql
ALTER TABLE mock_endpoint
    ADD COLUMN amqp_address VARCHAR(500),   -- queue/topic address, bijv. "orders.created"
    ADD COLUMN amqp_pattern VARCHAR(20);    -- RECEIVE, PUBLISH, REQUEST_REPLY
```

**`RequestLog`** — AMQP-specifieke velden toevoegen (zelfde tabel, protocol-kolom bepaalt welke velden gevuld zijn):
```sql
ALTER TABLE request_log
    ADD COLUMN amqp_address      VARCHAR(500),
    ADD COLUMN amqp_subject      VARCHAR(500),
    ADD COLUMN amqp_message_id   VARCHAR(500),
    ADD COLUMN amqp_correlation_id VARCHAR(500),
    ADD COLUMN amqp_reply_to     VARCHAR(500),
    ADD COLUMN amqp_properties   JSONB;
```

Bestaande kolommen `request_body` (= message body), `matched`, `mock_endpoint_id` blijven gewoon werken.

**`TriggerType`**
```java
public enum TriggerType { HTTP, CRON, AMQP }
```

**`TriggerConfig`** — extra kolommen voor AMQP trigger:
```sql
ALTER TABLE trigger_config
    ADD COLUMN amqp_address    VARCHAR(500),  -- address om naar te publiceren
    ADD COLUMN amqp_body       TEXT,
    ADD COLUMN amqp_properties JSONB;         -- message properties/headers
```

Flyway: **V2__amqp_support.sql**

---

### Stap 3 — `AmqpConnectionService`

Nieuwe `@ApplicationScoped` service die de verbinding met de externe Artemis-broker beheert:

```java
@ApplicationScoped
public class AmqpConnectionService {
    // @PostConstruct: verbinding opzetten met geconfigureerde Artemis
    // @PreDestroy: verbinding sluiten
    // startConsumer(MockEndpoint) — subscribet op endpoint.amqpAddress, roept AmqpMockService.onMessage() aan
    // stopConsumer(MockEndpoint)
    // publish(address, body, properties) — voor PUBLISH-endpoints en AMQP-triggers
    // isConnected() — health check
}
```

Let op: verbinding is optioneel — als Artemis niet geconfigureerd/beschikbaar is, loggen we een waarschuwing maar start BlockMock gewoon op. AMQP-endpoints worden dan als inactief gemarkeerd.

---

### Stap 4 — `AmqpMockService`

Vergelijkbaar met `HttpMockService`, maar voor binnenkomende AMQP-berichten:

```java
public void onMessage(String address, Message amqpMessage) {
    // 1. Zoek MockEndpoint op adres
    // 2. Log naar RequestLog (protocol=AMQP, amqp_address, body, properties)
    // 3. Update endpoint metrics
    // 4. Als REQUEST_REPLY: stuur antwoord op reply-to address
    // 5. Als endpoint niet gevonden: log als unmatched
}
```

---

### Stap 5 — Block enable/disable aanpassen

`BlockService.startBlock(blockId)` en `stopBlock(blockId)` moeten ook AMQP-endpoints afhandelen:

- HTTP-endpoint: `enabled = true/false` (bestaand gedrag)
- AMQP RECEIVE-endpoint: `AmqpConnectionService.startConsumer(endpoint)` / `stopConsumer(endpoint)`
- AMQP PUBLISH-endpoint: geen actie bij start/stop (BlockMock publiceert pas bij trigger-fire)

---

### Stap 6 — Expectations evalueren

`TestSuiteService.getMatchingLogs(expectation, start, end)` werkt al op `RequestLog`. AMQP-berichten komen in dezelfde tabel terecht (met `protocol = 'AMQP'`), dus expectations werken zonder grote aanpassingen.

Mogelijk aanpassen: `requiredBodyContains` matcht op AMQP message body, `requiredHeaders` matcht op `amqp_properties`.

---

### Stap 7 — AMQP-trigger

Een AMQP-trigger publiceert een bericht op een queue/topic, zodat de applicatie-onder-test dat bericht consumeert en verwerkt. Dit is het AMQP-equivalent van een HTTP-trigger die een POST-request doet.

Typisch use case:
```
BlockMock publiceert → "order.incoming" queue → app consumeert → app roept payment/inventory/notifications aan → BlockMock evalueert expectations
```

`TriggerService.fireTrigger(triggerConfig)`:
```java
case AMQP -> {
    amqpConnectionService.publish(
        trigger.getAmqpAddress(),
        trigger.getAmqpBody(),
        trigger.getAmqpProperties()
    );
    // TriggerFireResult bevat geen responseStatus (fire-and-forget),
    // maar wel timestamp en eventueel een foutmelding als publish mislukt
}
```

**Frontend** — trigger-formulier bij type AMQP:
- Address (queue/topic naam, bijv. `order.incoming`)
- Body (message body, vrije tekst / JSON)
- Properties (key-value editor, zelfde patroon als HTTP headers)

**`TriggerFireResult`** uitbreiden: voeg `messageId` toe (Artemis geeft een message-ID terug bij publish), zodat je de trigger-fire kunt correleren met een eventuele log-entry.

---

### Stap 8 — Frontend

- **Endpoint formulier**: toon `amqpAddress` + `amqpPattern` velden als protocol = AMQP/AMQPS
- **Request log**: toon AMQP-specifieke velden (address, subject, message ID, properties) als protocol = AMQP
- **Trigger formulier**: voeg AMQP-type toe met address + body + properties
- **`ProtocolType`** in `types/index.ts`: uitbreiden met `'AMQP' | 'AMQPS'`

---

### Stap 9 — AMQPS (TLS) — apart werkpakket

TLS zit aan de Artemis-kant (Docker-configuratie) én aan de client-kant (Vert.x verbindingsopties). Geen code in BlockMock zelf anders dan de `blockmock.amqp.tls=true` flag doorgeven aan de Vert.x client.

Advies: eerst AMQP plain werkend krijgen, dan AMQPS als losse stap.

---

### Volgorde van implementatie

1. Stap 1–3: broker draait, berichten komen binnen, worden gelogd
2. Stap 5: blocks werken correct (start/stop consumer)
3. Stap 4 + 6: expectations evalueren op AMQP-berichten
4. Stap 7: AMQP-trigger (BlockMock publiceert bericht)
5. Stap 8: frontend
6. Stap 9: AMQPS/TLS

### Demo uitbreiden

Na implementatie: voeg een AMQP-scenario toe aan de order-service demo. De order service publiceert een `order.created` event op een Artemis queue; BlockMock verifieert dat het bericht aankomt met de juiste inhoud.
