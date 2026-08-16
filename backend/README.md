# Wellness Backend

Spring Boot backend for the private wellness routine service.

## Requirements

- Java 21
- PostgreSQL 15 or newer, or Supabase local development

## Run locally

```bash
cp .env.example .env
set -a
source .env
set +a
./gradlew bootRun
```

Health check: `GET http://localhost:8080/actuator/health`

## Test

```bash
./gradlew test
./gradlew check
```

## Supabase integration

Production database credentials, JWT settings, Storage, Realtime, RLS and Cron are owned by
Developer A. Application tables are not intended for direct browser Data API access. The frontend
uses Supabase Auth, signed Storage operations and authorized Realtime channels only.

Never commit `.env`, service/secret keys, database passwords or real user media.
