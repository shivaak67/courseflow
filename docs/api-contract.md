# Prioritize API Contract (frozen for parallel agents)

Base URL (dev): `http://localhost:8080`

All `/api/**` routes except auth register/login require `Authorization: Bearer <JWT>` unless noted.

DTOs only — never expose JPA entities.

**Planning APIs** (categories, goals, projects, tasks, schedule-blocks, calendar-events, routines, reminders, notifications, time-entries, insights) are the product surface. There is no Canvas LMS sync and no Google Calendar integration.

---

## Auth

### POST `/api/auth/register`

Request:

```json
{
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "password": "string",
  "passwordConfirmation": "string"
}
```

Response `201`:

```json
{
  "accessToken": "string",
  "expiresIn": 86400000,
  "user": {
    "id": "uuid",
    "firstName": "string",
    "lastName": "string",
    "email": "string",
    "authProvider": "LOCAL",
    "role": "USER"
  }
}
```

### POST `/api/auth/login`

Request:

```json
{
  "email": "string",
  "password": "string"
}
```

Response `200`: same shape as register.

### POST `/api/auth/logout`

Response `204`. Client discards token (server denylist optional later).

### GET `/api/auth/me`

Response `200`: `user` object as above.

### Google OAuth

- Start: `GET /oauth2/authorization/google` (browser redirect)
- Callback: Spring handles `/login/oauth2/code/google`
- Success redirect: `{APP_OAUTH_SUCCESS_REDIRECT}?token=<JWT>` (e.g. `http://localhost:4200/auth/callback?token=...`)

---

## Planning APIs

Standard CRUD unless noted. All scoped to the authenticated user; missing/not-owned → `404`.

| Resource | Base path | Notes |
|----------|-----------|--------|
| Categories | `/api/categories` | `GET` list, `POST` create, `GET/PUT/DELETE /{id}` |
| Goals | `/api/goals` | Optional `categoryId`; status: `ACTIVE\|COMPLETED\|PAUSED\|ARCHIVED` |
| Projects | `/api/projects` | Optional `categoryId`, `goalId`; same status enum as goals |
| Tasks | `/api/tasks` | Optional `categoryId`, `projectId`; priority: `LOW\|MEDIUM\|HIGH\|URGENT` (manual); status: `TODO\|IN_PROGRESS\|COMPLETED\|CANCELLED` |
| Schedule blocks | `/api/schedule-blocks` | Links a `taskId` to `startAt`/`endAt`; optional date-range query on list |
| Calendar events | `/api/calendar-events` | Personal events (not tasks); optional date-range query on list |
| Routines | `/api/routines` | Recurrence: `DAILY\|WEEKLY\|SELECTED_WEEKDAYS\|MONTHLY`; `GET /occurrences?from=&to=` |
| Reminders | `/api/reminders` | Entity types: `TASK\|SCHEDULE_BLOCK\|ROUTINE\|CALENDAR_EVENT\|GOAL`; channel: `IN_APP` only; `POST /{id}/cancel` |
| Notifications | `/api/notifications` | In-app inbox; `POST /{id}/read`, `DELETE /{id}` |
| Notification settings | `/api/notification-settings` | `GET` / `PUT` (in-app / email flags) |
| Time entries | `/api/time-entries` | Logged work against a `taskId` |
| Insights | `/api/insights/summary` | Query: `from`, `to` — completion and minutes aggregates |

### Task (representative DTO)

```json
{
  "id": "uuid",
  "categoryId": "uuid|null",
  "projectId": "uuid|null",
  "title": "string",
  "description": "string|null",
  "dueDate": "date|null",
  "dueTime": "time|null",
  "estimatedMinutes": "number|null",
  "actualMinutes": 0,
  "priority": "LOW|MEDIUM|HIGH|URGENT",
  "status": "TODO|IN_PROGRESS|COMPLETED|CANCELLED",
  "completedAt": "iso-8601|null",
  "createdAt": "iso-8601",
  "updatedAt": "iso-8601"
}
```

### Insights summary (representative)

```json
{
  "from": "date",
  "to": "date",
  "tasksCreated": 0,
  "tasksCompleted": 0,
  "openTasks": 0,
  "totalMinutesLogged": 0,
  "estimatedMinutesOpen": 0,
  "completionRate": 0,
  "minutesByDay": [{ "date": "date", "minutes": 0 }],
  "topTasksByMinutes": [{ "taskId": "uuid", "title": "string", "minutes": 0 }]
}
```

---

## Dashboard

### GET `/api/dashboard/summary`

Planning-oriented summary for the home dashboard. Prefer `/api/insights/summary` for time and completion metrics.

---

## Error shape

```json
{
  "timestamp": "iso-8601",
  "status": 400,
  "error": "Bad Request",
  "message": "Human-readable, non-sensitive",
  "path": "/api/..."
}
```

Status usage: `201` create, `204` delete, `400` validation, `401` unauthenticated, `403` forbidden (rare; prefer `404` for not-owned), `404` missing/not-owned.
