# Tech Stack — Smart Queue Token System

Monorepo with three sub-projects plus Docker orchestration.

## 1. Backend — `backend/`
- **Java 21**, **Spring Boot 3.4.3**, Maven wrapper (`./mvnw.cmd`).
- **Spring Data JPA / Hibernate**, **Flyway** for migrations, **Bean Validation**, **Spring Actuator**.
- DB drivers: **PostgreSQL 16** (runtime/prod) and **H2** (in-memory, PostgreSQL compatibility mode — local default).
- Layout (package `com.smartqueue`):
  - `config/` → `DataInitializer` (seeds pilot office on boot)
  - `controller/` → `TokenController`, `OperatorController`, `AdminController`, `SmsWebhookController`
  - `dto/` → `IssueTokenRequest`, `TokenResponse`
  - `model/` → JPA entities + enums
  - `repository/` → Spring Data repositories
  - `service/` → `TokenService`, `LoadBalancerService`, `PredictionClient`, `SseEmitterService`, `sms/` (`SmsGatewayProvider`, `MockSmsGateway`)
- Key config (`src/main/resources/application.properties`):
  - `server.port=8080`
  - `prediction.service.url=${PREDICTION_SERVICE_URL:http://localhost:8000}`
  - `queue.called-timeout-minutes=3`
  - `queue.overbooking.default-risk-threshold=0.10`
  - `spring.jpa.hibernate.ddl-auto=update`; Flyway `baseline-on-migrate=true`
- DB migrations in `src/main/resources/db/migration/`: `V1__init_smart_queue_schema.sql`, `V2__seed_pilot_office_data.sql`.

## 2. Prediction microservice — `prediction/`
- **Python 3.11+**, **FastAPI**, **Pydantic v2**, **Uvicorn**.
- `requirements.txt`: fastapi, uvicorn[standard], pydantic, lightgbm, scikit-learn, numpy, pandas, httpx, pytest.
- Entry: `main.py` (FastAPI app, CORS open). Domain code in `app/` (`schemas.py`, `services.py`). Tests in `tests/`.
- Endpoints: `GET /health`, `POST /predict/wait-time`, `POST /predict/no-show`, `POST /calculate/overbooking`.
- **Port 8000**; Swagger UI at `http://localhost:8000/docs`.

## 3. Web portal — `web/`
- **React 19**, **Vite 8**, plain JS (**JSX**, not TypeScript), **vanilla CSS**, **oxlint** for linting.
- Components (`src/components/`): `CitizenPortal`, `OperatorTerminal`, `PublicDisplayBoard`, `AdminConsole`, `SmsSandbox`, `Navbar`, `Icons`.
- **Port 5173**; dev server proxies `/api` to the backend.

## 4. Infrastructure — `docker-compose.yml` (root)
Containers: `postgres:16-alpine`, `redis:7-alpine`, `prediction-service` (build `./prediction`), `backend` (build `./backend`).
- Ports: Postgres `5432`, Redis `6379`, Prediction `8000`, Backend `8080`. Named volume `pgdata`.

## Ports & URLs summary
| Service | Port/URL |
|---|---|
| Citizen / Operator / Admin / TV / SMS web UI | `http://localhost:5173` |
| Prediction API + Swagger | `http://localhost:8000` / `/docs` |
| Backend REST API | `http://localhost:8080` |
| Backend health actuator | `http://localhost:8080/actuator/health` |