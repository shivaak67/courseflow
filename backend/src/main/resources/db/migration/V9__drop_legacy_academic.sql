-- Drop legacy academic / Canvas tables (Java entities already removed).
-- Order respects foreign keys: study_sessions → assignments → courses; canvas_connections standalone.

DROP TABLE IF EXISTS study_sessions;
DROP TABLE IF EXISTS canvas_connections;
DROP TABLE IF EXISTS assignments;
DROP TABLE IF EXISTS courses;
