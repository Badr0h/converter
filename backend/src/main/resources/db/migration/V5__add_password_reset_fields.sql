-- ============================================
-- Migration V5: Add Password Reset Fields
-- Adds support for forgot password functionality
-- ============================================

-- Add reset token and expiry columns to users table
ALTER TABLE users ADD COLUMN reset_token VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN reset_token_expiry TIMESTAMP NULL;

-- Create indexes for performance
CREATE INDEX idx_users_reset_token ON users(reset_token);
CREATE INDEX idx_users_reset_token_expiry ON users(reset_token_expiry);

COMMENT ON COLUMN users.reset_token IS 'Token for password reset requests';
COMMENT ON COLUMN users.reset_token_expiry IS 'Expiration time for password reset token';
