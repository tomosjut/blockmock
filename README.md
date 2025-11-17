# 🧱 BlockMock

**Een krachtige multi-protocol mock server voor integration testing**

BlockMock is een generieke mock server die verschillende protocollen en communicatie patterns ondersteunt. Perfect voor het testen van je applicaties zonder afhankelijk te zijn van externe services.

## ✨ Features

### 🌐 Multi-Protocol Support
- **HTTP/HTTPS** - REST API mocking met geavanceerde response matching
- **SFTP** - File server mocking voor upload/download testing
- **AMQP/RabbitMQ** - Message queue mocking
- **SQL Databases** - PostgreSQL, MySQL, SQL Server, Oracle (met stored procedures!)

### 🎯 Geavanceerde Functionaliteit
- **📥 Import/Export** - Exporteer en importeer endpoints als JSON
- **📋 Templates** - Voorgedefinieerde templates voor snelle setup (REST API, GraphQL, OAuth2, etc.)
- **🎬 Scenarios** - Sequentiële endpoint activatie voor complexe testscenarios
- **📊 Metrics & Monitoring** - Real-time statistics per endpoint
- **🔗 Blocks** - Groupeer endpoints voor eenvoudig beheer
- **📖 OpenAPI/Swagger** - Automatische API documentatie

### 🎨 Advanced HTTP Mocking
- Response matching op headers, query parameters en body
- Regex support voor flexibele path matching
- Multiple responses per endpoint met priority
- Configurable delays voor latency simulation
- Custom headers en status codes

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose **OF** Podman & podman-compose
- Maven 3.8+

### Installatie

```bash
# Clone de repository
git clone https://github.com/yourusername/blockmock.git
cd blockmock

# Start de applicatie
./start.sh

# Stop de applicatie (als je klaar bent)
./stop.sh
```

De applicatie is nu beschikbaar op:
- **Web UI**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui
- **OpenAPI Spec**: http://localhost:8080/openapi

### 🐳 Docker vs Podman

BlockMock ondersteunt zowel Docker als Podman! Het `start.sh` script detecteert automatisch welke container runtime beschikbaar is.

**Met Podman:**
```bash
# Installeer Podman (Ubuntu/Debian)
sudo apt-get install podman

# Installeer podman-compose
pip3 install podman-compose

# Of gebruik ingebouwde 'podman compose' (Podman 4.0+)
# Geen extra installatie nodig

# Start de applicatie (werkt hetzelfde)
./start.sh
```

**Handmatig starten met Podman:**
```bash
# Start PostgreSQL
podman-compose up -d postgres
# Of: podman compose up -d postgres

# Start de applicatie
mvn quarkus:dev

# Stop alles
podman-compose down
# Of: podman compose down
```

**Voordelen van Podman:**
- ✅ Rootless containers (betere beveiliging)
- ✅ Docker-compatibel (zelfde commando's)
- ✅ Geen daemon vereist
- ✅ Kubernetes YAML ondersteuning

## 📖 Gebruikshandleiding

### HTTP/HTTPS Mocking

1. Ga naar de **Mock Endpoints** tab
2. Klik op **+ Nieuwe Mock**
3. Kies protocol **HTTP** of **HTTPS**
4. Configureer je endpoint:
   - **Method**: GET, POST, PUT, DELETE, etc.
   - **Path**: `/api/users` (met regex support)
   - **Responses**: Meerdere responses met matching criteria

**Voorbeeld**: Mock een REST API
```json
{
  "name": "User API",
  "protocol": "HTTP",
  "httpConfig": {
    "method": "GET",
    "path": "/api/users"
  },
  "responses": [
    {
      "responseStatusCode": 200,
      "responseBody": "{\"users\": [{\"id\": 1, \"name\": \"John\"}]}",
      "responseHeaders": {
        "Content-Type": "application/json"
      }
    }
  ]
}
```

**Toegang**: `http://localhost:8080/mock/http/api/users`

### SFTP Mocking

Configureer een SFTP server voor file operations testing:

```json
{
  "name": "SFTP File Server",
  "protocol": "SFTP",
  "sftpConfig": {
    "port": 2222,
    "operation": "UPLOAD",
    "pathPattern": "/uploads/*",
    "username": "testuser",
    "password": "testpass"
  }
}
```

**Verbinden**: `sftp -P 2222 testuser@localhost`

### SQL Database Mocking

Test je database code met echte database containers:

```json
{
  "name": "PostgreSQL Test DB",
  "protocol": "SQL",
  "sqlConfig": {
    "databaseType": "POSTGRESQL",
    "databaseName": "testdb",
    "username": "testuser",
    "password": "testpass",
    "initScript": "CREATE TABLE users (id SERIAL PRIMARY KEY, name VARCHAR(100)); INSERT INTO users (name) VALUES ('John Doe');"
  }
}
```

**JDBC URL** wordt gelogd in de console na het starten.

### AMQP/RabbitMQ Mocking

Mock message queues voor event-driven architectures:

```json
{
  "name": "Order Queue",
  "protocol": "AMQP",
  "amqpConfig": {
    "exchangeName": "orders",
    "exchangeType": "topic",
    "queueName": "order.created",
    "routingKey": "order.#"
  }
}
```

## 🎨 Templates

Gebruik voorgedefinieerde templates voor snelle setup:

- **REST API** - Standard REST API endpoint
- **GraphQL** - GraphQL endpoint
- **OAuth2** - OAuth2 token endpoint
- **Webhook** - Webhook receiver
- **SFTP Server** - File server
- **Message Queue** - AMQP queue
- **SQL Database** - PostgreSQL database

Ga naar de **Templates** tab en klik op een template om deze te gebruiken.

## 🎬 Scenarios

Maak sequenties van endpoint activaties voor complexe test flows:

```json
{
  "name": "Database Migration Test",
  "steps": [
    {
      "stepOrder": 0,
      "action": "ENABLE",
      "mockEndpoint": {"id": 1}
    },
    {
      "stepOrder": 1,
      "action": "DELAY",
      "delayMs": 1000
    },
    {
      "stepOrder": 2,
      "action": "DISABLE",
      "mockEndpoint": {"id": 1}
    }
  ]
}
```

Voer scenarios uit via de API of web UI.

## 📊 Metrics & Monitoring

Monitor je endpoints real-time:

- **Total Requests** - Totaal aantal requests
- **Matched/Unmatched** - Success rate
- **Last Request** - Laatste request timestamp
- **Avg Response Time** - Gemiddelde response tijd

**API Endpoint**: `GET /api/metrics`

## 📥 Import/Export

### Exporteren
```bash
# Alle endpoints
curl http://localhost:8080/api/import-export/export > endpoints.json

# Enkele endpoint
curl http://localhost:8080/api/import-export/export/1 > endpoint.json
```

### Importeren
```bash
curl -X POST http://localhost:8080/api/import-export/import \
  -H "Content-Type": application/json" \
  -d @endpoints.json
```

## 🔗 Blocks

Groupeer gerelateerde endpoints:

1. Ga naar **Blocks** tab
2. Maak een nieuwe block
3. Selecteer endpoints
4. Start/stop alle endpoints in een block tegelijk

Perfect voor het beheren van complete test suites.

## 🛠️ API Documentatie

Volledige API documentatie is beschikbaar via:
- **Swagger UI**: http://localhost:8080/swagger-ui
- **OpenAPI Spec**: http://localhost:8080/openapi

## 🏗️ Architectuur

**Tech Stack:**
- **Framework**: Quarkus 3.17.4
- **Database**: PostgreSQL 16
- **ORM**: Hibernate Panache
- **Migrations**: Flyway
- **Containers**: Testcontainers
- **Message Broker**: RabbitMQ
- **SFTP**: Apache SSHD

**Project Structure:**
```
blockmock/
├── src/main/java/nl/blockmock/
│   ├── domain/        # JPA entities
│   ├── resource/      # REST endpoints
│   └── service/       # Business logic
├── src/main/resources/
│   ├── db/migration/  # Flyway migrations
│   └── META-INF/resources/
│       ├── css/       # Stylesheets
│       └── js/        # JavaScript
└── docker-compose.yml # PostgreSQL container
```

## 🔧 Configuratie

Configuratie in `src/main/resources/application.properties`:

```properties
# HTTP Port
quarkus.http.port=8080

# Database
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/blockmock
quarkus.datasource.username=blockmock
quarkus.datasource.password=blockmock

# OpenAPI
quarkus.swagger-ui.path=/swagger-ui
```

## 📝 Development

```bash
# Development mode (live reload)
./mvnw quarkus:dev

# Build
./mvnw clean package

# Run tests
./mvnw test

# Build container image (Docker)
./mvnw package -Dquarkus.container-image.build=true

# Build container image (Podman)
./mvnw package -Dquarkus.container-image.build=true \
  -Dquarkus.container-image.builder=podman
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the MIT License.

## 🙏 Credits

Gebouwd met:
- [Quarkus](https://quarkus.io/)
- [Testcontainers](https://www.testcontainers.org/)
- [Apache SSHD](https://mina.apache.org/sshd-project/)
- [RabbitMQ](https://www.rabbitmq.com/)

---

**Happy Mocking!** 🎭
