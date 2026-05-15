# syntax=docker/dockerfile:1.7

FROM node:20-alpine AS build

WORKDIR /app

ARG VITE_API_BASE_URL=/api/recommender
ARG VITE_CATALOG_API_URL=/api/catalog

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_CATALOG_API_URL=${VITE_CATALOG_API_URL}

COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q --spider http://localhost/health || exit 1
