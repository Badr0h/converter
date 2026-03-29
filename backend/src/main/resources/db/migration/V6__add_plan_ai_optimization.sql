-- ============================================
-- Migration V6: Add AI & Cost Optimization Fields to Plans
-- Adds plan-based AI model selection, usage limits, and caching configuration
-- ============================================

-- Add new columns for AI model selection and usage limiting
ALTER TABLE plans ADD COLUMN ai_model VARCHAR(50) NOT NULL DEFAULT 'gpt-5-nano';
ALTER TABLE plans ADD COLUMN daily_limit INTEGER NULL;
ALTER TABLE plans ADD COLUMN monthly_limit INTEGER NULL;
ALTER TABLE plans ADD COLUMN prompt_template TEXT NULL;
ALTER TABLE plans ADD COLUMN enable_caching BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE plans ADD COLUMN cache_ttl_hours INTEGER NOT NULL DEFAULT 24;

-- Create indexes for better query performance
CREATE INDEX idx_plans_ai_model ON plans(ai_model);

-- Add comments for documentation
COMMENT ON COLUMN plans.ai_model IS 'AI model to use for this plan (e.g., gpt-5-nano, gpt-5-mini, gpt-5)';
COMMENT ON COLUMN plans.enable_caching IS 'Whether to cache conversion results for this plan';

-- Update existing plans with appropriate values
-- FREE Plan: Cheapest model, daily limits
UPDATE plans SET 
  ai_model = 'gpt-5-nano',
  daily_limit = 7,
  monthly_limit = NULL,
  enable_caching = TRUE,
  cache_ttl_hours = 24
WHERE name = 'FREE';

-- PRO Plan: Balanced model, monthly limits
UPDATE plans SET 
  ai_model = 'gpt-5-mini',
  daily_limit = NULL,
  monthly_limit = 500,
  enable_caching = TRUE,
  cache_ttl_hours = 24
WHERE name = 'PRO';

-- ENTERPRISE Plan: Best model, no limits (or very high)
UPDATE plans SET 
  ai_model = 'gpt-5',
  daily_limit = NULL,
  monthly_limit = 5000,
  enable_caching = TRUE,
  cache_ttl_hours = 168  -- 7 days for enterprise
WHERE name = 'ENTERPRISE';

-- For legacy plans, set sensible defaults
-- STARTER/PROFESSIONAL/BASIC plans
UPDATE plans SET 
  ai_model = CASE 
    WHEN name IN ('STARTER', 'BASIC') THEN 'gpt-5-nano'
    WHEN name IN ('PROFESSIONAL', 'STANDARD') THEN 'gpt-5-mini'
    ELSE 'gpt-5'
  END,
  enable_caching = TRUE,
  cache_ttl_hours = 24
WHERE ai_model IS NULL OR ai_model = '';
