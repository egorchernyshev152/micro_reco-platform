CREATE TABLE IF NOT EXISTS user_actor_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    actor_tmdb_id BIGINT NOT NULL,
    actor_name VARCHAR(255) NOT NULL,
    profile_url VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_actor_favorite UNIQUE (user_id, actor_tmdb_id)
);
