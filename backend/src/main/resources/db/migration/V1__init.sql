-- V1__init.sql
-- Complete consolidated schema for mathematical formula conversion system
-- Optimized for PostgreSQL with all enhancements from V1-V9

-- ============================================
-- Table: users
-- Stores user account information
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
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

-- Create index on email for faster lookups
CREATE INDEX idx_users_email ON users(email);

-- ============================================
-- Table: plans
-- Defines available subscription plans
-- ============================================
CREATE TABLE plans (
    id BIGSERIAL PRIMARY KEY,
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
    id BIGSERIAL PRIMARY KEY,
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

COMMENT ON TABLE conversions IS 'Updated to support bidirectional format conversion - all formats can be used as both input and output';

-- ============================================
-- Table: subscriptions
-- Manages user subscriptions
-- ============================================
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED', 'PENDING')),
    duration VARCHAR(20) NOT NULL 
        CHECK (duration IN ('ONE_MONTH', 'THREE_MONTHS', 'TWELVE_MONTHS')),
    start_date DATE NOT NULL,
    end_date DATE,
    max_conversions_per_month INTEGER DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_subscription_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) 
        REFERENCES plans(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_end_date_after_start CHECK (end_date IS NULL OR end_date >= start_date)
);

COMMENT ON COLUMN subscriptions.max_conversions_per_month IS 'Maximum conversions allowed per month based on subscription plan';

-- ============================================
-- Table: payments
-- Tracks payment transactions
-- ============================================
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
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

-- ============================================
-- Indexes for performance optimization
-- ============================================

-- Conversions indexes
CREATE INDEX idx_conversions_user_created ON conversions(user_id, created_at DESC);
CREATE INDEX idx_conversions_user ON conversions(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_conversions_created_at ON conversions(created_at DESC);

-- Subscriptions indexes
CREATE INDEX idx_subscriptions_user_status ON subscriptions(user_id, status);
CREATE INDEX idx_subscriptions_plan ON subscriptions(plan_id) WHERE plan_id IS NOT NULL;
CREATE INDEX idx_subscriptions_status ON subscriptions(status);
CREATE INDEX idx_subscriptions_end_date ON subscriptions(end_date) WHERE end_date IS NOT NULL;

-- Payments indexes
CREATE INDEX idx_payments_user_status ON payments(user_id, status);
CREATE INDEX idx_payments_user ON payments(user_id);
CREATE INDEX idx_payments_subscription ON payments(subscription_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_transaction_id ON payments(transaction_id) WHERE transaction_id IS NOT NULL;
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);
CREATE INDEX idx_payments_gateway ON payments(payment_gateway) WHERE payment_gateway IS NOT NULL;
CREATE INDEX idx_payments_updated_at ON payments(updated_at) WHERE updated_at IS NOT NULL;

-- ============================================
-- Trigger for updated_at timestamp
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

-- ============================================
-- Initial data for plans
-- ============================================
INSERT INTO plans (name, description, price, currency, duration, monthly_price, annual_price, max_conversions) VALUES
('FREE', 'Free tier with limited conversions', 0.00, 'USD', 365, 0.00, 0.00, 100),
('BASIC', 'Basic plan with moderate usage', 10.00, 'USD', 30, 10.00, 100.00, 200),
('PRO', 'Professional plan with high usage limits', 25.00, 'USD', 30, 25.00, 250.00, 500),
('PREMIUM', 'Premium plan with very high limits', 50.00, 'USD', 365, 50.00, 500.00, 1000),
('BUSINESS', 'Business plan with maximum conversions', 50.00, 'USD', 30, 50.00, 500.00, 9999);