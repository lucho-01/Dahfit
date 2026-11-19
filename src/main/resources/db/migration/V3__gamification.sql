-- ---------------------------------------------------------------------------
-- V3 - Gamificacion (C-19).
--
-- El XP es una metrica de constancia y nunca entra en el calculo de rango.
--   * xp_events: ledger inmutable. UNIQUE (user_id, source_key) hace la
--     idempotencia responsabilidad de la base, no solo del codigo.
--   * streaks: una fila por usuario; el record no se pierde al cortar racha.
--   * user_badges: una insignia por usuario y tipo.
-- ---------------------------------------------------------------------------

CREATE TABLE xp_events (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(30) NOT NULL
                CHECK (type IN ('SESSION_COMPLETED', 'FIRST_SESSION_OF_WEEK',
                                'FOUR_WEEK_REVIEW')),
    points      INTEGER     NOT NULL CHECK (points >= 0),
    local_date  DATE        NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    source_key  VARCHAR(120) NOT NULL,
    CONSTRAINT xp_events_unique_source UNIQUE (user_id, source_key)
);

CREATE INDEX xp_events_user_date ON xp_events (user_id, local_date DESC);

CREATE TABLE streaks (
    user_id            UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    current_days       INTEGER NOT NULL DEFAULT 0 CHECK (current_days >= 0),
    longest_days       INTEGER NOT NULL DEFAULT 0 CHECK (longest_days >= 0),
    last_activity_date DATE
);

CREATE TABLE user_badges (
    id        UUID PRIMARY KEY,
    user_id   UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    badge     VARCHAR(40) NOT NULL
              CHECK (badge IN ('PRIMER_ENTRENAMIENTO', 'RACHA_4',
                               'DIEZ_ENTRENAMIENTOS')),
    earned_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT user_badges_unique UNIQUE (user_id, badge)
);
