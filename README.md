# Spring Modulith Shop

Demo project for the tech talk **"The Architecture Decision You Can Undo"** — a modular monolith e-commerce application built with Spring Modulith.

## What This Demonstrates

A single deployable with stricter module boundaries than most microservice architectures in production — enforced by tests, not by network latency.

### Modules

```
catalog (no dependencies)
    │
order (depends on: catalog)
    │
    ├── fulfillment (depends on: order :: events)
    ├── fraud (depends on: order :: events)
    └── notification (depends on: catalog, order :: events)
```

### Spring Modulith Features

- **Module boundaries** — `@ApplicationModule` with `allowedDependencies`, verified by `ApplicationModules.verify()`
- **Named interfaces** — `@NamedInterface("events")` for granular API exposure
- **Event-driven communication** — `@ApplicationModuleListener` for decoupled inter-module messaging
- **Event externalization** — `@Externalized` to Kafka with one annotation
- **Transactional outbox** — `EVENT_PUBLICATION` table for reliable event delivery
- **Moments API** — `DayHasPassed` for time-based batch processing, testable with `TimeMachine`
- **Schema-per-module** — each module owns its own PostgreSQL schema
- **Cross-module queries** — Hibernate `@Subselect` for read-only views without code coupling
- **Runtime data-boundary guard** — a P6Spy listener (demo profile) that rejects any single SQL statement joining two module schemas
- **Observability** — one traceId across all modules via Micrometer Tracing + OpenTelemetry; module spans visible in Jaeger
- **Actuator** — `/actuator/modulith` for runtime module introspection

### Cross-module data access — the full menu

The same question — "one module needs another module's data" — has four possible answers,
and this project demonstrates all of them, including the forbidden one:

| # | Strategy | Example here | Coupling | Freshness |
| --- | --- | --- | --- | --- |
| 1 | Direct service call | order → `CatalogService.reserveStock(...)` | code + shared transaction | strong (same TX) |
| 2 | Read-only DB view | fulfillment → `CatalogProductView` (`@Subselect`) | data only, zero code | live at query time |
| 3 | Event-carried state | `OrderCompleted` carries sku, quantity, email | none — a fact arrived | snapshot at event time |
| 0 | Cross-schema JOIN | the commented `revenue-report` | hidden data weld | blocked by the P6Spy guard |

Pick by need: writes or same-transaction consistency → call. Reads without code coupling → view.
Reacting to something that happened → the event already carries what you need. And the JOIN is
what happens when nobody chooses — which is why the guard exists.

### External Integration

- **HTTP Interface client** — Spring's declarative HTTP client (`@HttpExchange` + `@ImportHttpServices`) for the payment gateway
- **Demo stub** — in-process payment gateway stub for live demos (`@Profile("demo")`)

## Stack

| Component | Version |
| --- | --- |
| Spring Boot | 4.0.7 |
| Spring Modulith | 2.0.7 |
| Java | 25 |
| PostgreSQL | 16 |
| Kafka | 7.6.0 (KRaft) |
| Jaeger | 1.76 (OTLP) |
| Testcontainers | 2.0.4 |

## Running

### Tests

```bash
./mvnw clean verify
```

Requires Docker (Testcontainers spins up PostgreSQL and Kafka automatically).

### Live Demo

```bash
docker compose up -d          # postgres + kafka + jaeger
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

```bash
# Place an order
curl -s -X POST localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerEmail":"alice@example.com","productSku":"LAP-001","quantity":1}'

# Cancel an order
curl -s -X POST localhost:8080/api/orders/1/cancel

# Shipment report — reads catalog data through the @Subselect view (guard stays silent)
curl -s localhost:8080/api/fulfillment/orders/1/report | jq

# Check module graph
curl -s localhost:8080/actuator/modulith | jq

# Traces: one traceId across order → fraud / fulfillment / notification
open http://localhost:16686
```

## Demo crib sheet

**Boundary check (fast, offline, no DB):**
```bash
./mvnw -o -q test -Dtest=ModularStructureTest
```

**Inspect the transactional outbox:**
```bash
docker compose exec postgres psql -U shopapp -d shopapp \
  -c "SELECT event_type, listener_id, status, publication_date, completion_date FROM event_publication ORDER BY publication_date;"
```

**Watch the Kafka topic:**
```bash
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic order-completed --from-beginning \
  --property print.key=true --property print.partition=true
```

**Outbox crash/recovery beat:**
```bash
# 1. arm the one-shot listener failure, place an order, watch the WARN, then Ctrl-C the app
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo \
  -Dspring-boot.run.arguments=--demo.fulfillment.fail-once=true
# 2. restart WITHOUT the flag — the incomplete publication is redelivered and completes
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

**Cross-schema guard beat:** uncomment the revenue-report block in
`fulfillment/internal/FulfillmentReportController`, restart, then:
```bash
curl -s localhost:8080/api/fulfillment/revenue-report   # → 500, CrossSchemaJoinException
```

**Cycle beat:** uncomment the `notificationService` field + call in
`order/internal/OrderProcessor` and set `order/package-info.java` to
`allowedDependencies = { "catalog", "notification" }`, then run the boundary check —
`verify()` reports the cycle. Resolve by deleting the sync call again: the
`OrderCompleted` event already drives the same notification, in one direction.

## Pre-stage checklist

0. **Four IntelliJ terminal tabs, renamed** so grabbing the wrong one is impossible: `1-commands`, `2-app`, `3-kafka`, `4-db`. All four must sit in the repo root — the DB snippets use relative paths. Console font at projector size (Settings → Editor → Color Scheme → Console Font); terminal buffer limit raised. Clear-log is right-click → Clear Buffer — **⌘K is Commit, never on stage.**
1. `docker compose down -v && docker compose up -d` — fresh schemas, outbox, topic.
2. Comment out `@Externalized(...)` on `order/events/OrderCompleted` — it gets added back live in the Kafka beat. (The committed state keeps it so `OrderExternalizationTest` stays green.) The import may stay: an unused import is a warning, not an error, and leaving it makes the live beat a single ⌘/.
3. `./mvnw -o test -Dtest='ModularStructureTest#verifyModularStructure'` — confirms green AND that the offline flag works (conference wifi!). No `-q`: it suppresses the `Tests run:` / `BUILD SUCCESS` lines the beat narrates, while leaving the module dump on screen.
4. Boot the app in `2-app` with the demo profile, place one order, confirm the trace appears in Jaeger (localhost:16686). **The boot must happen before step 5** — see the ordering warning there.
5. **Start the Kafka consumer in `3-kafka` — `;consumer` — and only now.** The order is reset → boot → consumer, and the boot is not optional. `;reset` wipes the Kafka volumes, so `order-completed` is gone; the app's `NewTopic` bean recreates it **with 3 partitions** at startup. Start the consumer first and the broker auto-creates the topic itself with **1** partition (no `KAFKA_NUM_PARTITIONS` in `docker-compose.yml`), every message then prints `Partition:0` regardless of key, and the per-product-routing point in the Kafka beat is dead. You also get a `UNKNOWN_TOPIC_OR_PARTITION` warning sitting in a terminal that is supposed to be visibly empty all talk. Verify: `docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic order-completed` → `PartitionCount: 3`.
6. In `4-db`, run `;outbox` and `;undone` — full history returns rows, unfinished returns none. Catches a wrong directory, a wrong database and stale SQL in one go. The three statements are repo content now (`sql/outbox-*.sql`), so git restores them; the four keywords are `;outbox`, `;undone`, `;newest` and `;psql` (interactive, exit with Ctrl-D). **`-P null=NULL` is not optional** — the crash-recovery beat points at a NULL and psql prints blank without it.
7. Confirm every break-glass block is commented: boundary violation (notification), cycle field + call (order), revenue report (fulfillment), `@Externalized`.
8. **Once per conference trip, on hotel wifi:** one ONLINE `./mvnw clean verify` so every jar is cached and `-o` cannot miss anything.
9. Identity furniture: terminal badge / status line set to **Simona Oancea · simonaoancea.com** (visible in every demo and recording); speaker slide loaded as the walk-on slide — name, site, credentials, **no talk title** (the title debuts in Act 7).
10. Light kit: IDE, terminal and deck all run LIGHT themes on stage — Jaeger is light already, so nothing strobes at a switch, and light survives weak projectors. Bump IDE + terminal font sizes and verify back-row legibility (`Tests run: 1`, the 10-span trace, the 62-character outbox table).
