-- V9__Update_Payments_Table_MySQL.sql
-- MySQL-specific migration for payments table enhancements

-- Add new columns to payments table
ALTER TABLE payments 
ADD COLUMN updated_at TIMESTAMP NULL DEFAULT NULL AFTER created_at,
ADD COLUMN payment_gateway VARCHAR(50) NULL AFTER transaction_id,
ADD COLUMN billing_cycle VARCHAR(20) NULL AFTER payment_gateway,
ADD COLUMN description VARCHAR(500) NULL AFTER billing_cycle,
ADD COLUMN failure_reason VARCHAR(500) NULL AFTER description,
ADD COLUMN refund_amount DECIMAL(10, 2) NULL AFTER failure_reason,
ADD COLUMN refund_date TIMESTAMP NULL AFTER refund_amount,
ADD COLUMN metadata TEXT NULL AFTER refund_date;

-- Modify existing columns with better constraints
ALTER TABLE payments 
MODIFY COLUMN amount DECIMAL(10, 2) NOT NULL,
MODIFY COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'USD',
MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
MODIFY COLUMN payment_method VARCHAR(50) NULL,
MODIFY COLUMN transaction_id VARCHAR(100) NULL;

-- Create indexes for better query performance
CREATE INDEX idx_payment_user_id ON payments(user_id);
CREATE INDEX idx_payment_subscription_id ON payments(subscription_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payment_created_at ON payments(created_at);
CREATE INDEX idx_payment_gateway ON payments(payment_gateway);
CREATE INDEX idx_payment_user_status ON payments(user_id, status);
CREATE INDEX idx_payment_updated_at ON payments(updated_at);

-- Add check constraints (MySQL 8.0.16+)
ALTER TABLE payments 
ADD CONSTRAINT chk_payment_amount_positive 
CHECK (amount > 0);

ALTER TABLE payments 
ADD CONSTRAINT chk_refund_amount_positive 
CHECK (refund_amount IS NULL OR refund_amount >= 0);

ALTER TABLE payments 
ADD CONSTRAINT chk_refund_amount_not_exceed_payment 
CHECK (refund_amount IS NULL OR refund_amount <= amount);

-- Update existing records
UPDATE payments SET updated_at = created_at WHERE updated_at IS NULL;
UPDATE payments SET currency = 'USD' WHERE currency IS NULL OR currency = '';
UPDATE payments SET status = 'PENDING' WHERE status IS NULL OR status = '';

-- Add foreign key constraints with proper names
ALTER TABLE payments 
ADD CONSTRAINT fk_payment_user 
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE payments 
ADD CONSTRAINT fk_payment_subscription 
FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE ON UPDATE CASCADE;