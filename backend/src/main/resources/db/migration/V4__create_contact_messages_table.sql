-- ============================================
-- Table: contact_messages
-- Stores user contact form submissions
-- ============================================
CREATE TABLE contact_messages (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reply_message TEXT,
    replied_at TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_replied BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT,
    CONSTRAINT fk_contact_message_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE SET NULL
);

-- Indexes for performance and filtering
CREATE INDEX idx_contact_messages_created_at ON contact_messages(created_at DESC);
CREATE INDEX idx_contact_messages_is_read ON contact_messages(is_read);
CREATE INDEX idx_contact_messages_is_replied ON contact_messages(is_replied);
CREATE INDEX idx_contact_messages_user_id ON contact_messages(user_id) WHERE user_id IS NOT NULL;

COMMENT ON TABLE contact_messages IS 'User contact form submissions and admin replies';
