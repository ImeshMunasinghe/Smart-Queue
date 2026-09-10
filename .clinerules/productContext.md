# Product Context — Smart Queue Token System

## Why this project exists
Physical queuing at **government offices** (Divisional Secretariat, Grama Niladhari counters) and **hospital Outpatient Departments (OPDs)** causes:
- Severe overcrowding and long physical queues.
- Unpredictable waiting times and poor citizen experience.
- Low counter utilization for staff.

## What the system solves
1. **Virtual/remote token booking** via omnichannel touchpoints — Citizen **Web App**, **USSD** (*123#), and **SMS**.
2. **Calibrated real-time wait-time estimates** (P50/P90 confidence) using a load-balancer + a lightweight prediction service.
3. **Probabilistic overbooking** based on predicted no-show rates to maximize counter throughput without physical overcrowding (risk kept under a configurable policy, default **10%**).
4. **High-efficiency counter operator workstation** with rapid keyboard hotkeys.

## Users / personas
- **Citizens** — request a token remotely, check live status, cancel / reclaim slot.
- **Counter operators** — desktop call/serve/skip workflow with keyboard-first ergonomics.
- **Admin capacity planners** — configure capacity limits, overbooking risk, and review audit trails.
- **Waiting-hall visitors** — read the big-screen public TV display (multi-counter matrix).

## Design principles / non-negotiables (from the source docs)
- **Zero PII storage guarantee.** Raw NIC numbers and unmasked phone numbers are never persisted. All queries/logs use SHA-256 hashes and masked phone numbers.
- **Office isolation.** `office_id` sits on every schema table; *never merge distinct physical locations into one office record*.
- **Zero slot overselling** via atomic conditional DB updates (`WHERE issued_count < max_limit`).
- **Stateless, REST-first** prediction microservice with graceful, heuristic cold-start fallback.
- Validate & normalize both legacy 9-digit NIC (`123456789V`) and modern 12-digit (`123456789012`).