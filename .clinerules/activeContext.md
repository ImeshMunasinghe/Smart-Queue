# Active Context — Current State

## Current milestone
The repository is in the **Pilot Stage**: a single seeded office (Colombo Divisional Secretariat Pilot Office) running a stateless queue service with conservative heuristic rules. No ML models are trained yet — the prediction service operates in cold-start `HEURISTIC` mode.

## Recent work (newest first)
- `c7eb45d` docs: refresh quick-start, run workflow, service URL reference, and stop instructions.
- `d972bc6` web: added backend reconnect polling and loading fallbacks for service categories and counters.
- `6693a63` backend: configure PostgreSQL driver, enable Flyway migrations, optimize Docker packaging.
- `027c620` docs: comprehensive system architecture guide + operational manual (`SYSTEM_GUIDE.md`).
- `e26dd6b` web: implements React 19 queue-tracking portal, operator station, and admin console.
- `3445d19` prediction: wait-time estimation, no-show model, and binomial overbooking service.

## Active concerns / decisions in flight
- Prediction microservice uses **heuristic rules** (`source="HEURISTIC"`); `lightgbm`, `scikit-learn`, `numpy`, `pandas` are declared in `requirements.txt` but **not yet exercised** — ML wiring is future work.
- Redis is declared **only in `docker-compose.yml`** (idempotency/session TTL). The backend `pom.xml` has **no Redis/Jedis/Lettuce dependency**, so no backend code talks to Redis yet.
- Backend defaults to **H2 in PostgreSQL mode** for instant local dev; the `SPRING_DATASOURCE_*` env vars (set in Docker) switch it to real Postgres 16.
- NIC validation regex used across layers: `^([0-9]{9}[vVxX]|[0-9]{12})$`.

## Environment gotchas (Windows / PowerShell)
- The shell is **PowerShell** — do **not** use `&&`, `;` is the separator. PowerShell errors on `&` unless quoted.
- The repo path contains a **space** (`d:\Smart Queue`) — always quote paths.
- Maven wrapper is `./mvnw.cmd` (Windows). Python venv is at `prediction\venv`.