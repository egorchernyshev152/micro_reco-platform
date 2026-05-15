CREATE TABLE IF NOT EXISTS recommendation_strategies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    algorithm VARCHAR(50) NOT NULL,
    event_weights JSONB,
    time_decay_half_life_days INT,
    min_events_per_user INT,
    candidate_limit INT,
    content_weight DOUBLE PRECISION,
    collaborative_weight DOUBLE PRECISION,
    popularity_weight DOUBLE PRECISION,
    fallback_algorithm VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE IF EXISTS recommendation_strategies
    ADD COLUMN IF NOT EXISTS content_weight DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS collaborative_weight DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS popularity_weight DOUBLE PRECISION;

CREATE TABLE IF NOT EXISTS recommender_config (
    id BIGINT PRIMARY KEY,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    training_period VARCHAR(32) NOT NULL DEFAULT 'WEEK',
    default_algorithm VARCHAR(40) NOT NULL DEFAULT 'HYBRID',
    default_strategy_id BIGINT,
    recommendation_limit INT NOT NULL DEFAULT 12,
    rebuild_batch_size INT NOT NULL DEFAULT 25,
    max_users_per_job INT NOT NULL DEFAULT 500,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS recommendation_rebuild_log (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP,
    processed_users INT,
    total_users INT,
    initiator VARCHAR(255),
    training_period VARCHAR(32),
    message TEXT
);

CREATE TABLE IF NOT EXISTS user_recommendation_preferences (
    user_id BIGINT PRIMARY KEY,
    boost_genres TEXT,
    mute_genres TEXT,
    freshness_bias DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    discovery_bias DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
