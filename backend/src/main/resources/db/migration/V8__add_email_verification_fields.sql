-- Add email verification fields to users table
ALTER TABLE users 
ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN verification_code VARCHAR(6),
ADD COLUMN verification_code_expiry TIMESTAMP;
