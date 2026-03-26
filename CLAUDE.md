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

## TypeScript discriminated unions

Volg hetzelfde patroon als de Java inheritance: gebruik discriminated unions voor polymorfe entiteiten.

```typescript
// 1. Base interface (niet exporteren)
interface FooBase {
  id?: number
  name: string
  type: FooType   // discriminant
}

// 2. Concrete interfaces met literal discriminant
export interface HttpFoo extends FooBase { type: 'HTTP'; httpUrl: string }
export interface AmqpFoo extends FooBase { type: 'AMQP'; amqpAddress: string }

// 3. Union type
export type Foo = HttpFoo | AmqpFoo

// 4. Type guards
export function isHttpFoo(f: Foo): f is HttpFoo { return f.type === 'HTTP' }
```

**Gebruik in componenten:**
- Display code: narrowing via `if (ep.protocol === 'HTTP')` of type guard
- Form state: gebruik een platte `FooForm` interface (alle velden optioneel), niet de union

**Form interface pattern:**
```typescript
/** Flat form type — all protocol-specific fields optional */
export interface FooForm {
  type?: FooType
  httpUrl?: string   // HTTP-specific
  amqpAddress?: string  // AMQP-specific
}
```

## Mock path routing
- `HttpMockResource` luistert op `@Path("/mock")`
- Inkomend pad `/mock/api/foo` → wordt gematcht op endpoint met path `/api/foo`
- Endpoints in de database bevatten **niet** het `/mock` prefix
