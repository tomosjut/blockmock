# Next Steps

## 1. TestScenario — prioriteit (architectuur)
Refactor `TestSuite` zodat deze bestaat uit `TestScenario`'s, elk met hun eigen trigger en expectations.

**Datamodel:**
```
TestSuite  (bevat gedeelde blocks/mocks)
  └── TestScenario  (name, description)
       ├── Trigger  (HTTP/AMQP/SFTP etc. — start van de integratie)
       └── Expectations  (welke mocks, hoe vaak, in welke volgorde)
```

**Wijzigingen:**
- Nieuwe entiteit `TestScenario` (vervangt de verwarrende bestaande `Scenario`/`ScenarioStep`/`ScenarioAction`)
- `TestExpectation` krijgt FK naar `TestScenario` (was `TestSuite`)
- `Trigger` krijgt FK naar `TestScenario` (was `TestSuite`)
- `TestRun` wordt per scenario (was per suite)
- Flyway migratie + frontend aanpassen

**Voorbeeld use cases:**
| Scenario | Trigger | Expectations |
|---|---|---|
| Happy path | POST /orders (geldige body) | payment 1x, inventory 1x, notification 1x |
| Payment failure | zelfde body, payment-mock geeft 402 | payment 1x, inventory 0x, notification 0x |
| Ongeldige data | body zonder totalAmount | geen mocks aangeroepen |

---

## Cleanup
- **Oude Scenario-klassen verwijderen** — `Scenario`, `ScenarioStep`, `ScenarioAction`, `ScenarioResource`, `ScenarioService` (ander concept, vervangen door TestScenario)
- **MetricsResource verwijderen** — `/api/metrics` niet meer gebruikt door frontend
- **vite.svg verwijderen** — Vite template overblijfsel in resources
- **Flyway migraties V7–V15** — SFTP, AMQP, SQL, NoSQL, Kafka, gRPC/WebSocket: geen actieve domain-klassen, beslissen of droppen
- **Ongebruikte protocol-scaffolding** — domain-klassen voor niet-geïmplementeerde protocollen opruimen

## Demo
- **Suite exporteren naar SCM** — "Order Flow Suite" exporteren en committen als `demo/order-service/order-flow-suite.json`; setup script vervangen door import
- **CI/CD script** — `ci-test.sh`: start run → fire trigger → wacht → complete → JUnit XML → non-zero exit bij failure

## Features
- **Dashboard uitbreiden** — actieve runs, recente trigger fires, suite pass/fail overzicht
- **HTTP headers in trigger-formulier** — `httpHeaders` zit in datamodel maar niet in UI
