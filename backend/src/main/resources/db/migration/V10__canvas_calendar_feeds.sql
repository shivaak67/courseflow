CREATE TABLE canvas_calendar_feeds (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    encrypted_url TEXT NOT NULL,
    host VARCHAR(255) NOT NULL,
    timezone VARCHAR(64) NOT NULL,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    last_synced_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(255),
    item_count INTEGER NOT NULL DEFAULT 0
);
ALTER TABLE calendar_events ADD COLUMN canvas_key VARCHAR(64);
ALTER TABLE calendar_events ADD COLUMN canvas_kind VARCHAR(20);
ALTER TABLE calendar_events ADD COLUMN canvas_url TEXT;
ALTER TABLE calendar_events ADD COLUMN canvas_start_date DATE;
ALTER TABLE calendar_events ADD COLUMN canvas_end_date DATE;
CREATE UNIQUE INDEX idx_calendar_canvas_uid ON calendar_events(user_id, canvas_key);
