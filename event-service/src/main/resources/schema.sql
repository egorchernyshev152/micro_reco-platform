CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_event_type CHECK (type in ('VIEW','LIKE','SAVE'))
);
