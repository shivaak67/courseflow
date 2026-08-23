# Prioritize

Academic productivity platform for college students. Syncs courses and assignments from Canvas LMS, ranks work with a transparent priority engine, and helps students decide what to work on first.

## Overview

Prioritize connects to Canvas, imports courses and assignments, and combines deadlines, point value, difficulty, estimated effort, and personal priority into a clear work queue. Students get a dashboard, calendar, and “What Should I Work On?” view. Power BI is used separately for historical analytics.

## Features

- Email/password and Google OAuth authentication (JWT)
- Canvas course and assignment sync
- Manual course and assignment management
- Smart priority scoring (configurable weights)
- Dashboard, calendar, and recommended work queue
- Study session / actual hours tracking
- Power BI–ready PostgreSQL schema

*(Sections expand as each development phase lands.)*

## Architecture

Monorepo with a Spring Boot API, Angular SPA, and PostgreSQL.

- Backend enforces ownership and authorization on every resource
- Frontend consumes REST APIs via Angular services
- Canvas integration is behind a client interface (mockable for tests)
- Analytics (Power BI) reads the database; it is not the main UI

See [docs/architecture.md](docs/architecture.md) for schema, auth flow, and phase plan.

## Tech Stack

| Layer | Technologies |
|-------|----------------|
| Frontend | Angular, TypeScript, Angular Material |
| Backend | Java, Spring Boot, Spring Security, Spring Data JPA |
| Database | PostgreSQL |
| Auth | JWT + Google OAuth 2.0 / OIDC |
| Integration | Canvas LMS REST API |
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
└── docker-compose.yml  # Local stack (later phase)
```

## Getting Started

### Prerequisites

- JDK 21+
- Node.js 20+ and npm
- PostgreSQL 16+ (or Docker)
- Google Cloud OAuth client (for Google sign-in)
- Canvas API token (for sync in development)

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

Course and assignment APIs require `Authorization: Bearer <JWT>` from `/api/auth/login` or `/api/auth/register`. Cross-user access returns `404`.

## Authentication

- **Local:** register / login with email and password (BCrypt hashes)
- **Google:** “Continue with Google” via Spring Security OAuth2 Login; app issues the same JWT

Secrets: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET` — never commit real values.

## Canvas Integration

Uses a `CanvasClient` abstraction. Development may use an API token from the environment; Canvas OAuth can replace tokens later without redesigning sync.

## Priority Engine

Weighted, transparent formula (urgency, points, difficulty, workload, personal priority). Assignments are labeled LOW / MEDIUM / HIGH / CRITICAL. Configurable via application settings.

## API Overview

REST under `/api/*`. Auth: `/api/auth/*` and Google OAuth endpoints. See [docs/api-contract.md](docs/api-contract.md).

## Testing

- Backend: JUnit, Mockito, Spring Boot integration tests
- Frontend: Angular unit tests (added with the app scaffold)
- Priority engine and ownership rules are high-priority test targets

## Docker

Dockerfiles and `docker-compose.yml` for PostgreSQL, backend, and frontend will be added in a later phase.

## CI/CD

GitHub Actions will build and test backend and frontend on pull requests. Deployment workflows for AWS come after the MVP.

## Deployment (AWS)

- Frontend: S3 + CloudFront
- Backend: EC2 (or equivalent compute)
- Database: Amazon RDS (PostgreSQL)
- Secrets: environment variables / AWS Secrets Manager

## Analytics (Power BI)

Power BI connects to PostgreSQL for completion rates, workload by course, estimated vs actual hours, and trends. Angular remains the interactive app UI.

## Roadmap

See [docs/architecture.md](docs/architecture.md) for MVP vs later and development phases.

## Multi-agent development

See [docs/multi-agent-workflow.md](docs/multi-agent-workflow.md).

## License

TBD
