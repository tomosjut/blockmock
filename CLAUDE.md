# BlockMock — ontwikkelstandaarden

## Stack
- **Backend**: Java 21, Quarkus 3.17.4, Hibernate ORM Panache, PostgreSQL, Flyway
- **Frontend**: React 18 + TypeScript, Vite — broncode in `frontend/`, build output naar `src/main/resources/META-INF/resources/`

## Frontend bouwen
Altijd bouwen voor starten of committen:
```bash
cd frontend && npm run build
```

## Java conventies
- Entities: `PanacheEntity` + Lombok `@Getter/@Setter` + `@PrePersist/@PreUpdate`
- JSONB velden (Map): `@Type(JsonBinaryType.class)` + `@Column(columnDefinition = "jsonb")`
- Bidirectionele relaties: `@JsonManagedReference` op parent, `@JsonBackReference` op child
- Wanneer een child-entiteit geserialiseerd wordt maar de parent niet volledig nodig is: gebruik `@JsonIgnoreProperties` op het parent-veld
- Sequenties: `CREATE SEQUENCE IF NOT EXISTS FooEntity_SEQ START WITH 1 INCREMENT BY 50`

## Flyway
- Migraties in `src/main/resources/db/migration/`
- Naamgeving: `V{n}__{omschrijving}.sql`
- Huidige versie: **V1** (gesquasht — volgende migratie wordt V2)

## Import/export
- Geen IDs in export-formaat — alles gematcht op natural keys:
  - `MockEndpoint`: `(httpMethod, httpPath)`
  - `Block`: `name`
  - `TestSuite`: `name` (UNIQUE constraint)
  - `TriggerConfig`: `(testScenario, name)` (UNIQUE constraint)

## Architectuur
- `TestSuite` bevat gedeelde blocks/mocks
- `TestScenario` heeft eigen expectations, triggers, runs en responseOverrides
- `TestRun` is per scenario

## Mock path routing
- `HttpMockResource` luistert op `@Path("/mock")`
- Inkomend pad `/mock/api/foo` → wordt gematcht op endpoint met path `/api/foo`
- Endpoints in de database bevatten **niet** het `/mock` prefix
