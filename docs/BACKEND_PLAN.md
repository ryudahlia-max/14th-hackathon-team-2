# Backend Delivery Plan

## Target

Deliver the first usable Spring Boot backend in one hackathon week. The web client authenticates
with Supabase Auth and sends the access token to the backend. The backend is the only component
that performs business writes to PostgreSQL. Private media is stored in Supabase Storage and
short-lived access is granted through signed operations.

## Parallel ownership

| Area | Developer A | Developer B |
| --- | --- | --- |
| Foundation | Spring Boot, security, errors, CI, Docker | No changes |
| Core product | Profile, friends, groups, routines, completions, calendar, feed | No changes |
| Engagement | Production adapters after ports are handed off | Chat, reactions, AI jobs, notifications, recap |
| Supabase | Auth/JWT, DB, Storage, Realtime, RLS, Cron | No account or SDK access |
| Deployment | Render configuration, secrets, release | No access |
| Migrations | `V001`-`V099`, review/apply every migration | Author `V100`-`V199`, never apply remotely |

The enforceable path and dependency rules are in the root `AGENTS.md`. Developer B receives
`docs/DEVELOPER_B_TASKS.md` as the complete assignment.

## Architecture contract

```text
Web client
  |  Supabase access token
  v
Spring Boot API
  +-- common: JWT verification, errors, API conventions
  +-- core: profile/friend/group/routine/calendar/feed
  +-- engagement: chat/AI/notification/recap
  +-- infrastructure: Developer A's external adapters
        |-- PostgreSQL / Supabase
        |-- private Storage
        |-- Realtime
        |-- OpenAI and push providers
```

Core and Engagement do not import each other's services, entities or repositories. Engagement
declares small outbound interfaces under `engagement.port.out`; Developer A connects those ports
to Core or infrastructure after Developer B's handoff. Cross-module identifiers are UUID values,
not database foreign keys.

## One-week execution

### Day 1: Freeze contracts

- Developer A: build/security/error foundation, Core schema, environment and CI.
- Developer B: domain model, outbound port interfaces and engagement migration draft.
- Shared checkpoint: agree only on immutable port DTOs and endpoint payloads.

### Days 2-3: Independent feature development

- Developer A: profile, friendships, groups, routines, completions, calendar and feed.
- Developer B: rooms, messages, reactions and AI job state machine using test fakes.
- No cross-module implementation dependency is allowed.

### Day 4: Finish workflows

- Developer A: Supabase Storage/Realtime configuration and deploy manifest.
- Developer B: notifications, retry behavior and monthly recap workflow.
- Developer B submits `docs/ENGAGEMENT_HANDOFF.md` with ports, topics, paths and payloads.

### Day 5: Adapter integration

- Developer A implements the outbound-port adapters without changing Engagement domain logic.
- Developer A reviews `V100`-`V199`, enables RLS and applies migrations to the selected project.
- Both developers run the same Gradle checks locally and in CI.

### Days 6-7: End-to-end hardening

- Verify authorization failures, idempotency, cursor pagination and external-delivery failure paths.
- Exercise signup-to-routine, friend/group, chat and AI flows with the web client.
- Deploy, smoke-test health and authenticated endpoints, then freeze the release candidate.

## Integration checkpoints

Developer A needs only the following artifacts from Developer B:

1. Compiling port interfaces and immutable DTOs.
2. Engagement SQL migrations in the reserved version range.
3. `docs/ENGAGEMENT_HANDOFF.md` containing environment, Storage and Realtime requirements.
4. Passing tests that use fakes instead of Supabase, OpenAI or push credentials.

Developer B does not need Developer A's database, credentials, running server or implementation to
finish any assigned task.

## Release gates

- `./gradlew check` and `./gradlew bootJar` pass on Java 21.
- Every authenticated endpoint verifies a Supabase JWT and derives the user ID from it.
- Every retriable command is idempotent.
- External delivery failure never erases persisted domain state.
- Browser roles cannot directly mutate backend-owned tables.
- Production tables have reviewed RLS and required indexes.
- No `.env`, database password, service key, API key or real user media is committed.
