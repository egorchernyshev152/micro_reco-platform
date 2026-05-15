# Demo Data Seeding

Чтобы быстро проверить рекомендации и сценарии с похожими пользователями, можно заполнить базы готовыми наборами фильмов, коллекций и событий. Скрипты находятся в каталоге `demo-data`.

## Каталог фильмов (`catalog-db`)
Файл: `demo-data/catalog-demo-data.sql`

1. Убедитесь, что `catalog-service` остановлен.
2. Поднимите базу (`docker-compose up -d db`), если она ещё не запущена.
3. Выполните скрипт в БД `catalog-db` (по умолчанию `postgres/postgres`):
   ```bash
   psql -h localhost -p 5433 -U postgres -d catalog-db -f demo-data/catalog-demo-data.sql
   ```
   Скрипт перезапишет таблицы `users`, `movies` и связанные справочники и создаст:
   - 12 фильмов на русском языке с разными жанрами/странами.
   - Коллекции и рейтинги нескольких пользователей.
   - Аккаунт `newbie@example.com` (пароль `newbie123`) без событий и коллекций для проверки cold-start кейса.
   - Пользователь `docu@example.com` помечен как приватный профиль.

## События (`event-db`)
Файл: `demo-data/event-demo-data.sql`

1. Убедитесь, что `event-service` остановлен.
2. Выполните:
   ```bash
   psql -h localhost -p 5433 -U postgres -d event-db -f demo-data/event-demo-data.sql
   ```
   Скрипт очистит таблицу `events` и добавит десятки событий (VIEW_CARD, WATCH_TRAILER, START/FINISH_WATCHING, RATE и т.д.) для пользователей `2–5` и `7`, охватывая разные устройства и источники (`CATALOG`, `RECOMMENDER_HOME`, `SIMILAR_WIDGET`, `FRIENDS_SIMILAR_USERS`).

После загрузки данных можно запускать сервисы:

```bash
mvn -pl catalog-service spring-boot:run
mvn -pl event-service spring-boot:run
mvn -pl recommender-service spring-boot:run
```

Полезные запросы для проверки:

- Популярные фильмы: `GET http://localhost:8083/api/v1/movies/popular?period=MONTH`.
- Персональные рекомендации (например, для `demo@example.com`, ID=2): `GET http://localhost:8083/api/v1/movies/user/2?algo=HYBRID&period=MONTH`.
- Похожие пользователи: `GET http://localhost:8083/api/v1/users/2/similar?limit=4&minOverlap=2`.
- Публичный профиль пользователя: `GET http://localhost:8081/public/users/2`.
- Публичная коллекция: `GET http://localhost:8081/public/users/2/collections/FAVORITE`.

При необходимости импорт TMDb по-прежнему доступен: скрипты не отключают `POST /internal/import/tmdb`, их задача — дать готовый демо-набор без сетевых запросов.

## Генератор больших массивов событий
Когда фильмов стало много (например, после импорта TMDb), удобно нагенерировать новые события автоматически. В каталоге `scripts` лежит утилита на Java `EventSeedGenerator` (см. `scripts/src/main/java/com/example/scripts/EventSeedGenerator.java`):

1. Соберите и запустите:
   ```bash
   mvn -pl scripts exec:java -Dexec.mainClass=com.example.scripts.EventSeedGenerator \
       -Dexec.args="--catalog-dsn jdbc:postgresql://localhost:5433/catalog-db --events-dsn jdbc:postgresql://localhost:5433/event-db --users 50 --movies 1000 --seed 123"
   ```
2. Скрипт подключится к `catalog-db`, выберет пользователей/фильмы и вставит последовательности событий в `event-db`. Существующие записи не удаляются. Есть флаг `--dry-run` для проверки без инсёрта.
