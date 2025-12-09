CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS items (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS ratings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    score INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_rating_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT chk_rating_score CHECK (score >= 1 AND score <= 5),
    CONSTRAINT uq_rating_user_item UNIQUE (user_id, item_id)
);
