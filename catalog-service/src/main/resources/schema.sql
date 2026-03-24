CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

ALTER TABLE IF EXISTS users
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS role VARCHAR(32) DEFAULT 'USER',
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE users SET password_hash = crypt('demo123', gen_salt('bf')) WHERE password_hash IS NULL;
UPDATE users SET role = 'USER' WHERE role IS NULL;
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
    status VARCHAR(100),
    poster_url VARCHAR(1024),
    backdrop_url VARCHAR(1024),
    trailer_url VARCHAR(1024),
    budget BIGINT,
    revenue BIGINT,
    average_rating NUMERIC(4, 2),
    ratings_count BIGINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

ALTER TABLE IF EXISTS movies
    ADD COLUMN IF NOT EXISTS tmdb_id BIGINT,
    ADD COLUMN IF NOT EXISTS kinopoisk_id BIGINT,
    ADD COLUMN IF NOT EXISTS original_language VARCHAR(20);

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

CREATE TABLE IF NOT EXISTS user_movie_collections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    movie_id BIGINT NOT NULL REFERENCES movies (id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, movie_id, type)
);

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
