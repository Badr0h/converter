-- V4__add_max_conversions.sql
-- Add max conversions per plan and subscription

-- Add max_conversions column to plans table
ALTER TABLE plans 
ADD COLUMN max_conversions INT DEFAULT 100;

-- Add max_conversions_per_month column to subscriptions table
ALTER TABLE subscriptions 
ADD COLUMN max_conversions_per_month INT DEFAULT 1;

-- Make plan_id nullable in subscriptions for free tier
ALTER TABLE subscriptions 
MODIFY COLUMN plan_id BIGINT NULL;

-- Update comment for clarity
ALTER TABLE plans 
MODIFY COLUMN max_conversions INT COMMENT 'Maximum conversions allowed per month for this plan';

ALTER TABLE subscriptions 
MODIFY COLUMN max_conversions_per_month INT COMMENT 'Maximum conversions allowed per month based on subscription plan';
