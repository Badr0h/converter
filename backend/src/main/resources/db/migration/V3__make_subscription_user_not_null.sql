-- V3__make_subscription_user_not_null.sql
-- Make user_id NOT NULL in subscriptions table to enforce user association

-- First, assign any subscriptions without a user to the first available user (if they exist)
UPDATE subscriptions 
SET user_id = (SELECT id FROM users LIMIT 1) 
WHERE user_id IS NULL;

-- Then modify the column to NOT NULL
ALTER TABLE subscriptions 
MODIFY COLUMN user_id BIGINT NOT NULL;
