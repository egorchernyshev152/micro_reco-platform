CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    session_id VARCHAR(255),
    source VARCHAR(100) NOT NULL,
    device VARCHAR(255),
    payload JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Ensure the new column exists even on старой схеме.
ALTER TABLE IF EXISTS events
    ADD COLUMN IF NOT EXISTS movie_id BIGINT;

-- Для совместимости временно создаем/оставляем старый столбец, чтобы перенести данные.
ALTER TABLE IF EXISTS events
    ADD COLUMN IF NOT EXISTS item_id BIGINT;

UPDATE events
SET movie_id = item_id
WHERE movie_id IS NULL AND item_id IS NOT NULL;

ALTER TABLE IF EXISTS events
    ALTER COLUMN movie_id SET NOT NULL;

ALTER TABLE IF EXISTS events
    DROP COLUMN IF EXISTS item_id;

-- Backfill missing columns when upgrading an existing database
ALTER TABLE IF EXISTS events
    ADD COLUMN IF NOT EXISTS session_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS source VARCHAR(100),
    ADD COLUMN IF NOT EXISTS device VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payload JSONB,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT NOW();

CREATE INDEX IF NOT EXISTS idx_events_user_created_at ON events (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_movie_created_at ON events (movie_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_events_type_created_at ON events (type, created_at DESC);
