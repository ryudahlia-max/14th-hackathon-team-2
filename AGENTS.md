# Backend Collaboration Rules

## Goal

Build a Spring Boot backend for a private wellness routine and social communication service.
Two backend developers work in the same repository with strict ownership boundaries.

## Developer A ownership

Developer A owns:

- Gradle, Spring Boot and shared configuration
- common responses, errors and security
- Supabase Auth/JWT, Database, Storage, Realtime, RLS and Cron
- environment variables, secrets, Docker, CI/CD and deployment
- profiles, friendships, groups, routines, completions, calendar and feed
- production adapters for ports declared by the engagement module
- applying and reviewing all production migrations

Developer A may edit:

- `backend/src/main/java/com/team2/wellness/common/**`
- `backend/src/main/java/com/team2/wellness/core/**`
- `backend/src/main/java/com/team2/wellness/infrastructure/**`
- `backend/src/main/resources/db/migration/core/**`
- backend build, configuration, deployment and Supabase files

## Developer B ownership

Developer B owns only:

- `backend/src/main/java/com/team2/wellness/engagement/**`
- `backend/src/test/java/com/team2/wellness/engagement/**`
- `backend/src/main/resources/db/migration/engagement/**`

Developer B implements chat rooms, messages, reactions, AI image generation workflow,
in-app notifications and monthly recap generation. The detailed task and acceptance criteria
are in `docs/DEVELOPER_B_TASKS.md`.

## Hard restrictions for Developer B

Developer B must not:

- access Supabase CLI, MCP, dashboard, production database or service-role keys
- implement Supabase Storage, Realtime or RLS policies
- edit Docker, CI/CD, Render or production environment configuration
- edit Gradle files without Developer A approval
- edit packages owned by Developer A
- import Core services, entities or repositories directly
- query Core tables or add foreign keys to Core-owned tables
- validate Supabase JWTs directly
- commit API keys, tokens, passwords or real user images

When an external capability is needed, Developer B defines an outbound port under
`engagement.port.out` and tests it with a fake. Developer A supplies the production adapter.

## Database boundaries

- Core migration versions: `V001` through `V099`
- Engagement migration versions: `V100` through `V199`
- Cross-module IDs are opaque UUID columns without database foreign keys
- Developer B writes engagement migration SQL but never applies it to Supabase
- Every exposed production table must have RLS enabled by Developer A

## Integration rules

- Integration happens through immutable DTOs and port interfaces only
- Persist domain state before attempting Realtime or push delivery
- External delivery failure must not roll back successfully persisted business state
- API commands that can be retried must be idempotent
- All tests must run without Supabase or production credentials

## Required checks

Before committing backend changes run:

```bash
cd backend
./gradlew test
./gradlew check
```

Do not weaken or delete tests to make a build pass.
