-- V2__add_monthly_annual_prices.sql
-- Add monthly_price and annual_price columns and seed preferred plan pricing

ALTER TABLE plans
  ADD COLUMN monthly_price DOUBLE DEFAULT NULL,
  ADD COLUMN annual_price DOUBLE DEFAULT NULL;

-- Set prices for known plans
UPDATE plans SET monthly_price = 0.00, annual_price = 0.00 WHERE name = 'FREE';
UPDATE plans SET monthly_price = 10.00, annual_price = 100.00 WHERE name = 'BASIC';
UPDATE plans SET monthly_price = 25.00, annual_price = 250.00 WHERE name = 'PRO';

-- If PREMIUM exists, convert it to BUSINESS pricing (or create BUSINESS)
UPDATE plans SET monthly_price = 50.00, annual_price = 500.00 WHERE name = 'PREMIUM';

-- Insert BUSINESS plan if not exists
INSERT INTO plans (name, price, currency, duration, monthly_price, annual_price)
SELECT 'BUSINESS', 50.00, 'USD', 30, 50.00, 500.00
WHERE NOT EXISTS (SELECT 1 FROM plans WHERE name = 'BUSINESS');

-- Keep legacy `price` in sync with monthly_price when available
UPDATE plans SET price = monthly_price WHERE monthly_price IS NOT NULL;
