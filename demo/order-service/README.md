# Order Service Demo

Demonstrates BlockMock as an integration test tool. The order service calls three external services (payment, inventory, notifications) — all mocked by BlockMock.

## Prerequisites

- Java 21 + Maven (BlockMock backend)
- Node.js 18+ (order service)
- PostgreSQL running (e.g. via Docker Compose)

## Run

**Terminal 1 — BlockMock backend**
```bash
cd /home/thomas/git/blockmock
./mvnw quarkus:dev
```
Wait for `Listening on: http://0.0.0.0:8080`.

**Terminal 2 — Order service**
```bash
cd demo/order-service
npm install
node app.js
```

**Terminal 3 — Configure BlockMock (once)**
```bash
cd demo/order-service
chmod +x setup-blockmock.sh
./setup-blockmock.sh
```
This creates the 3 mock endpoints, the block, and the test suite. Re-running is safe — existing resources are linked, not duplicated.

## Run an integration test

1. Open **http://localhost:8080** (BlockMock UI)
2. Go to **Test Suites** → click **▶ Start Run** on *Order Flow Suite*
3. Click **Runs** → click **▶ Place Order** to fire the trigger
4. Click **✓ Complete** — BlockMock evaluates the expectations
5. Check the results — all 3 expectations should pass (payment → inventory → notification, in order).

Optionally download the JUnit XML via **↓ JUnit XML** for CI integration.

## Export suite to SCM

After a successful run, export the suite so it can be version-controlled and re-imported on any environment:

1. In the UI: **Test Suites** → **↓ Export** on *Order Flow Suite*
2. Save the downloaded JSON as `demo/order-service/order-flow-suite.json` and commit it
3. To import on a fresh environment: **↑ Import** in the Test Suites page and select the JSON file

## Troubleshooting

- Check the **Logs** tab to see if incoming requests are being matched by BlockMock
- If a run is stuck in RUNNING after a restart, cancel it via the Runs modal and start a new one
- The order service calls BlockMock at `BLOCKMOCK_URL` (default: `http://localhost:8080`)
