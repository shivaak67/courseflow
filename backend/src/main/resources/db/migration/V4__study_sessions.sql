-- Study sessions logged against owned assignments.
CREATE TABLE study_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    assignment_id UUID NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    duration_minutes INT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_study_sessions_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_study_sessions_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE,
    CONSTRAINT chk_study_sessions_duration_minutes
        CHECK (duration_minutes > 0)
);

CREATE INDEX idx_study_sessions_user_id ON study_sessions (user_id);
CREATE INDEX idx_study_sessions_assignment_id ON study_sessions (assignment_id);
