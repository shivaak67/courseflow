-- Courses and assignments (after users table from V2).
CREATE TABLE courses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    canvas_course_id VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    course_code VARCHAR(100),
    term VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_courses_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_courses_user_canvas_id UNIQUE (user_id, canvas_course_id)
);

CREATE INDEX idx_courses_user_id ON courses (user_id);

CREATE TABLE assignments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    canvas_assignment_id VARCHAR(255),
    title VARCHAR(500) NOT NULL,
    description TEXT,
    due_date TIMESTAMP WITH TIME ZONE,
    points_possible DOUBLE PRECISION,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    submitted BOOLEAN NOT NULL DEFAULT FALSE,
    difficulty VARCHAR(20),
    estimated_hours DOUBLE PRECISION,
    actual_hours DOUBLE PRECISION NOT NULL DEFAULT 0,
    personal_priority INTEGER,
    priority_score DOUBLE PRECISION,
    priority_level VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assignments_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT uq_assignments_user_canvas_id UNIQUE (user_id, canvas_assignment_id),
    CONSTRAINT chk_assignments_difficulty
        CHECK (difficulty IS NULL OR difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_assignments_priority_level
        CHECK (priority_level IS NULL OR priority_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_assignments_user_id ON assignments (user_id);
CREATE INDEX idx_assignments_course_id ON assignments (course_id);
CREATE INDEX idx_assignments_due_date ON assignments (due_date);
