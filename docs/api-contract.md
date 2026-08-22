# Prioritize API Contract (frozen for parallel agents)

Base URL (dev): `http://localhost:8080`

All `/api/**` routes except auth register/login require `Authorization: Bearer <JWT>` unless noted.

DTOs only — never expose JPA entities.

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

## Courses

### GET `/api/courses`

Response `200`: `CourseResponse[]`

```json
{
  "id": "uuid",
  "canvasCourseId": "string|null",
  "name": "string",
  "courseCode": "string|null",
  "term": "string|null",
  "createdAt": "iso-8601",
  "updatedAt": "iso-8601"
}
```

### POST `/api/courses`

Request: `{ "name", "courseCode?", "term?" }` → `201` `CourseResponse`

### GET `/api/courses/{id}` → `200` | `404`

### PUT `/api/courses/{id}` → `200` | `404`

### DELETE `/api/courses/{id}` → `204` | `404`

---

## Assignments

### AssignmentResponse

```json
{
  "id": "uuid",
  "courseId": "uuid",
  "courseName": "string",
  "canvasAssignmentId": "string|null",
  "title": "string",
  "description": "string|null",
  "dueDate": "iso-8601|null",
  "pointsPossible": "number|null",
  "completed": false,
  "submitted": false,
  "difficulty": "EASY|MEDIUM|HARD|null",
  "estimatedHours": "number|null",
  "actualHours": "number",
  "personalPriority": "number|null",
  "priorityScore": "number|null",
  "priorityLevel": "LOW|MEDIUM|HIGH|CRITICAL|null",
  "createdAt": "iso-8601",
  "updatedAt": "iso-8601"
}
```

### GET `/api/assignments` — query: `courseId?`, `completed?`

### POST `/api/assignments` — create (manual)

### GET `/api/assignments/{id}`

### PUT `/api/assignments/{id}` — editable: estimatedHours, difficulty, personalPriority, completed, actualHours, title/description when manual

### GET `/api/assignments/upcoming`

### GET `/api/assignments/overdue`

### GET `/api/assignments/prioritized`

Response item extends AssignmentResponse:

```json
{
  "reasons": ["Due in 2 days", "Worth high point value", "Marked HARD"]
}
```

Ordered by `priorityScore` descending. Excludes completed/submitted by default.

---

## Dashboard

### GET `/api/dashboard/summary`

```json
{
  "dueTodayCount": 0,
  "dueThisWeekCount": 0,
  "overdueCount": 0,
  "highPriorityCount": 0,
  "completedCount": 0,
  "remainingCount": 0,
  "estimatedHoursRemainingThisWeek": 0,
  "workloadByCourse": [
    { "courseId": "uuid", "courseName": "string", "assignmentCount": 0, "estimatedHours": 0 }
  ]
}
```

---

## Canvas

### POST `/api/canvas/sync`

Response `200`:

```json
{
  "coursesUpserted": 0,
  "assignmentsUpserted": 0,
  "lastSyncedAt": "iso-8601"
}
```

---

## Study sessions

### POST `/api/study-sessions`

Request:

```json
{
  "assignmentId": "uuid",
  "startedAt": "iso-8601|null",
  "endedAt": "iso-8601|null",
  "durationMinutes": 60,
  "notes": "string|null"
}
```

### GET `/api/study-sessions`

Response: session list for current user.

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
