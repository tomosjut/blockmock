# BlockMock AMQP demo — order service

Demonstreert hoe BlockMock AMQP-berichten onderschept en expectations evalueert — als aparte test suite naast de bestaande HTTP suite.

## Flow

```
BlockMock trigger
  → publiceert order.incoming
    → order service ontvangt bericht
    → verwerkt order
    → publiceert order.confirmed
      → BlockMock onderschept het event (RECEIVE endpoint)
        → expectation: "order.confirmed event published" ✓
```

## Opzetten

**1. Start de services**
```bash
# vanuit repo root
docker-compose up -d          # PostgreSQL + Artemis
mvn quarkus:dev               # BlockMock op port 8080
```

**2. Importeer de BlockMock configuratie**
```bash
./setup-blockmock.sh
```

Maakt aan:
- Endpoint `order.confirmed` (AMQP RECEIVE) — BlockMock luistert hier op
- Block "AMQP Order Flow"
- Test suite "AMQP Order Suite"
- Scenario "Order Confirmed" met expectation
- AMQP trigger "Send order.incoming"

**3. Start de order service**
```bash
npm install   # eenmalig
node app.js
```

## Demo uitvoeren

1. Open BlockMock UI → **Test Suites** → *AMQP Order Suite*
2. Klik **▶ Run** op scenario *Order Confirmed*
3. In het Runs modal → klik **▶ Trigger** (stuurt een `order.incoming` bericht)
   - Kijk in de terminal van de order service: bericht ontvangen + `order.confirmed` gepubliceerd
   - Kijk in BlockMock **Logs**: twee AMQP entries (trigger fire + ontvangen event)
4. Klik **✓ Complete**
5. Resultaat: *order.confirmed event published* ✓

## Reset

```bash
./setup-blockmock.sh --reset
```

## Environment variabelen

| Variabele          | Default     | Beschrijving                |
|--------------------|-------------|-----------------------------|
| `AMQP_HOST`        | `localhost`  | Artemis hostname            |
| `AMQP_PORT`        | `5672`       | AMQP port                   |
| `AMQP_USER`        | `artemis`    | Broker username             |
| `AMQP_PASSWORD`    | `artemis`    | Broker password             |
| `INCOMING_ADDRESS` | `order.incoming`  | Queue om op te luisteren |
| `CONFIRMED_ADDRESS`| `order.confirmed` | Queue om naar te publiceren |
