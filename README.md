# Prioritize

Personal productivity and planning platform. Organize work as Goals → Projects → Tasks, block time on a schedule, manage calendar events and routines, and track progress with reminders, insights, and time entries.

## Overview

Prioritize helps you plan and execute work with a clear hierarchy: categories and goals break into projects and tasks. You set manual priorities and schedule blocks yourself—there is no Canvas LMS sync, no Google Calendar sync, and no automatic priority / decision engine. The app covers schedule, calendar, routines, reminders, notifications, time tracking, and insights. Power BI can connect separately for historical analytics.

## Features

- Email/password and Google OAuth authentication (JWT)
- Categories, goals, projects, and tasks (manual priority)
- Schedule blocks (time-blocking) and personal calendar events
- Recurring routines and occurrences
- Reminders and in-app notifications
- Time entries and insights summary
- Dashboard overview
- Power BI–ready PostgreSQL schema

*(Sections expand as each development phase lands.)*

## Architecture

Monorepo with a Spring Boot API, Angular SPA, and PostgreSQL.

- Backend enforces ownership and authorization on every resource
- Frontend consumes REST APIs via Angular services
- Planning model: Category / Goal / Project / Task plus schedule, calendar, routines, reminders, time tracking
- Analytics (Power BI) reads the database; it is not the main UI

See [docs/architecture.md](docs/architecture.md) for schema, auth flow, and phase plan.

## Tech Stack

| Layer | Technologies |
|-------|----------------|
| Frontend | Angular, TypeScript, Angular Material |
| Backend | Java, Spring Boot, Spring Security, Spring Data JPA |
| Database | PostgreSQL |
| Auth | JWT + Google OAuth 2.0 / OIDC |
| Analytics | Power BI |
| DevOps | Docker, Docker Compose, GitHub Actions |
| Cloud | AWS (S3 + CloudFront, compute, RDS) |

## Project Structure

```
├── backend/          # Spring Boot API
├── frontend/         # Angular SPA
├── docs/             # Architecture, API contracts, agent workflow
├── infra/            # AWS / deployment notes (later)
├── .github/          # CI/CD workflows (later)
├── .env.example      # Environment variable template
└── docker-compose.yml  # Local full stack (Postgres + API + SPA)
```

## Getting Started

### Prerequisites

- JDK 21+
- Node.js 20+ and npm
- PostgreSQL 16+ (or Docker)
- Google Cloud OAuth client (for Google sign-in)

### Setup

1. Clone the repository
2. Copy `.env.example` to `.env` and fill in values
3. Start PostgreSQL and create the `prioritize` database
4. Backend:
   - Requires JDK 21+
   - From `backend/`: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`)
   - Health check: `GET http://localhost:8080/actuator/health`
   - Tests: `./mvnw test`
5. Frontend:
   - Requires Node.js 20+ and npm
   - From `frontend/`: `npm install` then `npm start` → http://localhost:4200
   - API base URL: `http://localhost:8080` (see `src/environments/`)
   - Production build: `npm run build`

Planning APIs (goals, projects, tasks, etc.) require `Authorization: Bearer <JWT>` from `/api/auth/login` or `/api/auth/register`. Cross-user access returns `404`.

## Authentication

- **Local:** register / login with email and password (BCrypt hashes)
- **Google:** “Continue with Google” via Spring Security OAuth2 Login; app issues the same JWT

### Google OAuth setup (local)

1. In [Google Cloud Console](https://console.cloud.google.com/), create an OAuth 2.0 Client ID (Web application)
2. Add authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
3. Copy client ID/secret into `.env`:
   - `GOOGLE_OAUTH_ENABLED=true`
   - `GOOGLE_CLIENT_ID=...`
   - `GOOGLE_CLIENT_SECRET=...`
   - `APP_OAUTH_SUCCESS_REDIRECT=http://localhost:4200/auth/callback`
4. Restart the backend, then use **Continue with Google** on the login/register pages

Secrets: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET` — never commit real values.

## Planning model

Work is organized as **Goals → Projects → Tasks**, with optional categories. Priority on tasks is set manually (`LOW` / `MEDIUM` / `HIGH` / `URGENT`). Schedule blocks attach tasks to calendar time; routines, reminders, time entries, and insights support day-to-day planning—not an automated ranking engine.

## API Overview

REST under `/api/*`. Auth: `/api/auth/*` and Google OAuth endpoints. See [docs/api-contract.md](docs/api-contract.md).

## Testing

- Backend: JUnit, Mockito, Spring Boot integration tests
- Frontend: Angular unit tests (added with the app scaffold)
- Ownership rules and planning CRUD are high-priority test targets

## Docker

Run the full local stack (PostgreSQL, Spring Boot API, Angular SPA via nginx) with Docker Compose.

1. Copy `.env.example` to `.env` and fill in secrets (`JWT_SECRET`, optional Google OAuth values).
2. From the repo root:

```bash
docker compose up --build
```

3. Open the app at [http://localhost:4200](http://localhost:4200). API: [http://localhost:8080](http://localhost:8080) (health: `/actuator/health`).

**How ports map**

| Service  | Host port | Notes |
|----------|-----------|--------|
| frontend | 4200 → 80 | nginx serves the SPA with client-side routing fallback |
| backend  | 8080 → 8080 | Browser calls `http://localhost:8080` for API and OAuth (matches Angular `apiBaseUrl`) |
| db       | 5432 → 5432 | Compose Postgres; backend uses hostname `db` inside the network |

**Google OAuth:** keep the authorized redirect URI as `http://localhost:8080/login/oauth2/code/google` (same as non-Docker local). Success redirect stays `http://localhost:4200/auth/callback`.

**Port 5432 conflict:** if you already run Postgres on the host (or another container named similarly), stop it or change the compose port mapping. Compose brings up its own `db` service and volume (`prioritize_pgdata`); it does not use a host-installed `prioritize-postgres` instance.

Stop with `Ctrl+C` or `docker compose down`. Add `-v` to also remove the database volume.

## CI/CD

GitHub Actions builds and tests the backend (`./mvnw -B test`) and frontend (`npm ci` / `npm run build`) on pull requests and pushes to `main` and `develop`. Deployment workflows for AWS come after the MVP.

## Deployment (AWS)

- Frontend: S3 + CloudFront
- Backend: EC2 (or equivalent compute)
- Database: Amazon RDS (PostgreSQL)
- Secrets: environment variables / AWS Secrets Manager

## Analytics (Power BI)

Power BI connects to PostgreSQL for completion rates, workload trends, and estimated vs actual time. Angular remains the interactive app UI.

## Roadmap

See [docs/architecture.md](docs/architecture.md) for MVP vs later and development phases.

## Multi-agent development

See [docs/multi-agent-workflow.md](docs/multi-agent-workflow.md).

## License

TBD
