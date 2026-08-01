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
  -c "SELECT event_type, listener_id, publication_date, completion_date FROM event_publication ORDER BY publication_date;"
```

**Watch the Kafka topic:**
```bash
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic order-completed --from-beginning
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

1. `docker compose down -v && docker compose up -d` — fresh schemas, outbox, topic.
2. Comment out `@Externalized(...)` (+ its import) on `order/events/OrderCompleted` — it gets added back live in the Kafka beat. (The committed state keeps it so `OrderExternalizationTest` stays green.)
3. `./mvnw -o -q test -Dtest=ModularStructureTest` — confirms green AND that the offline flag works (conference wifi!).
4. Boot once with the demo profile, place one order, confirm the trace appears in Jaeger (localhost:16686).
5. Confirm every break-glass block is commented: boundary violation (notification), cycle field + call (order), revenue report (fulfillment), `@Externalized`.
