-- V1__init.sql
-- Schema complet consolidé pour le système de conversion de formules mathématiques
-- Compatible avec les entités JPA Java

-- ============================================
-- Table: users
-- Stocke les informations des comptes utilisateurs
-- ============================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================
-- Table: plans
-- Définit les différents plans d'abonnement disponibles
-- ============================================
CREATE TABLE plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DOUBLE NOT NULL,
    currency VARCHAR(10) NOT NULL,
    duration INT NOT NULL
);

-- ============================================
-- Table: conversions
-- Suit l'historique des conversions de formules mathématiques
-- ============================================
CREATE TABLE conversions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    
    input_format ENUM('TEXT', 'LATEX', 'MATHML', 'UNICODE') NOT NULL DEFAULT 'TEXT',
    
    output_format ENUM('PYTHON', 'NUMPY', 'SYMPY', 'SCIPY', 'JAVASCRIPT', 'MATLAB', 'R') NOT NULL DEFAULT 'PYTHON',
    
    prompt TEXT NOT NULL,
    ai_response TEXT NOT NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================
-- Table: subscriptions
-- Gère les abonnements des utilisateurs
-- ============================================
CREATE TABLE subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    plan_id BIGINT NOT NULL,
    status ENUM('ACTIVE', 'CANCELLED', 'EXPIRED', 'PENDING') NOT NULL DEFAULT 'PENDING',
    duration ENUM('ONE_MONTH', 'THREE_MONTHS', 'TWELVE_MONTHS') NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (plan_id) REFERENCES plans(id) ON DELETE RESTRICT ON UPDATE CASCADE
);

-- ============================================
-- Table: payments
-- Suit les transactions de paiement
-- ============================================
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    amount DOUBLE NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status ENUM('PENDING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    transaction_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id) ON DELETE CASCADE
);

-- ============================================
-- Index pour optimiser les performances
-- ============================================
CREATE INDEX idx_conversions_user_created ON conversions(user_id, created_at);
CREATE INDEX idx_subscriptions_user_status ON subscriptions(user_id, status);
CREATE INDEX idx_payments_user_status ON payments(user_id, status);
CREATE INDEX idx_subscriptions_plan ON subscriptions(plan_id);
CREATE INDEX idx_subscriptions_status ON subscriptions(status);

-- ============================================
-- Données initiales pour les plans (optionnel)
-- ============================================
INSERT INTO plans (name, price, currency, duration) VALUES
('FREE', 0.00, 'USD', 365),
('BASIC', 9.99, 'USD', 30),
('PRO', 29.99, 'USD', 30),
('PREMIUM', 99.99, 'USD', 365);