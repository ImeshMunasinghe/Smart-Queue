# SmartQueue — Core Backend Service

Production-grade Spring Boot 3.4.3 (Java 21) service powering token lifecycle management, concurrency control, load balancing, real-time push streaming, and administrative governance.

---

## 🏛️ Architectural Highlights

### 1. Atomic Capacity Reservation (Zero-Overselling Guarantee)
To guarantee NFR-1.2 and NFR-6.2 under high concurrent load, slot reservations bypass application-level check-then-act race conditions using atomic conditional database updates:
```sql
UPDATE SlotCapacity s
SET s.issuedCount = s.issuedCount + 1,
    s.activeWaitingCount = s.activeWaitingCount + 1,
    s.version = s.version + 1
WHERE s.officeId = :officeId
  AND s.id = :slotId
  AND s.issuedCount < COALESCE(s.manualOverrideLimit, CASE WHEN s.computedLimit > 0 THEN s.computedLimit ELSE s.rawCapacity END)
```
If 0 rows are updated, the session has reached capacity, cleanly rejecting the request without database lock escalations.

### 2. Database-Enforced State Machine (FR-3.3)
Token transitions are guarded by both application logic and PostgreSQL function triggers:
- `WAITING ➔ CALLED`, `WAITING ➔ CANCELLED`
- `CALLED ➔ SERVING`, `CALLED ➔ NO_SHOW`, `CALLED ➔ SKIPPED`, `CALLED ➔ CANCELLED`
- `SKIPPED ➔ CALLED`, `SKIPPED ➔ CANCELLED`
- `SERVING ➔ COMPLETED`
*All other transitions (such as `COMPLETED ➔ WAITING`) are rejected at the database level.*

### 3. Dual NIC Format Validation
Validated via regex `^([0-9]{9}[vVxX]|[0-9]{12})$`:
- **Legacy 9-digit format** with letter suffix (e.g., `145896235V`, `v`, `X`, `x`), automatically normalized to uppercase.
- **Modern 12-digit format** (e.g., `144756235896`).
- NIC numbers are hashed via SHA-256 (`citizen_reference_hash`) for citizen privacy compliance (NFR-4.4).

### 4. Resilient Server-Sent Events (SSE)
- Streams real-time updates via `/api/v1/tokens/{id}/stream`.
- Handles intermittent mobile connectivity using `Last-Event-ID` re-sync to replay missed state transitions.
- Dispatches `:keep-alive` comments every 15 seconds to prevent mobile carrier TCP timeouts.

### 5. Multi-Counter Load Balancer (ECT Minimization)
Routes tokens to eligible online counters by minimizing:
$$\text{ECT} = \text{now} + \text{RemainingTime}(\text{CurrentToken}) + \sum_{t \in \text{Queue}} \text{PredictedServiceTime}(t)$$

---

## 📡 REST API Reference

### Token Endpoints (`/api/v1/tokens`)
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/tokens` | Issue virtual queue token (idempotent, atomic capacity reservation) |
| `GET` | `/api/v1/tokens/{id}/status` | Query real-time token state, queue position, and ETA |
| `GET` | `/api/v1/tokens/{id}/stream` | SSE push stream for live status updates (`Last-Event-ID` supported) |
| `POST` | `/api/v1/tokens/{id}/cancel` | Citizen cancellation; releases slot capacity atomically |

### Operator Station Endpoints (`/api/v1/operator`)
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/operator/counters/{id}/call-next` | Call next waiting token in eligible queue |
| `POST` | `/api/v1/operator/tokens/{id}/serve` | Mark arrival and start service consultation |
| `POST` | `/api/v1/operator/tokens/{id}/complete` | Complete transaction and record duration log |
| `POST` | `/api/v1/operator/tokens/{id}/skip` | Place citizen into temporary skipped pool |
| `POST` | `/api/v1/operator/tokens/{id}/recall` | Recall previously skipped citizen |
| `POST` | `/api/v1/operator/tokens/{id}/no-show` | Forfeit slot on citizen absence |
| `GET` | `/api/v1/operator/counters/{id}/queue` | Fetch upcoming queue of tokens for counter |
| `PUT` | `/api/v1/operator/counters/{id}/status` | Update counter status (`ONLINE`, `PAUSED`, `OFFLINE`) |

### Admin & Analytics Endpoints (`/api/v1/admin`)
| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/admin/offices` | List all offices |
| `GET` | `/api/v1/admin/offices/{id}/service-types` | List active service types for office |
| `GET` | `/api/v1/admin/offices/{id}/slots` | List slot capacities and overbooking limits |
| `PUT` | `/api/v1/admin/slots/{id}/override` | Set manual capacity ceiling override |
| `GET` | `/api/v1/admin/offices/{id}/analytics` | Fetch throughput, active queue, and slot telemetry |

### SMS Telco Webhooks (`/api/v1/webhooks/sms`)
| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/webhooks/sms/inbound` | Inbound SMS command parser (`STATUS`, `CANCEL`, `HELP`) |

---

## 🧪 Testing & Verification

The test suite covers full lifecycle transitions, dual NIC validation, idempotency caching, active token constraints, and concurrent zero-overselling defense:

```bash
# Run Maven test suite
./mvnw.cmd test
```

**Results**: 7 tests run, 0 failures, 0 errors (**100% passing**).

---

## 🏃 Running the Backend Locally

```bash
# Start Spring Boot application on http://localhost:8080
./mvnw.cmd spring-boot:run
```
*On initial startup, `DataInitializer` automatically seeds the Colombo Divisional Secretariat Pilot Office, 3 pilot counters, and today's session capacities.*
