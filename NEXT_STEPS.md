# Next Steps

## Cleanup
- **Old UI verwijderen** — de originele vanilla JS/HTML/CSS UI in `src/main/resources/META-INF/resources/` (index.html, js/app.js, css/style.css) is vervangen door de React build. De oude bestanden kunnen weg.
- **Ongebruikte domain-klassen** — er zijn scaffolding-klassen voor protocollen die nog niet geïmplementeerd zijn (NoSQL, AMQP, SFTP, Kafka, gRPC configs). Bekijken wat weg kan en wat bewust als toekomstige extensie bewaard blijft.
- **Flyway migraties opruimen** — migraties voor tabellen zonder actieve domain-klassen (V7 SFTP, V8 AMQP, V9 SQL, V12 NoSQL, V13 Kafka, V14 gRPC/WebSocket). Beslissen: bewaren of droppen.
- **ImportExportResource** — de oude endpoint-export/import endpoints (exportAll, exportSingle, importEndpoints, importSingleEndpoint) zijn vervangen door de suite import/export. Die kunnen weg.

## Demo
- **Suite exporteren naar SCM** — na een succesvolle run de "Order Flow Suite" exporteren (↓ Export) en het JSON-bestand committen als `demo/order-service/order-flow-suite.json`. Het setup script kan dan vervangen worden door een import van dat bestand.
- **CI/CD script** — een `ci-test.sh` script aanmaken dat de volledige flow uitvoert: start run → fire trigger → wacht → complete → JUnit XML ophalen → non-zero exit bij failure.

## Features
- **Dashboard uitbreiden** — nu alleen endpoints + log-stats. Toevoegen: actieve runs, recente trigger fires, suite pass/fail overzicht.
- **HTTP headers in trigger-formulier** — het `httpHeaders` veld zit in het datamodel maar niet in de UI.
- **Auto-complete run** — optionele instelling op een trigger om de run automatisch te completen na X seconden (voor volledig hands-off gebruik).
