# Multi-agent development workflow

Use this after Phase 0 so parallel Cursor agents do not collide.

## Rules

1. **One agent per branch** — never two agents on the same branch or overlapping file sets.
2. **Implement against contracts** — [api-contract.md](api-contract.md) is the source of truth until OpenAPI is generated from code.
3. **Auth/security PRs first** — human review before merging auth-related work into `develop`/`main`.
4. **No secrets in git** — use `.env` locally; only `.env.example` is committed.
5. **Run tests before opening a PR.**

## Branch map

| Branch | Owner focus | Touch only |
|--------|-------------|------------|
| `develop` | Integration | merges only |
| `feature/auth-local-jwt` | Agent A | `backend/.../security`, `backend/.../controller` auth, `frontend/.../features/auth`, `frontend/.../core/auth` |
| `feature/auth-google-oauth` | Agent A (after local auth) | OAuth config, success handler, Google button + callback |
| `feature/courses-assignments` | Agent B | course/assignment model, repos, services, controllers, related frontend features |
| `feature/priority-engine` | Agent C | priority service, config weights, unit tests, prioritized endpoint |
| `feature/canvas-sync` | Agent (solo) | `integration/canvas`, sync endpoint |
| `feature/dashboard-ui` | Agent D | dashboard + work-queue UI consuming APIs |
| `feature/calendar-study-sessions` | Agent | calendar + study-sessions |
| `feature/docker-ci` | Agent | Dockerfiles, compose, GitHub Actions |
| `feature/aws-deploy` | Agent | `infra/`, deploy docs/workflows |

## Suggested parallel tracks (after backend skeleton exists)

```
          ┌─ Agent A: auth-local-jwt ──► auth-google-oauth
develop ──┼─ Agent B: courses-assignments
          ├─ Agent C: priority-engine (pure service + tests)
          └─ Agent D: Angular shell + mocks (until APIs merge)
```

Then serialize: merge auth → domain CRUD → priority → Canvas → rich UI → Docker/CI → AWS.

## How to start an agent in Cursor

Open a **new Agent chat** and paste a scoped prompt, for example:

```text
You are implementing feature/priority-engine only for Prioritize.
Branch from develop. Read docs/architecture.md and docs/api-contract.md.
Only modify backend priority scoring service, config, prioritized endpoint, and its unit tests.
Do not change auth, Canvas, or Angular.
```

## Merge order

1. Backend skeleton (Phase 1)
2. Local JWT auth + Angular auth pages
3. Google OAuth
4. Courses + Assignments CRUD
5. Priority engine
6. Canvas sync
7. Dashboard + work queue UI
8. Calendar + study sessions
9. Docker Compose
10. GitHub Actions
11. AWS
12. Power BI prep

## Conflict avoidance

- Freeze DTO field names in `api-contract.md` before parallel work
- Prefer additive PRs; avoid reformatting unrelated files
- If two features need the same entity, merge domain CRUD before UI-heavy branches
