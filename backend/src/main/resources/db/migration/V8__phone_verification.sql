-- Phone verification codes for SMS channel.
ALTER TABLE users ADD COLUMN phone_verification_code VARCHAR(16);
ALTER TABLE users ADD COLUMN phone_verification_expires_at TIMESTAMP WITH TIME ZONE;
