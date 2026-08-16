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

The local `.env` file is ignored by Git. Keep `OPENAI_API_KEY`, `SUPABASE_SECRET_KEY` and database
credentials there only. The AI worker polls queued jobs automatically; set `AI_WORKER_ENABLED=false`
when running an API-only instance.

## Test

```bash
./gradlew test
./gradlew check
```

## Supabase integration

Production database credentials, JWT settings, Storage, Realtime, RLS and Cron are owned by
Developer A. Application tables are not intended for direct browser Data API access. The frontend
uses Supabase Auth, signed Storage operations and authorized Realtime channels only.

Use a current server-side `sb_secret_...` key for `SUPABASE_SECRET_KEY`. The backend publishes to
private Realtime topics and creates short-lived URLs for objects in the private `avatars`,
`chat-media` and `ai-results` buckets. Clients must join Realtime channels with `private: true`.

## Engagement integration

- `SecurityContextCurrentUserAdapter` derives the user UUID from the verified JWT subject.
- `CoreAccessAdapter` supplies friendship, group, profile consent and missed-routine checks.
- Supabase adapters handle private Storage and Realtime REST operations.
- `OpenAiImageAdapter` uses image edits for face-reference workflows and image generations for
  monthly recap art.
- Push remains in-app only unless `PUSH_WEBHOOK_URL` is configured.
- Monthly recaps run at 00:05 Asia/Seoul on the first day of each month by default.

Never commit `.env`, service/secret keys, database passwords or real user media.
