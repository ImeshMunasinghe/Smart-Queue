# Progress — Delivered, Verified, and Remaining

## Verified delivered (per repo docs)
- **Backend** — Spring Boot service with Flyway schema, entities, repositories, token engine, atomic slot concurrency defense, load balancer, state machine, SSE hub, SMS webhook + sandbox. Test suite `mvnw test`: **7/7 pass** (Concurrency, Lifecycle, Dual NIC, Idempotency).
- **Prediction** — FastAPI service: wait-time (P50/P90), no-show probability, exact binomial overbooking. `pytest tests/test_services.py`: **3/3 pass** (binomial tail math, peak hours).
- **Web** — React 19 portal (Citizen Portal, Queue Tracker, Operator Station with hotkeys, Admin Console, SMS Sandbox, Public TV board). `npm run build`: **16 modules compile, 0 errors**.
- **Infra** — Docker Compose orchestrates all services; docs (`README.md`, `SYSTEM_GUIDE.md`).

## Not yet done / planned (Scale stage)
- Train ML models — prediction is **still heuristic** (`lightgbm`/`scikit-learn` only in requirements).
- Wire the backend to **Redis** (declared in Compose, no backend dependency/code yet).
- Multi-office rollout, Kubernetes, Kafka/RabbitMQ, Airflow — only after measured load justifies them.

## Verification commands
- Backend: `cd backend; ./mvnw.cmd test`
- Prediction: `cd prediction; .\venv\Scripts\python -m pytest tests/test_services.py`
- Web: `cd web; npm run build; npm run lint`

> Update this file every time a feature is completed **and** verified.