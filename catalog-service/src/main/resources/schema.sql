CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    profile_private BOOLEAN NOT NULL DEFAULT FALSE,
    role_changed_at TIMESTAMP,
    role_changed_by VARCHAR(255),
    blocked_changed_at TIMESTAMP,
    blocked_changed_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS role VARCHAR(32) DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS blocked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS profile_private BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS role_changed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS role_changed_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS blocked_changed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS blocked_changed_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE users SET password_hash = crypt('demo123', gen_salt('bf')) WHERE password_hash IS NULL;
UPDATE users SET role = 'USER' WHERE role IS NULL;
UPDATE users SET blocked = FALSE WHERE blocked IS NULL;
UPDATE users SET profile_private = FALSE WHERE profile_private IS NULL;
UPDATE users SET updated_at = NOW() WHERE updated_at IS NULL;

ALTER TABLE IF EXISTS users
    ALTER COLUMN password_hash SET NOT NULL,
    ALTER COLUMN role SET NOT NULL;

CREATE TABLE IF NOT EXISTS movies (
    id BIGSERIAL PRIMARY KEY,
    tmdb_id BIGINT,
    kinopoisk_id BIGINT,
    title VARCHAR(255) NOT NULL,
    original_title VARCHAR(255),
    original_language VARCHAR(20),
    description TEXT,
    synopsis TEXT,
    release_year INTEGER,
    release_date DATE,
    duration_minutes INTEGER,
    age_rating VARCHAR(10),
    tagline VARCHAR(255),
    status VARCHAR(100) NOT NULL DEFAULT 'DRAFT',
    poster_url VARCHAR(1024),
    backdrop_url VARCHAR(1024),
    trailer_url VARCHAR(1024),
    budget BIGINT,
    revenue BIGINT,
    average_rating NUMERIC(4, 2),
    ratings_count BIGINT DEFAULT 0,
    imported_rating NUMERIC(4, 2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

ALTER TABLE IF EXISTS movies
    ADD COLUMN IF NOT EXISTS tmdb_id BIGINT,
    ADD COLUMN IF NOT EXISTS kinopoisk_id BIGINT,
    ADD COLUMN IF NOT EXISTS original_language VARCHAR(20),
    ADD COLUMN IF NOT EXISTS imported_rating NUMERIC(4, 2);

CREATE UNIQUE INDEX IF NOT EXISTS idx_movies_tmdb_id ON movies (tmdb_id) WHERE tmdb_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_movies_kinopoisk_id ON movies (kinopoisk_id) WHERE kinopoisk_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS movie_genres (
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    genre VARCHAR(100) NOT NULL,
    PRIMARY KEY (movie_id, genre)
);

CREATE TABLE IF NOT EXISTS movie_countries (
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    country VARCHAR(100) NOT NULL,
    PRIMARY KEY (movie_id, country)
);

CREATE TABLE IF NOT EXISTS movie_tags (
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (movie_id, tag)
);

CREATE TABLE IF NOT EXISTS movie_cast (
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    order_index INTEGER NOT NULL,
    person_tmdb_id BIGINT,
    name VARCHAR(255) NOT NULL,
    character VARCHAR(255),
    profile_url VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (movie_id, order_index)
);

CREATE TABLE IF NOT EXISTS movie_assets (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    asset_type VARCHAR(40) NOT NULL,
    file_name VARCHAR(255),
    content_type VARCHAR(150),
    file_size BIGINT,
    object_key VARCHAR(512) NOT NULL,
    public_url VARCHAR(2048) NOT NULL,
    storage VARCHAR(40) NOT NULL,
    label VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_movie_assets_movie ON movie_assets (movie_id);

CREATE TABLE IF NOT EXISTS user_movie_collections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, movie_id, type)
);

CREATE TABLE IF NOT EXISTS user_actor_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    actor_tmdb_id BIGINT NOT NULL,
    actor_name VARCHAR(255) NOT NULL,
    profile_url VARCHAR(1024),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, actor_tmdb_id)
);

CREATE TABLE IF NOT EXISTS user_follows (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_follow UNIQUE (follower_id, target_id),
    CONSTRAINT chk_user_follow_self CHECK (follower_id <> target_id)
);

CREATE INDEX IF NOT EXISTS idx_user_follows_follower ON user_follows (follower_id);
CREATE INDEX IF NOT EXISTS idx_user_follows_target ON user_follows (target_id);

CREATE TABLE IF NOT EXISTS movie_ratings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    movie_id BIGINT NOT NULL,
    score INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_rating_movie FOREIGN KEY (movie_id) REFERENCES movies (id),
    CONSTRAINT chk_rating_score CHECK (score >= 1 AND score <= 10),
    CONSTRAINT uq_rating_user_movie UNIQUE (user_id, movie_id)
);

CREATE TABLE IF NOT EXISTS movie_reviews (
    id BIGSERIAL PRIMARY KEY,
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    score INTEGER NOT NULL CHECK (score BETWEEN 1 AND 10),
    content VARCHAR(4000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    flagged_at TIMESTAMP,
    last_reason VARCHAR(512),
    moderated_by VARCHAR(255),
    moderated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_movie_reviews_movie ON movie_reviews (movie_id);
CREATE INDEX IF NOT EXISTS idx_movie_reviews_author ON movie_reviews (author_id);
CREATE INDEX IF NOT EXISTS idx_movie_reviews_status ON movie_reviews (status);
CREATE INDEX IF NOT EXISTS idx_movie_reviews_flagged ON movie_reviews (flagged) WHERE flagged = TRUE;

CREATE TABLE IF NOT EXISTS review_moderation_logs (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES movie_reviews (id) ON DELETE CASCADE,
    action VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(512),
    performed_by_id BIGINT,
    performed_by_email VARCHAR(255),
    performed_by_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_review_moderation_review ON review_moderation_logs (review_id);

CREATE TABLE IF NOT EXISTS user_complaints (
    id BIGSERIAL PRIMARY KEY,
    target_user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    reporter_user_id BIGINT REFERENCES users (id) ON DELETE SET NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_complaints_target ON user_complaints (target_user_id);

CREATE TABLE IF NOT EXISTS user_audit_log (
    id BIGSERIAL PRIMARY KEY,
    target_user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    action VARCHAR(64) NOT NULL,
    details TEXT,
    performed_by_id BIGINT,
    performed_by_email VARCHAR(255),
    performed_by_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_audit_target ON user_audit_log (target_user_id);
