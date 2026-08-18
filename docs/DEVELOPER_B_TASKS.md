# Developer B Task Brief: Engagement Module

## Objective

Implement the engagement feature set locally without any Supabase account, production database,
deployment access or secrets. All code must remain inside the owned engagement paths described in
the root `AGENTS.md`.

## Module structure

Use this package structure:

```text
com.team2.wellness.engagement
├── chat
│   ├── api
│   ├── application
│   ├── domain
│   └── persistence
├── ai
│   ├── api
│   ├── application
│   ├── domain
│   └── persistence
├── notification
├── recap
└── port
    ├── in
    └── out
```

## Required outbound ports

Define small interfaces and immutable DTOs. Do not expose JPA entities through ports.

### `CurrentUserPort`

- Return the authenticated user UUID.
- Tests use a fake; Developer A implements the Supabase JWT adapter.

### `CoreAccessPort`

- `areAcceptedFriends(userId, targetUserId)`
- `isGroupMember(userId, groupId)`
- `getGroupMemberIds(groupId)`
- `getMissedRoutineOccurrence(occurrenceId, targetUserId)`
- `getUserSummary(userId)`

### `MediaStoragePort`

- Read a target user's registered face asset
- Store an AI output asset
- Resolve or store chat media
- Return a temporary download URL

### `RealtimePublisherPort`

- Publish a serializable payload using `topic` and `eventType`
- Tests must use a recording fake

### `PushNotificationPort`

- Send a push notification command
- Notification persistence must remain successful when push delivery fails

## Task B-1: Chat and reactions

Estimated effort: 1.5 days.

Implement:

- direct and group chat room creation
- room membership and authorization
- text, image, routine card, AI image and system messages
- cursor-based message pagination
- idempotency using client message IDs
- encouragement reactions
- JPA mappings, repositories and migrations starting at `V100`

Rules:

- Direct chat is allowed only between accepted friends
- Group chat is allowed only for current group members
- A normalized user pair has only one direct room
- A group ID has only one group room
- Persist a message before publishing a Realtime event
- A Realtime failure must not roll back the message

Required endpoints:

- `POST /api/v1/engagement/chat-rooms`
- `GET /api/v1/engagement/chat-rooms`
- `GET /api/v1/engagement/chat-rooms/{roomId}/messages`
- `POST /api/v1/engagement/chat-rooms/{roomId}/messages`
- `POST /api/v1/engagement/messages/{messageId}/reactions`
- `DELETE /api/v1/engagement/messages/{messageId}/reactions/{type}`

## Task B-2: AI future image workflow

Estimated effort: 1.5 days.

Implement job states `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `BLOCKED`.

Flow:

1. Validate relationship, target consent and missed routine through ports
2. Enforce three requests per requester per day
3. Persist a queued job and return HTTP 202
4. Claim a job without holding a transaction during the OpenAI request
5. Load the face asset through `MediaStoragePort`
6. Call the OpenAI Image API using a server-owned prompt that includes the selected routine title/category,
   missed occurrence count over the last 366 scheduled days, and most recent missed date
7. Store the output through `MediaStoragePort`
8. Create an `AI_IMAGE` chat message and notification
9. Publish job and message events

Retry only HTTP 429 and 5xx failures up to three attempts with exponential backoff.
Do not retry validation errors or moderation blocks. Never accept a raw prompt from clients.

Required endpoints:

- `POST /api/v1/engagement/ai-generations`
- `GET /api/v1/engagement/ai-generations/{jobId}`

## Task B-3: Notifications and recap

Estimated effort: 1 day.

Implement:

- in-app notification persistence and cursor pagination
- single and bulk read operations
- chat, reaction, AI completion and reminder notification commands
- monthly recap creation from an immutable statistics DTO supplied by Core
- one recap per group and month
- positive Korean summary and recap image generation workflow

Required endpoints:

- `GET /api/v1/engagement/notifications`
- `PATCH /api/v1/engagement/notifications/{id}/read`
- `PATCH /api/v1/engagement/notifications/read-all`

Developer B does not calculate routine statistics or schedule Cron jobs.

## Owned database tables

- `chat_rooms`
- `chat_room_members`
- `chat_messages`
- `message_reactions`
- `ai_generation_jobs`
- `notifications`
- `monthly_recaps`

User, group, routine and occurrence IDs are opaque UUID values without foreign keys to Core tables.

Required indexes:

- `chat_messages(room_id, created_at desc, id desc)`
- `chat_room_members(user_id, room_id)`
- `ai_generation_jobs(status, next_attempt_at)`
- `notifications(user_id, created_at desc, id desc)`

## Required tests

- Non-friends cannot create a direct room
- Non-members cannot access a group room
- Duplicate direct rooms are not created
- Duplicate client message IDs return the existing message
- Unauthorized users cannot read or send messages
- Realtime failure does not remove a persisted message
- Completed routines and missing consent block AI requests
- Daily AI rate limit is enforced
- Duplicate AI requests are idempotent
- 429 and 5xx failures retry; moderation blocks do not retry
- AI success creates a message and notification
- Push failure does not remove notification records
- Monthly recap creation is idempotent

Tests must not require Supabase, OpenAI or push credentials.

## Handoff deliverable

Add `docs/ENGAGEMENT_HANDOFF.md` containing:

- implemented API request/response examples
- outbound ports Developer A must implement
- required environment variables
- requested Storage buckets and object path rules
- Realtime topics and event payloads
- migrations and indexes
- known limitations and remaining TODOs

## Definition of done

- The engagement module compiles with the base project
- All required endpoints and migrations are included
- Unit and local persistence tests pass
- No Supabase SDK is imported
- No Core implementation class or table is accessed directly
- All external dependencies are represented by ports and test fakes
