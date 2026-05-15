# Docker Runbook

This project ships a full local Docker environment:

- `postgres`: PostgreSQL 16 with three databases created by `db-init/init.sql`.
- `event-service`: Spring Boot service on port `8082`.
- `catalog-service`: Spring Boot service on port `8081`.
- `recommender-service`: Spring Boot service on port `8083`.
- `web-app`: nginx-served React app on port `5173`, with API reverse proxies.

## First Start

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

The UI is available at:

```text
http://localhost:5173
```

Direct service URLs for local debugging:

```text
catalog-service:      http://localhost:8081/swagger-ui.html
event-service:        http://localhost:8082/swagger-ui.html
recommender-service:  http://localhost:8083/swagger-ui.html
postgres:             localhost:5433
```

The frontend talks to backend services through nginx:

```text
/api/catalog/*      -> catalog-service:8081/*
/api/recommender/*  -> recommender-service:8083/*
```

## Health Checks

```powershell
docker compose ps
Invoke-WebRequest http://localhost:5173/health
Invoke-WebRequest http://localhost:8081/actuator/health
Invoke-WebRequest http://localhost:8082/actuator/health
Invoke-WebRequest http://localhost:8083/actuator/health
```

`depends_on` waits for PostgreSQL and backend services to become healthy before starting dependents.

## Logs

```powershell
docker compose logs -f web-app
docker compose logs -f catalog-service event-service recommender-service
docker compose logs -f postgres
```

## Stop and Reset

Stop containers but keep database data:

```powershell
docker compose down
```

Drop containers and volumes, including PostgreSQL data and uploaded catalog assets:

```powershell
docker compose down -v
```

## Environment

Runtime settings live in `.env`. The committed `.env.example` contains safe local defaults.

Important variables:

```text
POSTGRES_USER
POSTGRES_PASSWORD
JWT_SECRET
TMDB_API_KEY
WEB_PORT
CATALOG_SERVICE_PORT
EVENT_SERVICE_PORT
RECOMMENDER_SERVICE_PORT
```

Set `TMDB_API_KEY` before using TMDb import endpoints.
