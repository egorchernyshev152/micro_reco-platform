# Project Overview

Подробное текстовое описание системы для обсуждений, отчетов и схем.

## Общая архитектура
- Три микросервиса на Spring Boot 3 (Java 17): `catalog-service`, `event-service`, `recommender-service`.
- Единственная БД PostgreSQL (docker-compose публикует на `localhost:5433`, init скрипт `db-init/init.sql` создаёт базы/схемы для сервисов).
- Взаимодействие между сервисами только синхронное по HTTP/1.1, JSON (REST). Очередей, стриминга, вебхуков нет. Инициатор запросов всегда `recommender-service`.
- Конфиги адресов downstream и таймаутов — `recommender-service/src/main/resources/application.yml` (`clients.catalog.base-url`, `clients.event.base-url`, `clients.http.*`).

## Сервисы и их ответственность
### catalog-service (порт 8081)
- Полноценный каталог кинофильмов: карточки, справочники жанров/стран/тегов, пользовательские рейтинги.
- REST эндпоинты (основные):
  - `/movies` — поиск с фильтрами (жанры, страны, годы, рейтинги).
  - `/movies/lookup?ids=1,2,3` — карточки по списку id (используется рекомендатором).
  - `/movies/all` — тех. выдача всех фильмов (для контентных алгоритмов).
  - `/movies/{id}/ratings` — работа с пользовательскими оценками.
  - `/users`, `/ratings` — управление пользователями и их отзывами.
- Сервис выступает источником атрибутов фильмов для рекомендаций и отчетов.
- Загрузка контента: `POST /internal/import/tmdb` — защищенный (внутренний) эндпоинт, который запускает импорт фильмов из TMDb. Требует `TMDB_API_KEY`. Параметры позволяют ограничить количество страниц, страну производства (`originCountry`), язык локализации (`language`/`originalLanguage`), пороги рейтингов (`minVoteAverage`, `minVoteCount`). Сервис подтягивает детали фильма, мапит на таблицы `movies`, `movie_genres`, `movie_countries`, `movie_tags`, автоматически подставляет ссылки на постеры/бекдропы (`https://image.tmdb.org/t/p/<size>/<path>`).

### event-service (порт 8082)
- Централизованное хранилище событий пользователя по фильмам (`VIEW_CARD`, `WATCH_TRAILER`, `RATE`, `START/FINISH_WATCHING`, `FAVORITE`, `BOOKMARK`, `SHARE` и т.д.).
- REST:
  - `/events` — запись и выборка событий. Фильтры: `userId`, `movieId`, `type`, `period`, `source`, `sessionId`, `limit`. Поддерживается batch-интерфейс `/events/bulk`.
  - `/events/stats/by-movie` — популярность фильмов (для отчётов и алгоритмов).
  - `/events/stats/by-user` — активность пользователей.
  - `/events/stats/by-day`, `/events/stats/time-distribution` — агрегаты по дням и часам.
- Источник данных для рекомендаций, трендов и отчетов.

- `/api/v1/movies/popular` — популярные фильмы за период (`limit`, `period`).
- `/api/v1/movies/trending` — тренды (выше вес свежих событий).
- `/api/v1/movies/user/{userId}` — персональные рекомендации (параметры `limit`, `period`, `algo`, `strategyId`).
- `/api/v1/movies/similar/{movieId}` — похожие фильмы по контенту.
- `/reports/top-movies` — DOCX отчёт по топ-фильмам.
- Внутренние HTTP клиенты:
  - `EventClient` → event-service: `GET /events`, `GET /events/stats/by-movie`.
  - `CatalogClient` → catalog-service: `GET /movies`, `/movies/lookup`, `/movies/all`.
- WebClient конфиг: `recommender-service/src/main/java/com/example/recommender/config/WebClientConfig.java` — таймауты, лог исходящих запросов, превращение любого 4xx/5xx в `WebClientResponseException`. Вызовы блокирующие (`block()`).

## Потоки данных (основные сценарии)
1) **Популярные/трендовые фильмы**
   - `EventClient.getEvents(null, period)` → событийный поток для взвешивания (время затухания, веса типов).
   - `EventClient.getStatsByMovie(period)` → агрегаты по фильмам (fallback/гистограммы).
   - `PopularityAlgorithm` или "Trending" стратегия формирует скоринг и подтягивает карточки через `CatalogClient.getMoviesByIds`.

2) **Персональные рекомендации**
   - `EventClient.getEvents(userId, period)` → история пользователя; `getEvents(null, period)` → общий поток.
   - Собирается множество просмотренных фильмов, выбирается стратегия/алгоритм (co-occurrence, content-based, hybrid).
   - `CatalogClient.getMoviesByIds` или `getAllMovies` (для контентного подхода) возвращают карточки.
   - При пустом ответе fallback → популярность.

3) **Похожие фильмы и отчёты**
   - Контентная матрица строится по метаданным (жанры/страны/теги) из `CatalogClient.getAllMovies`.
   - `/reports/top-movies` использует `EventClient.getStatsByMovie` + `CatalogClient.getMoviesByIds`, формируя DOCX.

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
- Контроллеры: `recommender-service/src/main/java/com/example/recommender/controller/*`, `event-service/src/main/java/com/example/event/controller/EventController.java`, `catalog-service/src/main/java/com/example/catalog/controller/MovieController.java`.
