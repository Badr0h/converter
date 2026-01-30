-- ============================================================================
-- V1__init.sql
-- Complete consolidated schema for mathematical formula conversion system
-- Optimized for PostgreSQL - Production Ready
-- ============================================================================

-- ============================================
-- Table: users
-- Stores user account information
-- ============================================
CREATE TABLE users (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    verification_code VARCHAR(6),
    verification_code_expiry TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index on email for faster lookups
CREATE INDEX idx_users_email ON users(email);

COMMENT ON TABLE users IS 'User account information with email verification support';

-- ============================================
-- Table: plans
-- Defines available subscription plans
-- ============================================
CREATE TABLE plans (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    duration INTEGER NOT NULL,
    monthly_price NUMERIC(10, 2),
    annual_price NUMERIC(10, 2),
    max_conversions INTEGER DEFAULT 100,
    CONSTRAINT chk_price_positive CHECK (price >= 0),
    CONSTRAINT chk_monthly_price_positive CHECK (monthly_price IS NULL OR monthly_price >= 0),
    CONSTRAINT chk_annual_price_positive CHECK (annual_price IS NULL OR annual_price >= 0),
    CONSTRAINT chk_duration_positive CHECK (duration > 0),
    CONSTRAINT chk_max_conversions_positive CHECK (max_conversions > 0)
);

COMMENT ON COLUMN plans.max_conversions IS 'Maximum conversions allowed per month for this plan';

-- ============================================
-- Table: conversions
-- Tracks history of mathematical formula conversions
-- ============================================
CREATE TABLE conversions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NULL,
    input_format VARCHAR(20) NOT NULL DEFAULT 'TEXT' 
        CHECK (input_format IN ('TEXT', 'LATEX', 'MATHML', 'UNICODE', 'PYTHON', 'NUMPY', 'SYMPY', 'SCIPY', 'JAVASCRIPT', 'MATLAB', 'R')),
    output_format VARCHAR(20) NOT NULL DEFAULT 'PYTHON'
        CHECK (output_format IN ('TEXT', 'LATEX', 'MATHML', 'UNICODE', 'PYTHON', 'NUMPY', 'SYMPY', 'SCIPY', 'JAVASCRIPT', 'MATLAB', 'R')),
    prompt TEXT NOT NULL,
    ai_response TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_conversion_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE conversions IS 'Supports bidirectional format conversion - all formats can be used as both input and output';

-- Conversions indexes for performance
CREATE INDEX idx_conversions_user_created ON conversions(user_id, created_at DESC);
CREATE INDEX idx_conversions_user ON conversions(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_conversions_created_at ON conversions(created_at DESC);

-- ============================================
-- Table: subscriptions
-- Manages user subscriptions
-- ============================================
CREATE TABLE subscriptions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED', 'PENDING')),
    duration VARCHAR(20) NOT NULL 
        CHECK (duration IN ('ONE_MONTH', 'THREE_MONTHS', 'TWELVE_MONTHS')),
    start_date DATE NOT NULL,
    end_date DATE,
    max_conversions_per_month INTEGER NOT NULL,
    is_trial BOOLEAN DEFAULT FALSE,
    trial_end_date DATE,
    auto_renew BOOLEAN DEFAULT TRUE,
    cancelled_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subscription_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) 
        REFERENCES plans(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_end_date_after_start CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT chk_trial_end_after_start CHECK (trial_end_date IS NULL OR trial_end_date >= start_date)
);

COMMENT ON COLUMN subscriptions.max_conversions_per_month IS 'Maximum conversions allowed per month based on subscription plan';
COMMENT ON COLUMN subscriptions.is_trial IS 'Indicates if this is a trial subscription';
COMMENT ON COLUMN subscriptions.auto_renew IS 'Indicates if subscription should auto-renew';

-- Subscriptions indexes
CREATE INDEX idx_subscriptions_user_status ON subscriptions(user_id, status);
CREATE INDEX idx_subscriptions_plan ON subscriptions(plan_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_end_date ON subscriptions(end_date) WHERE end_date IS NOT NULL;
CREATE INDEX idx_subscriptions_active ON subscriptions(user_id) WHERE status = 'ACTIVE';

-- ============================================
-- Table: payments
-- Tracks payment transactions
-- ============================================
CREATE TABLE payments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED', 'PARTIALLY_REFUNDED', 'CANCELLED')),
    payment_method VARCHAR(50),
    transaction_id VARCHAR(100) UNIQUE,
    payment_gateway VARCHAR(50),
    billing_cycle VARCHAR(20),
    description VARCHAR(500),
    failure_reason VARCHAR(500),
    refund_amount NUMERIC(10, 2),
    refund_date TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_payment_subscription FOREIGN KEY (subscription_id) 
        REFERENCES subscriptions(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_payment_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_refund_amount_positive CHECK (refund_amount IS NULL OR refund_amount >= 0),
    CONSTRAINT chk_refund_amount_not_exceed_payment CHECK (refund_amount IS NULL OR refund_amount <= amount)
);

COMMENT ON TABLE payments IS 'Payment transaction records with support for refunds and multiple gateways';

-- Payments indexes for performance
CREATE INDEX idx_payments_user_status ON payments(user_id, status);
CREATE INDEX idx_payments_user ON payments(user_id);
CREATE INDEX idx_payments_subscription ON payments(subscription_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id) WHERE transaction_id IS NOT NULL;
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);
CREATE INDEX idx_payments_gateway ON payments(payment_gateway) WHERE payment_gateway IS NOT NULL;
CREATE INDEX idx_payments_updated_at ON payments(updated_at) WHERE updated_at IS NOT NULL;

-- ============================================
-- Table: processed_webhooks
-- Tracks processed webhook events for idempotency
-- ============================================
CREATE TABLE processed_webhooks (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    webhook_event_id VARCHAR(100) NOT NULL UNIQUE,
    order_id VARCHAR(100),
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSED' 
        CHECK (status IN ('PROCESSED', 'FAILED', 'DUPLICATE')),
    error_message VARCHAR(500),
    payload JSONB,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE processed_webhooks IS 'Ensures webhook idempotency by tracking processed events';
COMMENT ON COLUMN processed_webhooks.webhook_event_id IS 'Unique identifier from webhook provider';
COMMENT ON COLUMN processed_webhooks.payload IS 'Complete webhook payload stored as JSONB for analysis';

-- Webhook indexes for fast lookups
CREATE INDEX idx_webhook_event_id ON processed_webhooks(webhook_event_id);
CREATE INDEX idx_webhook_order_id ON processed_webhooks(order_id) WHERE order_id IS NOT NULL;
CREATE INDEX idx_webhook_processed_at ON processed_webhooks(processed_at DESC);
CREATE INDEX idx_webhook_status ON processed_webhooks(status);

-- ============================================
-- Table: monthly_conversion_usage
-- Tracks monthly conversion usage per user
-- ============================================
CREATE TABLE monthly_conversion_usage (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    year_month CHAR(7) NOT NULL,
    conversion_count INTEGER NOT NULL DEFAULT 0,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usage_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_month UNIQUE (user_id, year_month),
    CONSTRAINT chk_year_month_format CHECK (year_month ~ '^\d{4}-\d{2}$'),
    CONSTRAINT chk_conversion_count_positive CHECK (conversion_count >= 0)
);

COMMENT ON TABLE monthly_conversion_usage IS 'Tracks conversion usage per user per month for quota management';
COMMENT ON COLUMN monthly_conversion_usage.year_month IS 'Format: YYYY-MM (e.g., 2026-01)';

-- Index for fast usage lookups
CREATE INDEX idx_usage_user_month ON monthly_conversion_usage(user_id, year_month DESC);
CREATE INDEX idx_usage_year_month ON monthly_conversion_usage(year_month);

-- ============================================
-- Triggers and Functions
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger to users table
CREATE TRIGGER set_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to payments table
CREATE TRIGGER set_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Function to automatically track conversion usage
CREATE OR REPLACE FUNCTION increment_conversion_usage()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.user_id IS NOT NULL THEN
        INSERT INTO monthly_conversion_usage (user_id, year_month, conversion_count)
        VALUES (NEW.user_id, TO_CHAR(NEW.created_at, 'YYYY-MM'), 1)
        ON CONFLICT (user_id, year_month) 
        DO UPDATE SET 
            conversion_count = monthly_conversion_usage.conversion_count + 1,
            last_updated = CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to track conversions automatically
CREATE TRIGGER track_conversion_usage
    AFTER INSERT ON conversions
    FOR EACH ROW
    EXECUTE FUNCTION increment_conversion_usage();

COMMENT ON FUNCTION increment_conversion_usage() IS 'Automatically increments monthly conversion count when a conversion is created';

-- ============================================
-- Useful Views
-- ============================================

-- View to get current active subscriptions with plan details
CREATE VIEW active_subscriptions AS
SELECT 
    s.id,
    s.user_id,
    u.email,
    u.full_name,
    s.plan_id,
    p.name AS plan_name,
    p.price AS plan_price,
    s.status,
    s.start_date,
    s.end_date,
    s.max_conversions_per_month,
    s.is_trial,
    s.auto_renew
FROM subscriptions s
JOIN users u ON s.user_id = u.id
LEFT JOIN plans p ON s.plan_id = p.id
WHERE s.status = 'ACTIVE';

COMMENT ON VIEW active_subscriptions IS 'Shows all active subscriptions with user and plan details';

-- View to monitor conversion usage vs limits
CREATE VIEW user_conversion_stats AS
SELECT 
    u.id AS user_id,
    u.email,
    u.full_name,
    COALESCE(mcu.conversion_count, 0) AS current_month_conversions,
    s.max_conversions_per_month,
    s.status AS subscription_status,
    p.name AS plan_name,
    CASE 
        WHEN s.max_conversions_per_month IS NULL THEN NULL
        WHEN COALESCE(mcu.conversion_count, 0) >= s.max_conversions_per_month THEN 'EXCEEDED'
        WHEN COALESCE(mcu.conversion_count, 0) >= (s.max_conversions_per_month * 0.8) THEN 'WARNING'
        ELSE 'OK'
    END AS usage_status,
    CASE 
        WHEN s.max_conversions_per_month IS NOT NULL AND s.max_conversions_per_month > 0 
        THEN ROUND((COALESCE(mcu.conversion_count, 0)::NUMERIC / s.max_conversions_per_month * 100), 2)
        ELSE NULL
    END AS usage_percentage
FROM users u
LEFT JOIN subscriptions s ON u.id = s.user_id AND s.status = 'ACTIVE'
LEFT JOIN plans p ON s.plan_id = p.id
LEFT JOIN monthly_conversion_usage mcu ON u.id = mcu.user_id 
    AND mcu.year_month = TO_CHAR(CURRENT_DATE, 'YYYY-MM');

COMMENT ON VIEW user_conversion_stats IS 'Real-time view of user conversion usage against their subscription limits';

-- ============================================
-- Initial Data: Plans
-- ============================================
INSERT INTO plans (name, description, price, currency, duration, monthly_price, annual_price, max_conversions) VALUES
('FREE', 'Free tier with limited conversions', 0.00, 'USD', 365, 0.00, 0.00, 100),
('BASIC', 'Basic plan with moderate usage', 10.00, 'USD', 30, 10.00, 100.00, 200),
('PRO', 'Professional plan with high usage limits', 25.00, 'USD', 30, 25.00, 250.00, 500),
('PREMIUM', 'Premium plan with very high limits', 50.00, 'USD', 365, 50.00, 500.00, 1000),
('BUSINESS', 'Business plan with maximum conversions', 50.00, 'USD', 30, 50.00, 500.00, 9999);


-- ============================================
-- End of V1__init.sql
-- ============================================