-- Extend users for planning platform (timezone + SMS readiness).
-- One column per ALTER for H2 compatibility.
ALTER TABLE users ADD COLUMN timezone VARCHAR(64) DEFAULT 'UTC' NOT NULL;
ALTER TABLE users ADD COLUMN phone_number VARCHAR(32);
ALTER TABLE users ADD COLUMN phone_verified BOOLEAN DEFAULT FALSE NOT NULL;

CREATE TABLE notification_settings (
    user_id UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    default_reminder_offsets_minutes VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
