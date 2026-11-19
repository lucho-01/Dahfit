-- ---------------------------------------------------------------------------
-- V5 - Reclamacion de cuentas (R-02) y peso corporal de la sesion (C-08).
--
-- R-02: una cuenta creada por un entrenador o un admin queda pendiente hasta
-- que su duenio entra con un codigo de un solo uso. El momento de la
-- reclamacion se guarda en user_credentials.claimed_at; los codigos se
-- almacenan hasheados y con caducidad, nunca en claro.
--
-- C-08: la sesion necesita el peso corporal con el que se abrio, no solo el
-- que tenia el perfil al evaluar. Sin esta columna, reabrir una sesion antigua
-- recalculaba sus series de peso corporal con el peso actual.
-- ---------------------------------------------------------------------------

ALTER TABLE user_credentials
    ADD COLUMN claimed_at TIMESTAMPTZ;

CREATE TABLE claim_codes (
    user_id    UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    code_hash  VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX claim_codes_expiry ON claim_codes (expires_at);

ALTER TABLE workouts
    ADD COLUMN bodyweight_kg NUMERIC(7, 3)
        CHECK (bodyweight_kg IS NULL OR bodyweight_kg > 0);
