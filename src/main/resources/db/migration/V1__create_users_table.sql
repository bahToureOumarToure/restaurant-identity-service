-- ============================================================================
-- V1 — Création de la table users
-- ============================================================================
-- Schéma propre au identity-service. Aucune FK vers d'autres services.
-- Toute référence à un User depuis un autre service se fera par UUID uniquement.
-- ============================================================================

CREATE TABLE users (
                       id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       email           VARCHAR(255) NOT NULL,
                       password_hash   VARCHAR(255) NOT NULL,
                       first_name      VARCHAR(100),
                       last_name       VARCHAR(100),
                       phone           VARCHAR(30),
                       role            VARCHAR(20)  NOT NULL DEFAULT 'CLIENT',
                       enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

                       CONSTRAINT users_email_unique UNIQUE (email),
                       CONSTRAINT users_role_check   CHECK (role IN ('CLIENT', 'STAFF', 'ADMIN', 'SUPER_ADMIN'))
);

-- Index pour les recherches par email (login)
CREATE INDEX idx_users_email ON users(email);

-- Commentaires de documentation (visibles dans pgAdmin, DBeaver…)
COMMENT ON TABLE  users               IS 'Utilisateurs du système (clients, staff, admins)';
COMMENT ON COLUMN users.role          IS 'CLIENT | STAFF | ADMIN | SUPER_ADMIN';
COMMENT ON COLUMN users.password_hash IS 'BCrypt hash, jamais le mot de passe en clair';