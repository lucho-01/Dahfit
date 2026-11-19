-- ---------------------------------------------------------------------------
-- V4 - Credenciales de acceso.
--
-- Se guardan aparte de users: el perfil es publico para el propio usuario y se
-- lee en casi todas las pantallas; el hash no debe viajar con el. El email se
-- normaliza en la aplicacion (minusculas, sin espacios) antes de guardarse.
--
-- El login se activa con dafit.security.enabled=true. Por defecto queda
-- apagado para no romper el contrato del frontend durante la integracion.
-- ---------------------------------------------------------------------------

CREATE TABLE user_credentials (
    user_id       UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    email         VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT user_credentials_email_unique UNIQUE (email)
);
