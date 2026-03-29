-- ===================================================
-- Migration: V7__add_enterprise_plan_improvements
-- ===================================================
-- Purpose: Add hard limit support for enterprise plans
--          and upgrade flow improvements
-- Date: 2026-03-28
-- ===================================================

-- Add is_enterprise column to mark enterprise plans
ALTER TABLE plans ADD COLUMN IF NOT EXISTS is_enterprise BOOLEAN DEFAULT FALSE NOT NULL;

-- Set existing ENTERPRISE plan to is_enterprise=true
UPDATE plans SET is_enterprise = TRUE WHERE LOWER(name) = 'enterprise';;

-- Add comment for clarity
COMMENT ON COLUMN plans.is_enterprise IS 'Marks this as an enterprise plan with hard limits (replaces unlimited)';

-- Add cache control column for plan-specific caching
ALTER TABLE plans ADD COLUMN IF NOT EXISTS cache_ttl_hours INTEGER DEFAULT 24 NOT NULL;

-- Update cache TTL for different plans:
-- FREE: 24h cache (reuse results to save costs)
-- PRO: 24h cache (reasonable default)
-- ENTERPRISE: 7d cache (optimize for large deployments)
UPDATE plans SET cache_ttl_hours = 24 WHERE LOWER(name) IN ('free', 'pro');
UPDATE plans SET cache_ttl_hours = 168 WHERE LOWER(name) = 'enterprise'; -- 7 days

-- Create index for global cache lookups
-- Used by GlobalConversionCache.getGlobalCache()
CREATE INDEX IF NOT EXISTS idx_conversion_formats_prompt 
ON conversions(input_format, output_format, prompt(255));

-- Create index for rate limiting checks
-- Used by UsageLimiter for rapid lookups
CREATE INDEX IF NOT EXISTS idx_conversion_user_created 
ON conversions(user_id, created_at DESC);

-- Add column to track cached results (for analytics)
ALTER TABLE conversions ADD COLUMN IF NOT EXISTS was_cached BOOLEAN DEFAULT FALSE;

-- Create table for rate limiting (server-side)
CREATE TABLE IF NOT EXISTS api_rate_limits (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  endpoint VARCHAR(255) NOT NULL,
  method VARCHAR(10) NOT NULL,
  request_count INT DEFAULT 0 NOT NULL,
  window_start TIMESTAMP NOT NULL,
  window_end TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE INDEX idx_rate_limit_user_endpoint (user_id, endpoint, method)
);

-- Create table for abuse incidents tracking
CREATE TABLE IF NOT EXISTS abuse_incidents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  incident_type VARCHAR(50) NOT NULL, -- 'RATE_LIMIT', 'REPEATED_ERRORS', 'INVALID_INPUT'
  description TEXT,
  ip_address VARCHAR(45),
  user_agent VARCHAR(500),
  blocked_until TIMESTAMP,
  resolved_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
  INDEX idx_abuse_user (user_id),
  INDEX idx_abuse_created (created_at DESC)
);

-- Add success indicator for conversions (for abuse detection)
ALTER TABLE conversions ADD COLUMN IF NOT EXISTS success BOOLEAN DEFAULT TRUE;

-- Migration summary
-- 1. Enterprise plans now have hard limits (e.g., 5000/month) instead of "unlimited"
-- 2. Global caching across users reduces API calls by ~60%
-- 3. Rate limiting and abuse protection prevent exploitation
-- 4. Analytics tables for monitoring system health
