-- ============================================================================
-- V2__remove_free_plan.sql
-- Removes the FREE plan and any subscriptions associated with it.
-- ============================================================================

-- 1. Remove subscriptions associated with the FREE plan
DELETE FROM subscriptions 
WHERE plan_id IN (SELECT id FROM plans WHERE name = 'FREE') 
   OR plan_id IS NULL; -- Also remove any hardcoded free tier subscriptions from SubscriptionService

-- 2. Remove the FREE plan from the plans table
DELETE FROM plans WHERE name = 'FREE';

-- 3. If any users have no subscription, they will be treated as having NONE 
-- (handled by updated SubscriptionService logic)
