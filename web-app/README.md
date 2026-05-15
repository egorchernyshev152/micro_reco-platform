# microReco Web

Front-end приложении для рекомендательной платформы фильмов. Основа на React + Vite + TypeScript, тёмная тема с изумрудными акцентами, навигация как у Кинопоиска.

## Запуск

```bash
cd web-app
cp .env.example .env # при необходимости скорректируйте адресы сервисов
npm install
npm run dev
```

- `VITE_API_BASE_URL` — базовый URL `recommender-service` (по умолчанию `http://localhost:8083`).
- `VITE_DEFAULT_USER_ID` — ID пользователя для персональных подборок (временно задаётся вручную).

## Структура

- `src/api` — HTTP-клиенты (axios) для рекомендаций и поиска по каталогу.
- `src/components` — базовые UI-блоки (Layout, Sidebar, MovieCard, т.д.).
- `src/pages` — основные страницы: дашборд, детали фильма, профиль пользователя.
- `src/context` — Zustand store для выбранного пользователя.
- `src/styles` — общие стили и CSS-переменные.

## TODO

- Подключить реальную авторизацию (ID пользователя из JWT).
- Добавить управление списками (favorites/watch later).
- Вытянуть реальные жанры/теги из каталога для фильтров.
