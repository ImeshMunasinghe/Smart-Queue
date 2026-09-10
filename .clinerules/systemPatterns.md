# System Patterns — Architecture & Conventions

## High-level architecture
```
Clients (Web / USSD / SMS / Operator / Admin / TV)
        │
        ▼
Backend (Spring Boot 3.4.3) ── REST/JSON ──▶ Prediction (FastAPI)
        │                                        (heuristic cold-start)
        │
   ┌────┴────┐
 Postgres 16  Redis 7
 (offices)   (idempotency/session — declared, not yet wired into backend code)
```

## Backend structure & patterns
- **Layered**: `controller → dto → service → repository → model`. Controllers are thin; business logic lives in services.
- **State machine** enforced via `Token` status transitions (`TokenState` enum) + `TokenStateTransition` audit records. Transitions are also backed by DB triggers/audit.
- **Zero-overselling defense**: atomic conditional reservation (`UPDATE ... WHERE issued_count < max_limit`).
- **Load balancing**: `LoadBalancerService` routes tokens to the counter minimizing **Estimated Completion Time (ECT)**, honoring counter↔service-type eligibility.
- **SSE push**: `SseEmitterService` streams live queue updates to web clients; supports `Last-Event-ID` reconnect sync.
- **SMS**: pluggable `SmsGatewayProvider`; a zero-cost `MockSmsSandbox` implements `STATUS <token>`, `CANCEL <token>`, `HELP`. Inbound handled by `SmsWebhookController`.
- **Prediction client**: `PredictionClient` calls the FastAPI service; must degrade gracefully if it is down.

## Data model (`backend/src/main/java/com/smartqueue/model`)
- `Office` — a single physical location; owns all data via `office_id`.
- `ServiceType` — services a counter can handle (eligibility matrix).
- `Counter` (+ `CounterStatus`) — physical counters assigned to an office.
- `SlotCapacity` — capacity limits per office/service/time-slot.
- `Token` — issued queue position; full lifecycle (`TokenState`).
- `TokenStateTransition` — immutable audit trail of state changes.
- `ServiceTimeLog` — measured per-service durations used for telemetry.
- Enums: `TokenState`, `CounterStatus`.

## Token lifecycle (`TokenState`)
- `WAITING ➔ CANCELLED` (self-service withdrawal reclaims slot).
- `WAITING ➔ CALLED` → `SERVED` | `SKIPPED` | `NO_SHOW` | (auto `NO_SHOW` after 3-min timeout).
- `CALLED ➔ CANCELLED` also supported; slot reclaimed on withdrawal/no-show.

## Multi-tenant / office model
- Every schema table carries `office_id` (see Flyway `V1`).
- Adding a real office = inserting a new `offices` row. **Never** merge distinct physical buildings into one office.

## Prediction service patterns
- Stateless; all logic in `app/services.py` (currently heuristic). Schemas live in `app/schemas.py` (Pydantic v2 with `Field` constraints).
- Overbooking = exact **binomial tail** `P(X > capacity) ≤ α` (default α = 0.10), capped by `max_overbooking_ratio` (default 1.35×).

## Coding conventions
- **Java**: `com.smartqueue` package root; Spring-injected constructors; JPA entities in `model`, DTOs in `dto`.
- **Python**: PEP 8; one service class `PredictionService`; domain Pure-Python math (stdlib `math`).
- **JS/React**: functional components (hooks), JSX (no TS), vanilla CSS files per concern, single `App.jsx` orchestrating tabs.

## Privacy invariant
Never persist raw NIC or unmasked phone numbers. Always hash (SHA-256) / mask before storing or logging.