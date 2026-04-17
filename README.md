# Spring Modulith Shop

Demo project for the tech talk **"The Architecture Decision You Can Revert"** — a modular monolith e-commerce application built with Spring Modulith.

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
- **Observability** — traceId correlation across modules via Micrometer
- **Actuator** — `/actuator/modulith` for runtime module introspection

### External Integration

- **Feign client** — declarative HTTP client for payment gateway (`@FeignClient`)
- **Demo stub** — in-process payment gateway stub for live demos (`@Profile("demo")`)

## Stack

| Component | Version |
|---|---|
| Spring Boot | 4.0.5 |
| Spring Modulith | 2.0.5 |
| Spring Cloud (OpenFeign) | 2025.1.1 |
| Java | 25 |
| PostgreSQL | 16 |
| Kafka | 7.6.0 (KRaft) |
| Testcontainers | 2.0.4 |

## Running

### Tests

```bash
./mvnw clean verify
```

Requires Docker (Testcontainers spins up PostgreSQL and Kafka automatically).

### Live Demo

```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

```bash
# Place an order
curl -s -X POST localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerEmail":"alice@example.com","productSku":"LAP-001","quantity":1}'

# Cancel an order
curl -s -X POST localhost:8080/api/orders/1/cancel

# Check module graph
curl -s localhost:8080/actuator/modulith | jq
```
