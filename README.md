# BlockMock

**HTTP mock server for integration testing.**

BlockMock lets you define mock HTTP endpoints and verify that your application calls them correctly — in the right order, with the right frequency. Run it next to your application during tests.

## Quick Start

**Prerequisites:** Java 21+, PostgreSQL 16 (or Docker)

### 1. Start PostgreSQL
```bash
docker compose up -d postgres
```

### 2. Download and run BlockMock
Download the latest JAR from the [Releases](../../releases) page, then:
```bash
java -jar blockmock-<version>.jar
```

Open **http://localhost:8080**

### Configuration
All settings have defaults that work out of the box. Override with environment variables:

| Variable | Default | Description |
|---|---|---|
| `BLOCKMOCK_DB_URL` | `jdbc:postgresql://localhost:5432/blockmock` | JDBC URL |
| `BLOCKMOCK_DB_USER` | `blockmock` | Database user |
| `BLOCKMOCK_DB_PASSWORD` | `blockmock` | Database password |
| `BLOCKMOCK_PORT` | `8080` | HTTP port |

---

## How it works

### 1. Define mock endpoints
Create HTTP endpoints that your application will call during tests. Each endpoint returns a configurable response.

Incoming calls are intercepted at `/mock/<path>` — so an endpoint with path `/api/payment/charge` receives traffic at `http://localhost:8080/mock/api/payment/charge`.

### 2. Group endpoints into Blocks
A **Block** is a named group of endpoints. Blocks are enabled at the start of a test run (so only endpoints in the test get intercepted) and disabled when the run completes.

### 3. Define Test Suites and Scenarios
A **Test Suite** holds a set of Blocks and one or more **Scenarios**. Each Scenario defines:
- **Expectations** — which endpoints should be called, how many times, and in what order
- **Response Overrides** — force a specific mock response for one endpoint during this scenario (e.g. make the payment service return 402)
- **Triggers** — an HTTP call or cron schedule that kicks off your application

### 4. Run a scenario
1. Click **▶ Run** on a scenario — BlockMock enables the blocks and starts recording
2. Click **▶ Trigger** — BlockMock fires the trigger (calls your application)
3. Click **✓ Complete** — BlockMock evaluates expectations and shows the result

### 5. CI/CD
Use the included `ci-test.sh` script to run a scenario from your pipeline:

```bash
./demo/order-service/ci-test.sh \
  --suite "Order Flow Suite" \
  --scenario "Happy Path" \
  --url http://localhost:8080
```

Exits 0 on pass, 1 on failure. Writes JUnit XML to `ci-output/results.xml`.

---

## Demo: Order Service

The `demo/order-service/` directory contains a complete working example with a Node.js order service and a BlockMock test setup.

```bash
# Start BlockMock + PostgreSQL
docker compose up -d postgres
java -jar blockmock-<version>.jar

# Start the order service
cd demo/order-service && npm install && node server.js

# Set up test data in BlockMock (idempotent, --reset to start fresh)
bash demo/order-service/setup-blockmock.sh

# Run CI test
bash demo/order-service/ci-test.sh \
  --suite "Order Flow Suite" \
  --scenario "Happy Path"
```

---

## Building from source

```bash
# Build frontend first
cd frontend && npm ci && npm run build && cd ..

# Run in dev mode (live reload)
mvn quarkus:dev

# Build uber-jar for distribution
mvn package -DskipTests -Dquarkus.package.type=uber-jar
# Output: target/blockmock-<version>-runner.jar
```

---

## API

Full API docs at **http://localhost:8080/swagger-ui** when running.

Key endpoints:
- `POST /mock/**` — receives traffic from your application under test
- `GET /api/test-suites` — list test suites
- `POST /api/test-suites/{id}/scenarios/{sid}/runs` — start a run
- `POST /api/triggers/{id}/fire` — fire a trigger
- `POST /api/import-export/suites` — import a suite from JSON
- `GET /api/dashboard` — dashboard stats

---

## License

MIT
