# Spring Modulith Shop

Demo project for the tech talk **"The Architecture Decision You Can Undo"** — a modular monolith e-commerce application built with Spring Modulith.

**Slides:** [The Architecture Decision You Can Undo (PDF)](https://github.com/simonaOancea/spring-modulith-shop/releases/latest/download/slides.pdf) — the talk this repo is the live demo for.

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

## Walk the talk's demos yourself

The talk breaks five things and puts every one of them back. Here they are in talk order,
each with the change to make, what you'll see, and the undo. Start with the app running
(`docker compose up -d`, then `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo`) and
keep a second terminal for the commands.

The three outbox queries live in `sql/`. The `PGTZ` flag makes their `done` column show your
local time instead of the container's UTC — replace the `$(…)` with a zone name such as
`Europe/Berlin` if your OS has no `/etc/localtime` link.

```bash
OUTBOX='docker compose exec -T -e PGTZ="$(readlink /etc/localtime | sed "s|.*zoneinfo/||")" postgres psql -U shopapp -d shopapp -P null=NULL -P border=2 -f -'
```

### 1. Break a boundary

The structure test is the boundary. Green first:

```bash
./mvnw test -Dtest=ModularStructureTest
```

Now uncomment `violateBoundary()` in `notification/internal/BoundaryViolationExample.java`
(the type is fully qualified, so there is no import to add) and run the test again. It fails
with an *illegal dependency*: notification reaches into fulfillment's internals, which its
`package-info.java` never allowed:

```
Module 'notification' depends on module 'fulfillment' via …BoundaryViolationExample -> …Shipment.
Allowed targets: catalog, order :: events.
```

**Undo:** comment the method out again. Green.

### 2. Events you can trust — the outbox

Place an order and look at the outbox. Every listener of every event gets its own row, and a
row is only written when the order itself is committed — same transaction, never one without
the other:

```bash
curl -s -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"customerEmail":"alice@example.com","productSku":"LAP-001","quantity":1}'
sh -c "$OUTBOX" < sql/outbox-full.sql
```

Now make a listener die. Restart the app with the one-shot failure flag, place another
order, and watch the fulfillment listener throw once:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo \
  -Dspring-boot.run.arguments=--demo.fulfillment.fail-once=true
```

```bash
sh -c "$OUTBOX" < sql/outbox-undone.sql     # fulfillment's row: status FAILED, done NULL
```

Ctrl-C the app and start it again **without** the flag. On boot, Spring Modulith republishes
every incomplete row: the log prints `Shipment created for order #N`, and the same query now
returns nothing. No broker was involved — a database row and a restart.

Delivery is at-least-once, so listeners are idempotent (see `FulfillmentProcessor`: check
before you insert).

### 3. Build a cycle

Events don't make dependencies disappear — a listener that names `OrderCompleted` depends on
the order module. Close the loop by calling back the other way: in
`order/internal/OrderProcessor.java` uncomment the `notificationService` field and the call
after `order.complete()`, and in `order/package-info.java` widen the declaration to
`allowedDependencies = { "catalog", "notification" }`. Everything is declared on both sides,
and the structure test still fails:

```
Cycle detected: Slice notification -> Slice order -> Slice notification
```

**Undo:** re-comment the field and the call, restore `{ "catalog" }`. The event already drives
the same notification, in one direction.

### 4. Read the data, not the code — then kill a join

Fulfillment's report shows product names and prices, yet fulfillment imports nothing from
catalog. It reads `catalog.products` through `CatalogProductView`, a read-only `@Subselect`
entity — data coupling written down in one place, findable with one search for the table name:

```bash
curl -s localhost:8080/api/fulfillment/orders/1/report | jq     # any completed order id
```

That single-schema read is allowed on purpose. What the guard kills is a statement that
touches two or more module schemas. Uncomment the `revenue-report` block in
`fulfillment/internal/FulfillmentReportController.java`, restart, and call it:

```bash
curl -s localhost:8080/api/fulfillment/revenue-report
# {"status":500,"error":"cross-schema join rejected","schemas":"catalog, fulfillment, orders"}
```

The query never reached Postgres. `guards/AssertQueriesDontJoinSchemas` sits in the JDBC
driver (P6Spy, demo profile), reads the SQL before it runs, and rejects it — the coupling
`ApplicationModules.verify()` cannot see, because there is no import to catch. It is a
teaching device; GitHub ran the production version of the idea (schema domains plus a query
linter) for years before splitting their database.

**Undo:** re-comment the block.

### 5. Leave the process

`OrderCompleted` carries `@Externalized("order-completed::#{#this.productSku()}")`: the part
before `::` is the topic, the rest makes the SKU the message key, so events for one product
stay in order on one partition. Watch it arrive — start the consumer **after** the app has
booted once, so the topic exists with its three partitions:

```bash
docker compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-completed --from-beginning --property print.key=true --property print.partition=true
```

Place an order; the consumer prints the event with key `LAP-001`. Then look at the outbox
newest-first: Kafka is just one more listener in that table, with the same guarantees —
broker down means an incomplete row, republished on restart:

```bash
sh -c "$OUTBOX" < sql/outbox-newest.sql
```

**Undo:** comment the annotation out and restart. The in-process listeners keep working;
nothing leaves the JVM. That is the first of the four extraction steps in the talk, and the
one that costs a single line.

### Reset

```bash
docker compose down -v && docker compose up -d     # fresh schemas, empty outbox, new topic
```
