# DB Diagram

```mermaid
erDiagram
    USERS ||--o{ RATINGS : rates
    ITEMS ||--o{ RATINGS : receives
    EVENTS {
        BIGSERIAL id PK
        BIGINT user_id
        BIGINT item_id
        VARCHAR type (VIEW|LIKE|SAVE)
        TIMESTAMP created_at
    }
    USERS {
        BIGSERIAL id PK
        VARCHAR name
        VARCHAR email UNIQUE
    }
    ITEMS {
        BIGSERIAL id PK
        VARCHAR title
        TEXT description
        VARCHAR category
    }
    RATINGS {
        BIGSERIAL id PK
        BIGINT user_id FK
        BIGINT item_id FK
        INT score CHECK 1..5
        TIMESTAMP created_at
    }
    USERS ||--o{ EVENTS : triggers
    ITEMS ||--o{ EVENTS : involved_in
```

- Каталог (users, items, ratings) хранится в `catalog-service`.
- `event-service` хранит `events` (user_id/item_id — ссылки по идентификаторам на каталожные сущности, без FK из-за раздельных БД).
