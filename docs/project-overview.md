# Project Overview

Подробное текстовое описание системы для обсуждений, отчетов и схем.

## Общая архитектура
- Три микросервиса на Spring Boot 3 (Java 17): `catalog-service`, `event-service`, `recommender-service`.
- Единственная БД PostgreSQL (docker-compose публикует на `localhost:5433`, init скрипт `db-init/init.sql` создаёт базы/схемы для сервисов).
- Взаимодействие между сервисами только синхронное по HTTP/1.1, JSON (REST). Очередей, стриминга, вебхуков нет. Инициатор запросов всегда `recommender-service`.
- Конфиги адресов downstream и таймаутов — `recommender-service/src/main/resources/application.yml` (`clients.catalog.base-url`, `clients.event.base-url`, `clients.http.*`).

## Сервисы и их ответственность
### catalog-service (порт 8081)
- CRUD по товарам, пользователям, рейтингам.
- REST эндпоинты (основные):
  - `/items` — CRUD и получение всех товаров.
  - `/items/search?ids=1,2,3` — карточки по списку id.
  - `/items/by-categories?categories=cat1,cat2` — товары по категориям.
  - Аналогично `/users`, `/ratings`.
- Используется рекомендатором для обогащения выдачи карточками товаров.

### event-service (порт 8082)
- Хранит события пользователя по товарам (VIEW/LIKE/SAVE и т.п.).
- REST:
  - `/events` — CRUD, фильтры `userId` и `period=DAY|WEEK|MONTH`.
  - `/events/stats/by-item` — агрегированная популярность по товарам.
  - `/events/stats/by-day` — агрегаты по дням.
- Источник данных для рекомендаций и отчётов.

### recommender-service (порт 8083)
- API для рекомендаций и отчётов.
- REST:
  - `/api/v1/recommendations/popular` — популярное (параметры `period`, `limit`).
  - `/api/v1/recommendations/user/{userId}` — персональные (параметры `limit`, `period`, `algo`, `strategyId`).
  - `/reports/top-items` — DOCX отчёт по топ-товарам за период.
- Внутренние HTTP клиенты:
  - `EventClient` → event-service: `GET /events`, `GET /events/stats/by-item`.
  - `CatalogClient` → catalog-service: `GET /items/search`, `GET /items`, `GET /items/by-categories`.
- WebClient конфиг: `recommender-service/src/main/java/com/example/recommender/config/WebClientConfig.java` — таймауты, лог исходящих запросов, превращение любого 4xx/5xx в `WebClientResponseException`. Вызовы блокирующие (`block()`).

## Потоки данных (основные сценарии)
1) **Популярные рекомендации**
   - `EventClient.getEvents(null, period)` → все события.
   - `RecommendationService` готовит контекст; `PopularityAlgorithm` считает вес событий с временным затуханием.
   - Список itemId → `CatalogClient.getItemsByIds` → обогащение карточками → ответ клиенту.

2) **Персональные рекомендации**
   - `EventClient.getEvents(userId, period)` → история пользователя; `getEvents(null, period)` → общий поток.
   - Собирается множество просмотренных; выбирается стратегия/алгоритм.
   - `CooccurrenceAlgorithm` строит матрицу совместных действий, исключает уже виденное, считает кандидатов.
   - `CatalogClient.getItemsByIds` подтягивает карточки кандидатов; при пустом результате — fallback (обычно популярность).

3) **Отчёт по топ-товарам**
   - `EventClient.getStatsByItem(period)` → популярность.
   - `CatalogClient.getItemsByIds` → заголовки товаров.
   - `ReportService` собирает DOCX и отдаёт файл.

## Поведение по ошибкам и таймаутам
- Таймауты соединения/чтения/ответа задаются в `clients.http.*`.
- Любой ответ 4xx/5xx downstream выбрасывает `WebClientResponseException` с телом ответа.
- Нет ретраев, circuit breaker, очередей: доступность ответов напрямую зависит от downstream.

## Примечания по протоколам
- Сейчас HTTP/1.1. HTTP/2 можно включить без смены контрактов, если понадобится мультиплексирование/меньше накладных расходов (требует H2+TLS и поддержки на клиенте/сервере).
- gRPC имеет смысл обсуждать при очень высоком RPS, строгих SLO по задержкам или необходимости стриминга; иначе REST достаточен, лучше инвестировать в ретраи, circuit breaker, метрики/трейсинг.

## Где смотреть код
- Конфиги: `recommender-service/src/main/resources/application.yml`.
- WebClient конфиг: `recommender-service/src/main/java/com/example/recommender/config/WebClientConfig.java`.
- HTTP клиенты: `recommender-service/src/main/java/com/example/recommender/client/EventClient.java`, `.../CatalogClient.java`.
- Алгоритмы: `recommender-service/src/main/java/com/example/recommender/service/algorithm/*`.
- Контроллеры: `recommender-service/src/main/java/com/example/recommender/controller/*`, `event-service/src/main/java/com/example/event/controller/EventController.java`, `catalog-service/src/main/java/com/example/catalog/controller/ItemController.java`.
