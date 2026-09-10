# Project Rules — Smart Queue Token System

These rules govern all code work in this repository. Follow them strictly.

## Never break
- **Zero PII storage.** Never persist/log raw NIC numbers or unmasked phone numbers — always SHA-256 hash / mask. (See `systemPatterns.md`.)
- **Office isolation.** Keep `office_id` scoping on all data operations. Never merge distinct physical locations into one office.
- **No slot overselling.** Preserve atomic conditional reservation (`WHERE issued_count < max_limit`).
- **REST + stateless boundary.** The prediction microservice must stay stateless and REST-based; the backend must remain the only client of it.
- **Graceful degradation.** If the prediction service is down, the backend must fall back gracefully, never hard-fail token issuance.

## Code conventions by module
- **Backend (Java)** — package `com.smartqueue`. Layered: `controller → dto → service → repository → model`. Keep controllers thin. Add DTOs, not entities, at API boundaries.
- **Prediction (Python)** — PEP 8; Pydantic v2 schemas in `app/schemas.py`, logic in `app/services.py`. Prefer stdlib math over external deps for core formulas.
- **Web (React/JS)** — functional components + hooks; **JSX (keep it JavaScript)**, vanilla CSS, no TypeScript. Reuse components in `src/components/`; add new portals as new tab components in `App.jsx`.

## Database migrations
- **Never rely on `ddl-auto=update`** for schema changes in committed code. Add a new versioned Flyway script (`V<n>__description.sql`) in `backend/src/main/resources/db/migration/`.
- Don't edit already-applied migration files; create a new one.

## Commands to use (Windows / PowerShell)
- This shell is **PowerShell**: separate commands with `;`, never `&&`/`&`.
- Repo path has a space — always quote `"d:\Smart Queue\..."`.
- Backend build/test: `cd backend; ./mvnw.cmd test`
- Backend run: `cd backend; ./mvnw.cmd spring-boot:run`
- Prediction run: `cd prediction; .\venv\Scripts\uvicorn main:app --port 8000 --reload`
- Prediction tests: `cd prediction; .\venv\Scripts\python -m pytest tests/test_services.py`
- Web dev: `cd web; npm run dev`
- Web build/lint: `cd web; npm run build` / `npm run lint`
- Full stack: `docker compose up -d` / `docker compose down`

## Verification before finishing a task
- Run the relevant build/tests for **every** module you touched and confirm they pass:
  - Backend changes → `mvnw.cmd test`
  - Prediction changes → `pytest tests/test_services.py`
  - Web changes → `npm run build` (and `npm run lint`)
- Update `progress.md` and `activeContext.md` to reflect what changed.

## Behavior
- Match existing naming and formatting exactly (tabs in Java files, PEP 8 in Python, existing JSX style in web).
- Don't add dependencies without a real need, and confirm they're supported versions (see `techStack.md`).
- Keep documentation/docs in sync (`README.md`, `SYSTEM_GUIDE.md`) when behavior changes.