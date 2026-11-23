ALTER TABLE benchmark_points
    ALTER COLUMN percentile TYPE NUMERIC(5, 2)
    USING percentile::NUMERIC(5, 2);

CREATE TABLE sync_batches (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX sync_batches_user_received
    ON sync_batches (user_id, received_at DESC);

CREATE TABLE sync_batch_results (
    id         UUID         PRIMARY KEY,
    batch_id   UUID         NOT NULL REFERENCES sync_batches (id) ON DELETE CASCADE,
    item_order INTEGER      NOT NULL CHECK (item_order >= 0),
    item_key   VARCHAR(128) NOT NULL,
    target     VARCHAR(32)  NOT NULL CHECK (target IN ('WORKOUT_SET', 'WORKOUT_FINISH')),
    outcome    VARCHAR(32)  NOT NULL CHECK (outcome IN ('APPLIED', 'DUPLICATE', 'REJECTED')),
    message    TEXT,
    CONSTRAINT sync_batch_results_order_unique UNIQUE (batch_id, item_order)
);
