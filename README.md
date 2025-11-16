# BlockMock

Een generieke mock server die verschillende protocollen en communicatie patterns ondersteunt.

## Features

- **Meerdere protocollen**: HTTP(S), SFTP, AMQP, MQTT (in ontwikkeling)
- **Communicatie patterns**: Request/Reply, Fire/Forget, Pub/Sub
- **Flexibele matching**: Path, headers, query params, body matching
- **Request logging**: Alle inkomende requests worden gelogd
- **Web UI**: Vaadin-based management interface
- **REST API**: Voor programmatische configuratie

## Tech Stack

- **Backend**: Quarkus 3.17.4, Java 21
- **Frontend**: Vaadin 24.5.5
- **Database**: PostgreSQL
- **ORM**: Hibernate Panache
- **Migrations**: Flyway

## Vereisten

- Java 21
- Maven 3.8+
- PostgreSQL 12+

## Database Setup

Maak een PostgreSQL database aan:

```sql
CREATE DATABASE blockmock;
CREATE USER blockmock WITH PASSWORD 'blockmock';
GRANT ALL PRIVILEGES ON DATABASE blockmock TO blockmock;
```

Of pas de database configuratie aan in `src/main/resources/application.properties`.

## Applicatie starten

### Development mode

```bash
mvn quarkus:dev
```

De applicatie is beschikbaar op:
- Web UI: http://localhost:8080
- Mock endpoints: http://localhost:8080/mock/http/*
- REST API: http://localhost:8080/api/*

### Production build

```bash
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar
```

## Gebruik

### Mock Endpoint aanmaken via UI

1. Open de Web UI op http://localhost:8080
2. Ga naar de tab "Mock Endpoints"
3. Klik op "+ Nieuwe Mock"
4. Vul het formulier in:
   - **Naam**: Beschrijvende naam voor je mock
   - **Protocol**: Kies HTTP/HTTPS
   - **Pattern**: Request/Reply
   - **HTTP Method**: GET, POST, PUT, DELETE, etc.
   - **Path**: Het pad waarop de mock reageert (bijv. `/api/users`)
   - **Response Status**: HTTP status code (bijv. 200)
   - **Response Body**: De body die teruggegeven wordt (JSON, XML, text)
   - **Response Headers**: Optionele headers als JSON object
   - **Delay**: Simuleer latency in milliseconden
5. Klik op "Opslaan"

### Mock bewerken of verwijderen

- Klik op het **✏️ icoon** om een mock te bewerken
- Klik op **▶️/⏸️** om een mock te activeren/deactiveren
- Klik op **🗑️** om een mock te verwijderen

### Request Logs bekijken

1. Ga naar de tab "Request Logs"
2. Logs worden automatisch ververst elke 5 seconden
3. Klik op een log regel om details te zien:
   - Volledige request headers, query params, en body
   - Response details
   - Matching status

### HTTP Mock voorbeeld

Een HTTP mock endpoint reageert op requests naar `/mock/http/{jouw-path}`.

Bijvoorbeeld:
- Mock configuratie: `GET /api/users`
- Bereikbaar via: `http://localhost:8080/mock/http/api/users`

## API Endpoints

### Management API

- `GET /api/endpoints` - Lijst alle mock endpoints
- `POST /api/endpoints` - Maak nieuwe mock endpoint
- `GET /api/endpoints/{id}` - Haal specifieke endpoint op
- `PUT /api/endpoints/{id}` - Update endpoint
- `DELETE /api/endpoints/{id}` - Verwijder endpoint
- `POST /api/endpoints/{id}/toggle` - Toggle enabled status

### Request Logs

- `GET /api/logs` - Alle request logs
- `GET /api/logs/recent?limit=100` - Recente logs
- `GET /api/logs/endpoint/{id}` - Logs voor specifiek endpoint
- `GET /api/logs/stats` - Statistieken
- `DELETE /api/logs` - Wis alle logs

## Roadmap

### Fase 1: Core & HTTP (✓ Voltooid)
- [x] Project setup
- [x] Database schema
- [x] Domain model
- [x] HTTP protocol handler
- [x] Basic HTML/JS UI

### Fase 2: UI Verbetering (✓ Voltooid)
- [x] Mock endpoint create/edit forms
- [x] Response configuratie UI
- [x] Request log detail view
- [x] Auto-refresh voor logs (elke 5 seconden)

### Fase 3: SFTP Protocol
- [ ] SFTP server implementatie
- [ ] File mocking
- [ ] SFTP-specifieke UI

### Fase 4: AMQP Protocol
- [ ] AMQP listener
- [ ] Queue/Exchange mocking
- [ ] Pub/Sub pattern

### Fase 5: Advanced Features
- [ ] Import/export configuraties
- [ ] Scenario's
- [ ] Stateful mocks
- [ ] Template engine (Handlebars/FreeMarker)

## Licentie

MIT
