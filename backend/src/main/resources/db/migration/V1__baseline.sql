-- Phase 1 baseline. Domain tables (users, courses, assignments, etc.) arrive in later feature migrations.
CREATE TABLE IF NOT EXISTS schema_baseline (
    id INT PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
