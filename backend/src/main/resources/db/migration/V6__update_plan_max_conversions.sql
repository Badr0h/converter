-- V6__update_plan_max_conversions

UPDATE plans
SET max_conversions = 200
WHERE name = 'BASIC';

UPDATE plans
SET max_conversions = 500
WHERE name = 'PRO';

UPDATE plans
SET max_conversions = 1000
WHERE name = 'PREMIUM';

UPDATE plans
SET max_conversions = 9999
WHERE name = 'BUSINESS';
