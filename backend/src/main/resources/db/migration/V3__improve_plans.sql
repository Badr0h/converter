-- ============================================================================
-- V3__improve_plans.sql
-- Refines plan names, descriptions, pricing and limits.
-- Removes redundant PREMIUM plan.
-- ============================================================================

-- 1. Remove PREMIUM plan and its subscriptions
DELETE FROM subscriptions WHERE plan_id IN (SELECT id FROM plans WHERE name = 'PREMIUM');
DELETE FROM plans WHERE name = 'PREMIUM';

-- 2. Update BASIC plan
UPDATE plans 
SET name = 'STARTER',
    description = 'Ideal for individuals and side projects. Comprehensive AI conversion tools to accelerate your workflow.',
    price = 15.00,
    monthly_price = 15.00,
    annual_price = 150.00,
    max_conversions = 300
WHERE name = 'BASIC';

-- 3. Update PRO plan
UPDATE plans 
SET name = 'PROFESSIONAL',
    description = 'Unleash the full potential of AI with higher limits, priority processing, and advanced features for power users.',
    price = 39.00,
    monthly_price = 39.00,
    annual_price = 390.00,
    max_conversions = 1500
WHERE name = 'PRO';

-- 4. Update BUSINESS plan
UPDATE plans 
SET name = 'ENTERPRISE',
    description = 'The ultimate solution for teams and large-scale operations. Unlimited power with dedicated support and API access.',
    price = 89.00,
    monthly_price = 89.00,
    annual_price = 890.00,
    max_conversions = 1000000
WHERE name = 'BUSINESS';
