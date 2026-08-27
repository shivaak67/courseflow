# Prioritize — Architecture Summary

Portfolio-quality personal productivity and planning platform. This document is the reference for implementers and parallel agents.

## Application name

**Prioritize**

## Description

Organize work as Goals → Projects → Tasks with optional categories. Students (and anyone planning work) set manual priorities, schedule blocks, calendar events, and routines. Reminders, notifications, time entries, and insights support follow-through. Dual auth (email/password + Google OAuth). Server-side data isolation per user.

**Product stance:** No Canvas LMS integration. No Google Calendar sync. No auto-priority / decision engine—manual priority and scheduling only.

## Monorepo layout

- `backend/` — Spring Boot (`com.prioritize`)
- `frontend/` — Angular SPA
- `docs/` — architecture, API contracts, agent workflow
- `infra/` — AWS notes (later)

## Backend packages

`config`, `security`, `controller`, `service`, `repository`, `model`, `dto`, `mapper`, `exception`, `util`

## Frontend areas

`core` (auth, guards, interceptors, services), `shared`, `features` (auth, dashboard, goals, projects, tasks, categories, schedule, calendar, routines, notifications, settings, insights, time tracking), `layout`, `environments`

## Core entities (planning pivot)

**Model:**  
User → Category; User → Goal → Project → Task → ScheduleBlock;  
User → Routine | CalendarEvent | Reminder | Notification | TimeEntry;  
User → NotificationSettings  

Ownership: all queries scoped by authenticated `userId`. Cross-user access returns 404.  
**No Canvas. No Google Calendar sync. No auto-priority / decision engine. Manual priority and scheduling only.**

## Auth (chosen)

- Local: BCrypt + JWT Bearer
- Google: Spring OAuth2 Login → issue same JWT → redirect to Angular `/auth/callback`
- `AuthProvider`: LOCAL | GOOGLE
- Link Google to existing LOCAL user when verified email matches

## Phases

0 Scaffold → 1 Backend skeleton → 2 Local JWT + Angular auth → 3 Google OAuth → 4 Planning core (categories/goals/projects/tasks) → 5 Schedule / calendar / routines → 6 Reminders & notifications → 7 Time entries & insights → 8 Dashboard UI polish → 9 Docker → 10 CI → 11 AWS → 12 Power BI

## Branching

- `main` — protected, PR-only
- `develop` — integration
- Feature branches: `feature/<area>` (one concern per branch)

See [api-contract.md](api-contract.md) and [multi-agent-workflow.md](multi-agent-workflow.md).
