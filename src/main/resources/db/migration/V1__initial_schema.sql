-- =============================================================================
-- DAFIT / DAHIAN — esquema inicial
-- PostgreSQL 16+ (Neon). Alineado 1:1 con com.dafit.domain.model.*
--
-- Convenciones:
--   * UUID generados en la aplicación.
--   * Instantes en TIMESTAMPTZ, normalizados a UTC (C-18).
--   * Fecha local de la sesión se guarda aparte de la zona horaria (C-18).
--   * Todo lo opcional es NULL, nunca 0 ni centinela (C-15).
--   * Los CHECK replican los enums del dominio: si diverge, falla al insertar
--     y no en la capa de aplicación.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Control de esquema
-- -----------------------------------------------------------------------------

CREATE TABLE dafit_schema_info (
    id             SMALLINT    PRIMARY KEY CHECK (id = 1),
    engine_version VARCHAR(20) NOT NULL,
    installed_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO dafit_schema_info (id, engine_version) VALUES (1, '0.1.0');

-- -----------------------------------------------------------------------------
-- Usuarios y datos corporales
-- -----------------------------------------------------------------------------

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
    created_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT users_bodyweight_positive
        CHECK (bodyweight_kg IS NULL OR bodyweight_kg > 0)
);

CREATE TABLE body_metrics (
    id          UUID PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    weight_kg   NUMERIC(7, 3) NOT NULL CHECK (weight_kg > 0),
    measured_on DATE          NOT NULL,
    recorded_at TIMESTAMPTZ   NOT NULL,
    source      VARCHAR(40)   NOT NULL
);

-- Varias pesadas el mismo dia son validas: el dominio dice que cada pesada es
-- una fila nueva (no se sobrescribe).
CREATE INDEX body_metrics_user_recent ON body_metrics (user_id, measured_on DESC);

-- -----------------------------------------------------------------------------
-- Catálogo de ejercicios
-- -----------------------------------------------------------------------------

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
    -- C-08: fracción del peso corporal movilizado. NULL en los ejercicios que
    -- no son de peso corporal (el dominio no inventa un valor que no aplica).
    bodyweight_factor NUMERIC(4, 3)
                      CHECK (bodyweight_factor IS NULL
                             OR (bodyweight_factor >= 0 AND bodyweight_factor <= 1)),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL
);

CREATE INDEX exercises_active_evaluable ON exercises (active, evaluable);

CREATE TABLE exercise_muscles (
    exercise_id UUID        NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    muscle      VARCHAR(20) NOT NULL
                CHECK (muscle IN ('CHEST', 'BACK', 'SHOULDERS', 'BICEPS',
                                  'TRICEPS', 'QUADRICEPS', 'HAMSTRINGS', 'GLUTES')),
    PRIMARY KEY (exercise_id, muscle)
);

-- -----------------------------------------------------------------------------
-- Motor de evaluación
-- -----------------------------------------------------------------------------

-- Coeficiente de participación: decide qué ejercicios cuentan y mide cobertura
-- (§7.4, C-14). No escala la nota (C-09).
CREATE TABLE muscle_coefficients (
    id          UUID PRIMARY KEY,
    exercise_id UUID          NOT NULL REFERENCES exercises (id) ON DELETE CASCADE,
    muscle      VARCHAR(20)   NOT NULL,
    coefficient NUMERIC(4, 3) NOT NULL CHECK (coefficient >= 0 AND coefficient <= 1),
    version     VARCHAR(40)   NOT NULL,
    approved    BOOLEAN       NOT NULL DEFAULT FALSE,
    CONSTRAINT muscle_coefficients_unique UNIQUE (exercise_id, muscle, version)
);

-- Prueba primaria o sustitución permitida. Sin ella el músculo no emite rango.
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
    created_at                           TIMESTAMPTZ  NOT NULL,
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

-- -----------------------------------------------------------------------------
-- Entrenamientos
-- -----------------------------------------------------------------------------

CREATE TABLE workouts (
    id          UUID PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    local_date  DATE        NOT NULL,   -- C-18
    time_zone   VARCHAR(64) NOT NULL,   -- C-18
    started_at  TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status      VARCHAR(20) NOT NULL
                CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    notes       VARCHAR(1000),
    CONSTRAINT workouts_finish_after_start
        CHECK (finished_at IS NULL OR finished_at >= started_at)
);

-- Una sola sesión abierta por usuario (WorkoutSessionService).
CREATE UNIQUE INDEX workouts_one_open_per_user
    ON workouts (user_id)
    WHERE status = 'IN_PROGRESS';

CREATE INDEX workouts_user_date ON workouts (user_id, local_date DESC);

CREATE TABLE workout_sets (
    -- C-17: el id es el clientUuid del dispositivo. Reenviar no duplica.
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
    -- C-04: se registra 0-5; oficial solo <= 4.
    rir               INTEGER       NOT NULL CHECK (rir BETWEEN 0 AND 5),
    type              VARCHAR(20)   NOT NULL
                      CHECK (type IN ('WORKING', 'WARMUP', 'BACKOFF',
                                      'DROP', 'AMRAP', 'FAILURE')),
    notes             VARCHAR(500),
    performed_at      TIMESTAMPTZ   NOT NULL,
    bodyweight_kg     NUMERIC(7, 3),
    bodyweight_factor NUMERIC(4, 3),
    CONSTRAINT workout_sets_unique_order UNIQUE (workout_id, set_order)
);

CREATE INDEX workout_sets_workout ON workout_sets (workout_id, set_order);
CREATE INDEX workout_sets_exercise ON workout_sets (exercise_id, performed_at DESC);

-- -----------------------------------------------------------------------------
-- Rutinas
-- -----------------------------------------------------------------------------

CREATE TABLE routines (
    id             UUID PRIMARY KEY,
    owner_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    assigned_to_id UUID         REFERENCES users (id) ON DELETE SET NULL,
    name           VARCHAR(120) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL
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

-- -----------------------------------------------------------------------------
-- Evaluaciones (§7, C-13, C-15)
-- -----------------------------------------------------------------------------

CREATE TABLE exercise_assessments (
    id                 UUID PRIMARY KEY,
    user_id            UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exercise_id        UUID          NOT NULL REFERENCES exercises (id),
    status             VARCHAR(30)   NOT NULL
                       CHECK (status IN ('OK', 'SIN_DATOS', 'FALTA_PESO_CORPORAL',
                                         'FALTA_CATEGORIA', 'SIN_FUENTE',
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
    -- C-13: la evaluación recuerda con qué algoritmo y qué tabla se calculó.
    algorithm_version  VARCHAR(20)   NOT NULL
                       CHECK (algorithm_version IN ('EPLEY_V1', 'BRZYCKI_V1')),
    benchmark_table_id UUID          REFERENCES benchmark_tables (id),
    bodyweight_used_kg NUMERIC(7, 3),
    best_set_id        UUID          REFERENCES workout_sets (id) ON DELETE SET NULL,
    assessed_at        TIMESTAMPTZ   NOT NULL,
    -- C-15: si hay rango oficial hay puntos; si no, ambos nulos.
    CONSTRAINT exercise_assessments_official_consistency
        CHECK ((rank IS NULL AND points IS NULL) OR (rank IS NOT NULL AND points IS NOT NULL))
);

CREATE INDEX exercise_assessments_user_exercise
    ON exercise_assessments (user_id, exercise_id, assessed_at DESC);

CREATE TABLE muscle_assessments (
    id                      UUID PRIMARY KEY,
    user_id                 UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    muscle                  VARCHAR(20)   NOT NULL
                            CHECK (muscle IN ('CHEST', 'BACK', 'SHOULDERS', 'BICEPS',
                                              'TRICEPS', 'QUADRICEPS', 'HAMSTRINGS',
                                              'GLUTES')),
    status                  VARCHAR(30)   NOT NULL,
    points                  INTEGER       CHECK (points IS NULL OR points BETWEEN 0 AND 1000),
    rank                    VARCHAR(20),
    coverage                NUMERIC(4, 3) CHECK (coverage IS NULL OR coverage BETWEEN 0 AND 1),
    primary_tests_evaluated INTEGER       NOT NULL DEFAULT 0,
    assessed_at             TIMESTAMPTZ   NOT NULL,
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
    assessed_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT global_assessments_official_consistency
        CHECK ((rank IS NULL AND points IS NULL) OR (rank IS NOT NULL AND points IS NOT NULL))
);

CREATE INDEX global_assessments_user
    ON global_assessments (user_id, assessed_at DESC);
