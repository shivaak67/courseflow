-- Planning domain: Goals → Projects → Tasks → Schedule, plus routines/reminders/time.
-- Manual priority and scheduling only — no Canvas, no auto-ranking.

CREATE TABLE categories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    icon VARCHAR(64),
    color VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_categories_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_categories_user_id ON categories (user_id);

CREATE TABLE goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories (id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    target_date DATE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_goals_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED', 'ARCHIVED'))
);

CREATE INDEX idx_goals_user_id ON goals (user_id);
CREATE INDEX idx_goals_user_status ON goals (user_id, status);

CREATE TABLE projects (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories (id) ON DELETE SET NULL,
    goal_id UUID REFERENCES goals (id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_date DATE,
    target_date DATE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_projects_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'PAUSED', 'ARCHIVED'))
);

CREATE INDEX idx_projects_user_id ON projects (user_id);
CREATE INDEX idx_projects_goal_id ON projects (goal_id);
CREATE INDEX idx_projects_user_status ON projects (user_id, status);

CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories (id) ON DELETE SET NULL,
    project_id UUID REFERENCES projects (id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    due_date DATE,
    due_time TIME,
    estimated_minutes INT,
    actual_minutes INT NOT NULL DEFAULT 0,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT chk_tasks_status CHECK (status IN ('TODO', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_tasks_estimated_minutes CHECK (estimated_minutes IS NULL OR estimated_minutes >= 0),
    CONSTRAINT chk_tasks_actual_minutes CHECK (actual_minutes >= 0)
);

CREATE INDEX idx_tasks_user_id ON tasks (user_id);
CREATE INDEX idx_tasks_user_status ON tasks (user_id, status);
CREATE INDEX idx_tasks_user_due_date ON tasks (user_id, due_date);
CREATE INDEX idx_tasks_project_id ON tasks (project_id);

CREATE TABLE schedule_blocks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_schedule_blocks_range CHECK (end_at > start_at)
);

CREATE INDEX idx_schedule_blocks_user_start ON schedule_blocks (user_id, start_at);
CREATE INDEX idx_schedule_blocks_task_id ON schedule_blocks (task_id);

-- Personal calendar items that are not tasks (appointments, etc.).
CREATE TABLE calendar_events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories (id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    all_day BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_calendar_events_range CHECK (end_at >= start_at)
);

CREATE INDEX idx_calendar_events_user_start ON calendar_events (user_id, start_at);

CREATE TABLE routines (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories (id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    recurrence_type VARCHAR(20) NOT NULL,
    -- Comma-separated ISO weekdays 1-7 (Mon=1) for WEEKLY/SELECTED; NULL otherwise.
    days_of_week VARCHAR(32),
    interval_value INT NOT NULL DEFAULT 1,
    start_time TIME NOT NULL,
    end_time TIME,
    start_date DATE NOT NULL,
    end_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_routines_recurrence CHECK (recurrence_type IN ('DAILY', 'WEEKLY', 'SELECTED_WEEKDAYS', 'MONTHLY')),
    CONSTRAINT chk_routines_interval CHECK (interval_value >= 1),
    CONSTRAINT chk_routines_date_range CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_routines_user_id ON routines (user_id);

CREATE TABLE reminders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    related_entity_type VARCHAR(32) NOT NULL,
    related_entity_id UUID NOT NULL,
    reminder_at TIMESTAMP WITH TIME ZONE NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    attempt_count INT NOT NULL DEFAULT 0,
    failure_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reminders_entity_type CHECK (related_entity_type IN ('TASK', 'SCHEDULE_BLOCK', 'ROUTINE', 'CALENDAR_EVENT', 'GOAL')),
    CONSTRAINT chk_reminders_channel CHECK (channel IN ('IN_APP', 'SMS', 'EMAIL')),
    CONSTRAINT chk_reminders_status CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_reminders_attempts CHECK (attempt_count >= 0)
);

CREATE INDEX idx_reminders_due ON reminders (status, reminder_at);
CREATE INDEX idx_reminders_user_id ON reminders (user_id);
CREATE INDEX idx_reminders_related ON reminders (related_entity_type, related_entity_id);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    related_entity_type VARCHAR(32),
    related_entity_id UUID,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_notifications_entity_type CHECK (
        related_entity_type IS NULL
        OR related_entity_type IN ('TASK', 'SCHEDULE_BLOCK', 'ROUTINE', 'CALENDAR_EVENT', 'GOAL', 'REMINDER')
    )
);

CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at);
CREATE INDEX idx_notifications_user_id ON notifications (user_id);

CREATE TABLE time_entries (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES tasks (id) ON DELETE CASCADE,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    duration_minutes INT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_time_entries_duration CHECK (duration_minutes >= 0),
    CONSTRAINT chk_time_entries_range CHECK (
        started_at IS NULL
        OR ended_at IS NULL
        OR ended_at >= started_at
    )
);

CREATE INDEX idx_time_entries_user_id ON time_entries (user_id);
CREATE INDEX idx_time_entries_task_id ON time_entries (task_id);
