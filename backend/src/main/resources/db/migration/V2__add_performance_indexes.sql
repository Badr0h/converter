-- Ajout des index pour améliorer les performances
-- Créé le 2025-01-28 pour corriger les problèmes de performance identifiés

-- Index pour les recherches fréquentes sur les conversions
CREATE INDEX IF NOT EXISTS idx_conversions_user_id ON conversions(user_id);
CREATE INDEX IF NOT EXISTS idx_conversions_created_at ON conversions(created_at);
CREATE INDEX IF NOT EXISTS idx_conversions_user_created ON conversions(user_id, created_at);

-- Index pour les recherches d'utilisateurs par email
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Index pour les recherches d'abonnements
CREATE INDEX IF NOT EXISTS idx_subscriptions_user_id ON subscriptions(user_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_status ON subscriptions(status);

-- Index pour les paiements
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

-- Commentaires pour expliquer l'utilité de chaque index
COMMENT ON INDEX idx_conversions_user_id IS 'Accélère les recherches de conversions par utilisateur';
COMMENT ON INDEX idx_conversions_created_at IS 'Accélère les recherches de conversions par date';
COMMENT ON INDEX idx_conversions_user_created IS 'Accélère les recherches de conversions par utilisateur et date (combiné)';
COMMENT ON INDEX idx_users_email IS 'Accélère les recherches d''utilisateurs par email (login/inscription)';
COMMENT ON INDEX idx_subscriptions_user_id IS 'Accélère les recherches d''abonnements par utilisateur';
COMMENT ON INDEX idx_subscriptions_status IS 'Accélère les recherches d''abonnements par statut';
COMMENT ON INDEX idx_payments_user_id IS 'Accélère les recherches de paiements par utilisateur';
COMMENT ON INDEX idx_payments_status IS 'Accélère les recherches de paiements par statut';
