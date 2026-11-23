-- =============================================================================
-- Esquema de TESTS (perfil test/jpa, H2 en modo PostgreSQL).
--
-- Equivalente portatil del estado FINAL de las migraciones V1..V7 de
-- src/main/resources/db/migration. Si se cambia una migracion de produccion,
-- este archivo debe actualizarse en el mismo commit.
--
-- Diferencias deliberadas con produccion:
--   * TIMESTAMPTZ -> TIMESTAMP WITH TIME ZONE (H2 no conoce el alias PG).
--   * Sin el indice parcial workouts_one_open_per_user: H2 no soporta
--     CREATE UNIQUE INDEX ... WHERE. En PostgreSQL ese indice existe (V1) y en
--     tests la regla "una sola sesion abierta" la impone WorkoutSessionService.
--   * El CHECK de exercise_assessments ya incluye NO_DISPONIBLE (V7).
-- =============================================================================

CREATE TABLE dafit_schema_info (
    id             SMALLINT    PRIMARY KEY CHECK (id = 1),
    engine_version VARCHAR(20) NOT NULL,
    installed_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

INSERT INTO dafit_schema_info (id, engine_version) VALUES (1, '0.1.0');

-- ------------------------------------------------------------------ usuarios

CREATE TABLE users (
    id                UUID PRIMARY KEY,
    display_name      VARCHAR(120) NOT NULL,
    role              VARCHAR(20)  NOT NULL
                      CHECK (role IN ('ATHLETE', 'COACH', 'ADMIN')),
    sex               VARCHAR(12)  NOT NULL
                      CHECK (sex IN ('MALE', 'FEMALE', 'UNSPECIFIED')),
    birth_date        DATE,
    bodyweight_kg     NUMERIC(7, 3),
    preferred_unit    VARCHAR(5)   NOT NULL
                      CHECK (preferred_unit IN ('KG', 'LB')),
    time_zone         VARCHAR(64)  NOT NULL,
    benchmark_consent BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT users_bodyweight_positive
        CHECK (bodyweight_kg IS NULL OR bodyweight_kg > 0)
);

CREATE TABLE body_metrics (
    id          UUID PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    weight_kg   NUMERIC(7, 3) NOT NULL CHECK (weight_kg > 0),
    measured_on DATE          NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    source      VARCHAR(40)   NOT NULL
);

CREATE INDEX body_metrics_user_recent ON body_metrics (user_id, measured_on DESC);

-- -------------------------------------------------------- catalogo ejercicios

CREATE TABLE exercises (
    id                UUID PRIMARY KEY,
    name              VARCHAR(120) NOT NULL,
    equipment         VARCHAR(20)  NOT NULL
                      CHECK (equipment IN ('BARBELL', 'DUMBBELL', 'MACHINE',
                                           'CABLE', 'BODYWEIGHT', 'KETTLEBELL',
                                           'BAND', 'OTHER')),
    load_mode         VARCHAR(20)  NOT NULL
                      CHECK (load_mode IN ('TOTAL', 'PER_HAND', 'PER_SIDE',
                                           'BODYWEIGHT', 'BODYWEIGHT_ADDED',
                                           'ASSISTED', 'MACHINE_STACK', 'BAND')),
    unilateral        BOOLEAN      NOT NULL DEFAULT FALSE,
    evaluable         BOOLEAN      NOT NULL DEFAULT TRUE,
    bodyweight_factor NUMERIC(4, 3)
                      CHECK (bodyweight_factor IS NULL
                             OR (bodyweight_factor >= 0 AND bodyweight_factor <= 1)),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX exercises_active_evaluable ON exercises (active, evaluable);

CREATE TABLE exercise_muscles (
    exercise_id UUID        NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    muscle      VARCHAR(20) NOT NULL
                CHECK (muscle IN ('CHEST', 'BACK', 'SHOULDERS', 'BICEPS',
                                  'TRICEPS', 'QUADRICEPS', 'HAMSTRINGS', 'GLUTES')),
    PRIMARY KEY (exercise_id, muscle)
);

-- ------------------------------------------------------- motor evaluacion

CREATE TABLE muscle_coefficients (
    id          UUID PRIMARY KEY,
    exercise_id UUID          NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    muscle      VARCHAR(20)   NOT NULL,
    coefficient NUMERIC(4, 3) NOT NULL CHECK (coefficient >= 0 AND coefficient <= 1),
    version     VARCHAR(40)   NOT NULL,
    approved    BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT muscle_coefficients_unique UNIQUE (exercise_id, muscle, version)
);

CREATE TABLE primary_tests (
    id          UUID PRIMARY KEY,
    muscle      VARCHAR(20) NOT NULL,
    exercise_id UUID        NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    primary_    BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT primary_tests_unique UNIQUE (muscle, exercise_id)
);

CREATE TABLE benchmark_tables (
    id                                   UUID PRIMARY KEY,
    exercise_id                          UUID         NOT NULL
                                         REFERENCES exercises (id) ON DELETE CASCADE,
    source                               VARCHAR(120) NOT NULL,
    source_version                       VARCHAR(40)  NOT NULL,
    sex                                  VARCHAR(12)  NOT NULL,
    age_band                             VARCHAR(20),
    bodyweight_kg                        NUMERIC(7, 3),
    licensed                             BOOLEAN      NOT NULL DEFAULT FALSE,
    bodyweight_interpolation_allowed     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                           TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT benchmark_tables_unique_version
        UNIQUE (exercise_id, source, source_version, sex, age_band, bodyweight_kg)
);

CREATE TABLE benchmark_points (
    id                 UUID PRIMARY KEY,
    benchmark_table_id UUID     NOT NULL
                       REFERENCES benchmark_tables (id) ON DELETE CASCADE,
    percentile         INTEGER  NOT NULL CHECK (percentile BETWEEN 0 AND 100),
    load_kg            NUMERIC(7, 3) NOT NULL CHECK (load_kg > 0),
    CONSTRAINT benchmark_points_unique_percentile
        UNIQUE (benchmark_table_id, percentile)
);

CREATE INDEX benchmark_points_table_asc
    ON benchmark_points (benchmark_table_id, percentile);

-- ------------------------------------------------------------ entrenamientos

CREATE TABLE workouts (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    local_date  DATE        NOT NULL,
    time_zone   VARCHAR(64) NOT NULL,
    started_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    status      VARCHAR(20) NOT NULL
                CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    notes       VARCHAR(1000),
    bodyweight_kg NUMERIC(7, 3)
                  CHECK (bodyweight_kg IS NULL OR bodyweight_kg > 0),
    CONSTRAINT workouts_finish_after_start
        CHECK (finished_at IS NULL OR finished_at >= started_at)
);

-- Nota: en PostgreSQL, V1 anade el indice parcial workouts_one_open_per_user
-- (una sola sesion IN_PROGRESS por usuario). H2 no soporta indices parciales.

CREATE INDEX workouts_user_date ON workouts (user_id, local_date DESC);

CREATE TABLE workout_sets (
    id                UUID PRIMARY KEY,
    workout_id        UUID          NOT NULL REFERENCES workouts (id) ON DELETE CASCADE,
    exercise_id       UUID          NOT NULL REFERENCES exercises (id),
    set_order         INTEGER       NOT NULL CHECK (set_order > 0),
    load_kg           NUMERIC(7, 3) NOT NULL CHECK (load_kg >= 0),
    input_unit        VARCHAR(5)    NOT NULL CHECK (input_unit IN ('KG', 'LB')),
    load_mode         VARCHAR(20)   NOT NULL
                      CHECK (load_mode IN ('TOTAL', 'PER_HAND', 'PER_SIDE',
                                           'BODYWEIGHT', 'BODYWEIGHT_ADDED',
                                           'ASSISTED', 'MACHINE_STACK', 'BAND')),
    implement_count   INTEGER       NOT NULL DEFAULT 1 CHECK (implement_count >= 1),
    reps              INTEGER       NOT NULL CHECK (reps BETWEEN 0 AND 100),
    rir               INTEGER       NOT NULL CHECK (rir BETWEEN 0 AND 5),
    type              VARCHAR(20)   NOT NULL
                      CHECK (type IN ('WORKING', 'WARMUP', 'BACKOFF',
                                      'DROP', 'AMRAP', 'FAILURE')),
    notes             VARCHAR(500),
    performed_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    bodyweight_kg     NUMERIC(7, 3),
    bodyweight_factor NUMERIC(4, 3),
    CONSTRAINT workout_sets_unique_order UNIQUE (workout_id, set_order)
);

CREATE INDEX workout_sets_workout ON workout_sets (workout_id, set_order);
CREATE INDEX workout_sets_exercise ON workout_sets (exercise_id, performed_at DESC);

-- ------------------------------------------------------------------ rutinas

CREATE TABLE routines (
    id             UUID PRIMARY KEY,
    owner_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    assigned_to_id UUID         REFERENCES users (id) ON DELETE SET NULL,
    name           VARCHAR(120) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX routines_owner ON routines (owner_id, created_at DESC);
CREATE INDEX routines_assigned ON routines (assigned_to_id);

CREATE TABLE routine_exercises (
    id             UUID PRIMARY KEY,
    routine_id     UUID    NOT NULL REFERENCES routines (id) ON DELETE CASCADE,
    exercise_id    UUID    NOT NULL REFERENCES exercises (id),
    exercise_order INTEGER NOT NULL CHECK (exercise_order > 0),
    target_sets    INTEGER NOT NULL CHECK (target_sets BETWEEN 1 AND 20),
    target_reps    INTEGER NOT NULL CHECK (target_reps BETWEEN 0 AND 100),
    rest_seconds   INTEGER CHECK (rest_seconds IS NULL OR rest_seconds >= 0),
    CONSTRAINT routine_exercises_unique_order UNIQUE (routine_id, exercise_order)
);

-- -------------------------------------------------------------- evaluaciones

CREATE TABLE exercise_assessments (
    id                 UUID PRIMARY KEY,
    user_id            UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_id        UUID          NOT NULL REFERENCES exercises (id),
    status             VARCHAR(30)   NOT NULL
                       -- V7: NO_DISPONIBLE (DEC-05) incluido
                       CHECK (status IN ('OK', 'SIN_DATOS', 'FALTA_PESO_CORPORAL',
                                         'FALTA_CATEGORIA', 'SIN_FUENTE',
                                         'NO_DISPONIBLE',
                                         'FUERA_DE_TABLA_P5', 'FUERA_DE_TABLA_P95',
                                         'COBERTURA_INSUFICIENTE')),
    points             INTEGER       CHECK (points IS NULL OR points BETWEEN 0 AND 1000),
    rank               VARCHAR(20)
                       CHECK (rank IS NULL OR rank IN ('PRINCIPIANTE', 'NOVATO',
                                                        'AVANZADO', 'BESTIA',
                                                        'ELITE', 'BRUTAL')),
    e1rm_kg            NUMERIC(8, 4),
    relative_strength  NUMERIC(8, 4),
    percentile         NUMERIC(5, 2),
    algorithm_version  VARCHAR(20)   NOT NULL
                       CHECK (algorithm_version IN ('EPLEY_V1', 'BRZYCKI_V1')),
    benchmark_table_id UUID          REFERENCES benchmark_tables (id),
    bodyweight_used_kg NUMERIC(7, 3),
    best_set_id        UUID          REFERENCES workout_sets (id) ON DELETE SET NULL,
    assessed_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT exercise_assessments_official_consistency
        CHECK ((rank IS NULL AND points IS NULL) OR (rank IS NOT NULL AND points IS NOT NULL))
);

CREATE INDEX exercise_assessments_user_exercise
    ON exercise_assessments (user_id, exercise_id, assessed_at DESC);

CREATE TABLE muscle_assessments (
    id                      UUID PRIMARY KEY,
    user_id                 UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    muscle                  VARCHAR(20) NOT NULL
                            CHECK (muscle IN ('CHEST', 'BACK', 'SHOULDERS', 'BICEPS',
                                              'TRICEPS', 'QUADRICEPS', 'HAMSTRINGS',
                                              'GLUTES')),
    status                  VARCHAR(30) NOT NULL,
    points                  INTEGER     CHECK (points IS NULL OR points BETWEEN 0 AND 1000),
    rank                    VARCHAR(20),
    coverage                NUMERIC(4, 3) CHECK (coverage IS NULL OR coverage BETWEEN 0 AND 1),
    primary_tests_evaluated INTEGER     NOT NULL DEFAULT 0,
    assessed_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT muscle_assessments_official_consistency
        CHECK ((rank IS NULL AND points IS NULL) OR (rank IS NOT NULL AND points IS NOT NULL))
);

CREATE INDEX muscle_assessments_user_muscle
    ON muscle_assessments (user_id, muscle, assessed_at DESC);

CREATE TABLE muscle_assessment_exercises (
    muscle_assessment_id UUID NOT NULL
                         REFERENCES muscle_assessments (id) ON DELETE CASCADE,
    exercise_id          UUID NOT NULL,
    PRIMARY KEY (muscle_assessment_id, exercise_id)
);

CREATE TABLE global_assessments (
    id                UUID PRIMARY KEY,
    user_id           UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status            VARCHAR(30) NOT NULL,
    points            INTEGER     CHECK (points IS NULL OR points BETWEEN 0 AND 1000),
    rank              VARCHAR(20),
    muscles_evaluated INTEGER     NOT NULL CHECK (muscles_evaluated BETWEEN 0 AND 8),
    muscles_required  INTEGER     NOT NULL CHECK (muscles_required > 0),
    assessed_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT global_assessments_official_consistency
        CHECK ((rank IS NULL AND points IS NULL) OR (rank IS NOT NULL AND points IS NOT NULL))
);

CREATE INDEX global_assessments_user
    ON global_assessments (user_id, assessed_at DESC);

-- --------------------------------------------------------------- gamificacion

CREATE TABLE xp_events (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(30) NOT NULL
                CHECK (type IN ('SESSION_COMPLETED', 'FIRST_SESSION_OF_WEEK',
                                'FOUR_WEEK_REVIEW')),
    points      INTEGER     NOT NULL CHECK (points >= 0),
    local_date  DATE        NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
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
    earned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT user_badges_unique UNIQUE (user_id, badge)
);

-- ------------------------------------------------------- credenciales (V4/V5)

CREATE TABLE user_credentials (
    user_id       UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    email         VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    claimed_at    TIMESTAMP WITH TIME ZONE,
    CONSTRAINT user_credentials_email_unique UNIQUE (email)
);

CREATE TABLE claim_codes (
    user_id    UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    code_hash  VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX claim_codes_expiry ON claim_codes (expires_at);
