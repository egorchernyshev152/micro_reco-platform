CREATE TABLE IF NOT EXISTS recommendation_strategies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    algorithm VARCHAR(50) NOT NULL,
    event_weights JSONB,
    time_decay_half_life_days INT,
    min_events_per_user INT,
    candidate_limit INT,
    fallback_algorithm VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
