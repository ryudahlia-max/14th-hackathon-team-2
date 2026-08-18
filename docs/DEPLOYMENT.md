# Backend Deployment

## Supabase project

Use a dedicated project for this hackathon. Do not link the repository to an unrelated existing
project. The backend expects PostgreSQL 15 or newer and supports the current Supabase
`sb_publishable_...` and `sb_secret_...` key model.

Required server-only values:

- pooled JDBC URL, database user and password
- project URL
- project secret API key (`sb_secret_...`)
- Auth issuer and JWKS URL

Required public web-client values are the project URL and publishable key only. Never put the
secret key in a browser environment variable.

After choosing the project:

1. Link it with `supabase link --project-ref <ref>` and enter the database password interactively.
2. Apply Auth and private bucket configuration with `supabase config push`.
3. Configure the backend environment from `backend/.env.example`.
4. Start one backend instance. Flyway applies Core, Engagement, indexes, RLS and Realtime policies.
5. Confirm `/actuator/health`, Flyway history and the Supabase Security Advisor.

The configured private buckets are `avatars`, `routine-proofs`, `chat-media`, `ai-results` and
`recap-cards`. Backend-owned application tables revoke access from `anon` and `authenticated`;
the browser reaches business data through the Spring API.

## Render

Create a Blueprint from the repository root using `render.yaml`. Set every `sync: false` value in
the Render dashboard. Use Supabase's transaction pooler JDBC endpoint and keep the Hikari pool at
10 connections or fewer for the initial release.

Set `AI_WORKER_ENABLED=true` on only one instance unless worker concurrency has been load-tested.
The queue uses pessimistic row locking, and external OpenAI calls occur after the claim transaction
has committed. Set `PUSH_WEBHOOK_URL` only when a trusted FCM/APNs gateway is available; otherwise
notifications remain in-app.

## Smoke checks

- Health returns HTTP 200.
- An application endpoint without a JWT returns HTTP 401.
- A valid Supabase user token can bootstrap a profile.
- `anon` and `authenticated` cannot select backend-owned tables through the Data API.
- A private chat topic rejects non-members and accepts current members.
- An AI request progresses from `QUEUED` to a terminal status without exposing media publicly.
