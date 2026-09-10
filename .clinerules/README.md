# Smart Queue — Cline Memory Bank & Rules

> This folder is the **persistent memory bank** for the **Smart Queue Token System**. Every `*.md` file here is auto-loaded by Cline as rule context on each session, so it must be kept accurate, current, and concise.

## How to use & maintain

- **Read first.** Before making changes, consult `techStack.md`, `systemPatterns.md`, and `activeContext.md` so your work fits existing conventions.
- **Update after every significant change.** Reflect completed work in `progress.md` and the current concern in `activeContext.md`.
- **Keep it truthful.** Never document something as done unless it was verified (tests/build actually pass).
- **Keep it concise.** These files are loaded every session; prefer short, high-signal bullets over prose.

## Index

| File | Purpose |
|------|---------|
| `productContext.md` | Why this project exists, the domain problem, and the users it serves. |
| `activeContext.md` | What is happening right now, recent changes, and active concerns/risks. |
| `techStack.md` | Technologies, versions, ports, and every run/build/test command. |
| `systemPatterns.md` | Architecture, data model, state machine, and coding conventions. |
| `progress.md` | Verified delivered features, test status, and the remaining roadmap. |
| `projectRules.md` | Explicit rules the agent must follow when working in this repo. |
| `lazyMode.md` | Lazy senior dev mode — the "climb-the-ladder" efficiency rules and `ponytail:` annotation convention. |