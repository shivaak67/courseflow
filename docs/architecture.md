# Prioritize — Architecture Summary

Portfolio-quality academic productivity platform. This document is the Phase 0 reference for implementers and parallel agents.

## Application name

**Prioritize**

## Description

Syncs Canvas LMS courses and assignments, lets students refine effort/difficulty/priority, and ranks work with a transparent weighted priority engine. Dual auth (email/password + Google OAuth). Server-side data isolation per user.

## Monorepo layout

- `backend/` — Spring Boot (`com.prioritize`)
- `frontend/` — Angular SPA
- `docs/` — architecture, API contracts, agent workflow
- `infra/` — AWS notes (later)

## Backend packages

`config`, `security`, `controller`, `service`, `repository`, `model`, `dto`, `mapper`, `integration/canvas`, `exception`, `util`

## Frontend areas

`core` (auth, guards, interceptors, services), `shared`, `features` (auth, dashboard, courses, assignments, calendar, work-queue, study-sessions), `layout`, `environments`

## Core entities (planning pivot)

**Target model (V6–V7):**  
User → Category; User → Goal → Project → Task → ScheduleBlock;  
User → Routine | CalendarEvent | Reminder | Notification | TimeEntry;  
User → NotificationSettings  

**Legacy academic (still in DB until cleanup):** Course, Assignment, StudySession, CanvasConnection.

Ownership: all queries scoped by authenticated `userId`. Cross-user access returns 404.  
**No Canvas. No auto-priority / decision engine. Manual priority and scheduling only.**

## Auth (chosen)

- Local: BCrypt + JWT Bearer
- Google: Spring OAuth2 Login → issue same JWT → redirect to Angular `/auth/callback`
- `AuthProvider`: LOCAL | GOOGLE
- Link Google to existing LOCAL user when verified email matches

## Priority formula

```
priorityScore = wU*urgency + wP*pointValue + wD*difficulty + wW*workload + wR*personalPriority
```

Defaults: 0.35 / 0.20 / 0.15 / 0.15 / 0.15. Levels: CRITICAL ≥ 80, HIGH ≥ 60, MEDIUM ≥ 40, else LOW.

## Canvas

`CanvasClient` interface + HTTP impl + mock. Upsert by Canvas IDs. Env token in MVP; OAuth later behind the same interface.

## Phases

0 Scaffold → 1 Backend skeleton → 2 Local JWT + Angular auth → 3 Google OAuth → 4 Courses/Assignments → 5 Priority engine → 6 Canvas sync → 7 Dashboard/work queue UI → 8 Calendar/study sessions → 9 Docker → 10 CI → 11 AWS → 12 Power BI

## Branching

- `main` — protected, PR-only
- `develop` — integration
- Feature branches: `feature/<area>` (one concern per branch)

See [api-contract.md](api-contract.md) and [multi-agent-workflow.md](multi-agent-workflow.md).
