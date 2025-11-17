# BlockMock Multi-Protocol Demo Guide

## 🎯 Demo Overzicht

Deze demo toont de kracht van BlockMock met support voor **meerdere protocols**:
- ✅ **HTTP/REST** - Web API's
- ✅ **SFTP** - File transfer
- ✅ **RabbitMQ** - AMQP 0.9.1 messaging
- ✅ **Apache Artemis** - JMS messaging
- ✅ **IBM MQ** - Enterprise messaging

**Nieuw in deze release**: Message Broker Abstractie Layer die RabbitMQ, Artemis én IBM MQ ondersteunt!

---

## 📋 Vereisten

### Software
- Docker & Docker Compose
- Python 3.8+ (voor Artemis/IBM MQ test scripts)
- curl (voor HTTP tests)
- sshpass + sftp client (voor SFTP tests)

### Python Packages
```bash
pip install stomp.py       # Voor Artemis
pip install pymqi          # Voor IBM MQ (optioneel, complexere install)
```

---

## 🚀 Quick Start

### 1. Start Message Brokers

```bash
cd blockmock
docker-compose -f demo/docker-compose-brokers.yml up -d
```

Dit start:
- **RabbitMQ** op poort 5672 (management UI: http://localhost:15672)
- **Apache Artemis** op poort 61616 (console: http://localhost:8161)
- **IBM MQ** op poort 1414 (console: https://localhost:9443)

Wacht ~30 seconden tot alle brokers ready zijn:
```bash
docker-compose -f demo/docker-compose-brokers.yml ps
```

### 2. Start BlockMock

```bash
# Start PostgreSQL
docker-compose up -d

# Start BlockMock (als het nog niet draait)
mvn quarkus:dev -Dquarkus.http.port=8888
```

BlockMock UI: http://localhost:8888

### 3. Importeer Demo Endpoints

Open http://localhost:8888 en ga naar **Mock Endpoints** tab:

1. Klik op "📤 Import"
2. Selecteer `demo/complete-demo-endpoints.json`
3. Ververs de lijst

Je ziet nu 8 demo endpoints:
- 3x HTTP endpoints
- 2x SFTP endpoints
- 1x RabbitMQ endpoint
- 1x Artemis endpoint
- 1x IBM MQ endpoint

---

## 🎬 Demo Scenario's

### Scenario 1: HTTP REST API Demo

**Doel**: Laat zien hoe BlockMock HTTP endpoints kan mocken.

```bash
# Maak scripts executable
chmod +x demo/test-http.sh

# Run demo
./demo/test-http.sh
```

**Wat gebeurt er:**
- GET /api/users → Lijst van gebruikers
- POST /api/users → Nieuwe gebruiker aanmaken
- GET /api/users/123 → Specifieke gebruiker ophalen (met regex)
- PUT /api/users/123 → Gebruiker updaten

**Team demo punten:**
- Toon de **Request Logs** tab in UI → zie alle inkomende requests
- Toon hoe je **status codes** en **delays** kunt configureren
- Toon **regex support** voor dynamic paths
- Toon **meerdere responses** per endpoint

---

### Scenario 2: SFTP File Transfer Demo

**Doel**: Laat zien hoe BlockMock als SFTP server werkt.

```bash
# Installeer sshpass als je dat nog niet hebt
# Ubuntu/Debian: sudo apt-get install sshpass
# macOS: brew install sshpass

# Maak script executable
chmod +x demo/test-sftp.sh

# Run demo
./demo/test-sftp.sh
```

**Wat gebeurt er:**
- Upload file naar `/uploads/` directory
- Download mock PDF van `/downloads/report.pdf`
- List directories

**Team demo punten:**
- SFTP server draait **permanent** op poort 2222
- Toon **Request Logs** → zie UPLOAD/DOWNLOAD operaties
- Toon hoe je **mock file content** kunt configureren
- Toon **authentication** (username/password)

**Alternatief: Handmatig met GUI client**
```
Host: localhost
Port: 2222
Username: testuser
Password: testpass
```

GUI tools: FileZilla, WinSCP, Cyberduck

---

### Scenario 3: RabbitMQ Messaging Demo

**Doel**: Laat zien hoe BlockMock RabbitMQ queues kan mocken.

**Via RabbitMQ Management UI** (makkelijkste voor demo):

1. Open http://localhost:15672
2. Login: guest / guest
3. Ga naar **Queues** tab
4. Klik op `demo.order.processing`
5. Scroll naar **Publish message**
6. Payload:
```json
{
  "order_id": "ORD-12345",
  "customer_id": 789,
  "items": [
    {"product_id": "PROD-001", "quantity": 2, "price": 29.99}
  ],
  "total": 59.98
}
```
7. Klik **Publish message**

**Wat gebeurt er:**
- BlockMock ontvangt de order message
- Logt het in **Request Logs**
- Stuurt automatisch een confirmation terug (auto-reply)

**Team demo punten:**
- Toon **Request Logs** → zie de order message
- Toon **auto-reply functionaliteit** (RPC pattern)
- Toon **routing keys** en **exchange types**
- Highlight: **Exact dezelfde config werkt voor Artemis en IBM MQ!**

---

### Scenario 4: Apache Artemis JMS Demo

**Doel**: Laat zien hoe BlockMock Artemis queues kan mocken via JMS.

```bash
# Maak script executable
chmod +x demo/test-artemis.py

# Run demo
python3 demo/test-artemis.py
```

**Wat gebeurt er:**
- Connect naar Artemis via STOMP protocol
- Subscribe op `demo.notification.queue`
- Verstuur test notification
- BlockMock logt de notification
- Optioneel: BlockMock stuurt response terug

**Team demo punten:**
- **Broker Type** selectie in UI → kies "Apache ActiveMQ Artemis"
- Default poort wijzigt automatisch naar **61616**
- JMS protocol, maar dezelfde concepten: queues, routing, etc.
- Toon Artemis console: http://localhost:8161 (admin/admin)

---

### Scenario 5: IBM MQ Enterprise Messaging Demo

**Doel**: Laat zien hoe BlockMock IBM MQ queues kan mocken.

**Optie A: Via Python script** (als pymqi geïnstalleerd is)
```bash
chmod +x demo/test-ibmmq.py
python3 demo/test-ibmmq.py
```

**Optie B: Via IBM MQ Explorer** (visueel makkelijker):

1. Open https://localhost:9443/ibmmq/console
   (accepteer self-signed certificate)
2. Login: admin / passw0rd
3. Navigate to Queue Manager **QM1**
4. Open **Queues** → Find `DEMO.PAYMENT.QUEUE`
5. **Put Message**:
```json
{
  "payment_type": "credit_card",
  "amount": 149.99,
  "currency": "EUR",
  "merchant_id": "MERCH-12345"
}
```

**Wat gebeurt er:**
- BlockMock ontvangt payment request
- Logt in **Request Logs**
- Stuurt payment approval terug (auto-reply enabled)

**Team demo punten:**
- **Broker Type** selectie in UI → kies "IBM MQ"
- Default poort wijzigt naar **1414**
- **Queue Manager** naam in plaats van virtual host
- Enterprise-grade messaging, gemockt in seconds!

---

## 🎯 Demo Flow voor Team Presentatie

### Aanbevolen volgorde (15-20 minuten):

#### 1. Introductie (2 min)
"BlockMock is een universele mock server die meerdere protocols ondersteunt. Vandaag laat ik zien: HTTP, SFTP, en drie verschillende message brokers."

#### 2. HTTP Demo (3 min)
```bash
./demo/test-http.sh
```
- Run script, toon output
- Open UI → Request Logs tab
- Laat zien: timestamps, request bodies, response codes
- Highlight: "Dit is wat developers al kennen van HTTP mocking"

#### 3. SFTP Demo (2 min)
```bash
./demo/test-sftp.sh
```
- Laat file transfer zien
- Open Request Logs → toon UPLOAD/DOWNLOAD logs
- Mention: "Handig voor batch processing scenarios"

#### 4. Message Broker Demo - Het hoogtepunt! (8 min)

**A. RabbitMQ** (2 min)
- Open RabbitMQ Management UI
- Verstuur message via UI
- Toon in BlockMock Request Logs
- "Dit werkt nu al met RabbitMQ..."

**B. Artemis** (3 min)
- Open BlockMock UI → Mock Endpoints
- Toon "Demo: Artemis Notification Service"
- Highlight: **Broker Type dropdown** → "Apache ActiveMQ Artemis"
- "Kijk, automatisch poort 61616!"
- Run Python script:
```bash
python3 demo/test-artemis.py
```
- Toon Request Logs

**C. IBM MQ** (3 min)
- Open BlockMock UI → Mock Endpoints
- Toon "Demo: IBM MQ Payment Gateway"
- Highlight: **Broker Type dropdown** → "IBM MQ"
- "Queue Manager in plaats van virtual host"
- Open IBM MQ Console → Put message
- Toon Request Logs
- **Key message**: "Exact dezelfde interface, drie verschillende brokers!"

#### 5. Architectuur Highlight (3 min)

Toon `BROKER_ABSTRACTION_GUIDE.md`:
- **MessageBrokerClient interface** → één API voor alles
- **RabbitMqClient**, **ArtemisClient**, **IbmMqClient**
- **MessageBrokerClientFactory** → selecteert de juiste client

Diagram op whiteboard/slides:
```
                  BlockMock
                      |
            MessageBrokerClient (interface)
                      |
        +-------------+-------------+
        |             |             |
   RabbitMQ       Artemis        IBM MQ
   (AMQP)         (JMS)          (JMS)
```

"Dit maakt het makkelijk om:
- Bij klanten te werken die verschillende brokers gebruiken
- Migration scenarios te testen (RabbitMQ → Artemis)
- Multi-broker architecturen te mocken"

#### 6. Wrap-up (2 min)
- Toon Dashboard met statistieken
- Mention andere features: Kafka, gRPC, WebSocket, SQL, NoSQL
- "BlockMock: één tool, alle protocols"

---

## 🔧 Troubleshooting

### Brokers starten niet
```bash
# Check status
docker-compose -f demo/docker-compose-brokers.yml ps

# Check logs
docker-compose -f demo/docker-compose-brokers.yml logs rabbitmq
docker-compose -f demo/docker-compose-brokers.yml logs artemis
docker-compose -f demo/docker-compose-brokers.yml logs ibmmq

# Restart specific broker
docker-compose -f demo/docker-compose-brokers.yml restart artemis
```

### Poort conflict
Als poorten bezet zijn, wijzig in `docker-compose-brokers.yml`:
```yaml
ports:
  - "5673:5672"  # RabbitMQ op 5673 in plaats van 5672
```

### BlockMock endpoints werken niet
1. Check of endpoints **enabled** zijn in UI
2. Kijk naar **Request Logs** voor errors
3. Verifieer **broker type** selectie klopt
4. Check **poort nummers** en **credentials**

### Python scripts falen
```bash
# Install dependencies
pip install stomp.py

# Voor IBM MQ (complex):
# Zie installatie instructies in test-ibmmq.py
```

### IBM MQ connection issues
IBM MQ heeft soms wat langer nodig om op te starten (~1-2 minuten).

Check readiness:
```bash
docker exec demo-ibmmq dspmq
# Output should show: QMNAME(QM1) STATUS(Running)
```

---

## 📊 Success Metrics

Na de demo kan je team:
- ✅ Begrijpen hoe BlockMock meerdere protocols ondersteunt
- ✅ Zien hoe de **broker abstractie** werkt
- ✅ Zelf endpoints configureren voor hun use cases
- ✅ Weten hoe ze kunnen switchen tussen brokers
- ✅ Enthousiast zijn over multi-protocol mocking!

---

## 🎁 Bonus Demo's

### Advanced: Message Routing

Laat zien hoe routing keys werken met **TOPIC exchanges**:

1. RabbitMQ Management UI → Create bindings:
   - `order.created` → `demo.order.processing`
   - `order.shipped` → `demo.shipping.queue`
   - `order.*` → `demo.audit.queue`

2. Verstuur messages met verschillende routing keys
3. Toon hoe BlockMock alleen matched op configured patterns

### Advanced: Request Matching

HTTP endpoint met **conditional responses**:
- Status 200 als body bevat `"premium": true`
- Status 403 als body bevat `"suspended": true`
- Default 200

---

## 📚 Volgende Stappen

Na de demo kunnen developers:
1. BlockMock lokaal draaien voor development
2. Eigen endpoints configureren via UI of JSON import
3. Integreren in CI/CD pipelines
4. Uitbreiden met custom scenarios

**Documentatie**:
- `BROKER_ABSTRACTION_GUIDE.md` - Technische details
- `README.md` - Algemene setup
- API docs: http://localhost:8888/q/swagger-ui

---

## 💡 Tips voor de Demo

1. **Rehearse!** - Run alle scripts van tevoren
2. **Screenshots ready** - Voor als iets misgaat
3. **Browser tabs voorbereid** - RabbitMQ UI, Artemis console, IBM MQ console, BlockMock UI
4. **Fallback plan** - Als live demo faalt, toon screenshots/video
5. **Focus op waarde** - "Hiermee kunnen we sneller testen zonder afhankelijkheid van echte brokers"

---

## 🎬 Demo Checklist

Voordat je begint:

- [ ] Alle brokers draaien (`docker ps` toont 3 containers)
- [ ] BlockMock draait op poort 8888
- [ ] Demo endpoints geïmporteerd en enabled
- [ ] Test scripts zijn executable (`chmod +x demo/*.sh`)
- [ ] Python packages geïnstalleerd
- [ ] Browser tabs open:
  - [ ] BlockMock UI (localhost:8888)
  - [ ] RabbitMQ Management (localhost:15672)
  - [ ] Artemis Console (localhost:8161)
  - [ ] IBM MQ Console (localhost:9443)
- [ ] Terminal venster klaar voor scripts
- [ ] Request Logs tab open in BlockMock UI

**Je bent klaar! Veel succes met de demo! 🚀**
