# Engagement 모듈 인수인계

## 범위

Developer B가 구현한 기능은 채팅/리액션, AI 미래 이미지 작업, 인앱 알림, 월간 리캡이다. 구현 코드는 `backend/src/main/java/com/team2/wellness/engagement` 아래에 있고, Core 테이블을 직접 조회하거나 Supabase SDK를 사용하지 않는다.

## Developer A 구현 포트

모든 어댑터는 `engagement.port.out` 인터페이스를 구현하고, JPA 엔티티가 아닌 포트 DTO만 반환해야 한다.

| 포트 | 메서드 | A 구현 책임 |
|---|---|---|
| `CurrentUserPort` | `currentUserId()` | Supabase JWT subject를 UUID로 변환 |
| `CoreAccessPort` | `areAcceptedFriends`, `isGroupMember`, `getGroupMemberIds` | 친구 수락 상태와 현재 그룹 멤버십 확인 |
| `CoreAccessPort` | `getMissedRoutineOccurrence`, `hasAiImageConsent`, `getUserSummary` | AI 요청용 미완료 occurrence/동의 확인 및 사용자 요약 제공 |
| `MediaStoragePort` | `findFaceAsset`, `read`, `storeAiOutput`, `temporaryDownloadUrl` | Supabase Storage 읽기/쓰기 및 서명 URL 발급 |
| `RealtimePublisherPort` | `publish(topic, eventType, payload)` | Supabase Realtime broadcast 발행 |
| `PushNotificationPort` | `send(PushCommand)` | FCM/APNs 등 push 발송. 예외를 caller로 전파해도 B 서비스가 기록을 보존한다. |
| `ImageGenerationPort` | `generate(ImageCommand)` | A의 `OpenAiImageAdapter`가 참조 이미지 편집과 일반 이미지 생성을 구분한다. |

위 포트의 A 어댑터는 `infrastructure` 패키지에 연결되었다. 외부 전달은 활성 DB 트랜잭션이
있으면 커밋 이후 실행되므로 저장된 메시지와 알림보다 먼저 전송되지 않는다.

## 환경 변수

```dotenv
# OpenAI Image API (server-only; client에 노출 금지)
OPENAI_API_KEY=
OPENAI_BASE_URL=https://api.openai.com

# 기존 Supabase 설정으로 Storage/Realtime 어댑터가 사용
SUPABASE_URL=
SUPABASE_SECRET_KEY=
```

현재 WebClient의 Spring property는 `app.openai.api-key`, `app.openai.base-url`이다. A는 application configuration에서 위 환경 변수에 매핑해야 한다. API key는 저장소, 로그, 클라이언트에 기록하면 안 된다.

## Storage 버킷과 경로

Private 버킷: `avatars`, `chat-media`, `ai-results`.

| 용도 | object key 규칙 |
|---|---|
| 등록 얼굴 참조 | `avatars/{userId}/current.png` 또는 프로필에 저장한 `avatars/...` 경로 |
| 채팅 이미지 | `chat-media/{ownerId}/{objectId}.{ext}` |
| AI 미래 이미지 | `ai-results/ai-generations/{targetUserId}/{objectId}.{ext}` |
| 월간 리캡 이미지 | `ai-results/ai-generations/{groupId}/{objectId}.{ext}` |

얼굴 및 AI 결과는 private 객체로 유지한다. API 응답에서 표시가 필요할 때에만
`temporaryDownloadUrl`로 기본 5분 만료의 서명 URL을 제공한다.

## Realtime 계약

| Topic | Event | Payload |
|---|---|---|
| `chat-room:{roomId}` | `message.created` | `MessageView`: `id`, `roomId`, `senderId`, `type`, `content`, `mediaUrl`, `createdAt` |
| `ai-generation:{jobId}` | `ai_generation.succeeded` | `JobView`: `id`, `status`, `attemptCount`, `outputObjectKey`, `failureCode` |

실시간 발행은 항상 DB 저장 후 시도한다. Realtime 실패는 저장된 메시지/작업/알림을 롤백하지 않는다.

## API 예시

### 채팅방 생성

```http
POST /api/v1/engagement/chat-rooms
Authorization: Bearer <jwt>
Content-Type: application/json

{ "type": "DIRECT", "targetUserId": "b7f8b458-0861-4b20-b2ad-b4cc66f47bfa" }
```

```json
{ "id": "5f2f5ef6-490e-4e50-8b50-c1240b29b8d2", "type": "DIRECT", "groupId": null }
```

그룹은 `{ "type": "GROUP", "groupId": "..." }`를 전달한다.

### 메시지 전송과 다음 페이지

```http
POST /api/v1/engagement/chat-rooms/{roomId}/messages
Content-Type: application/json

{ "clientMessageId": "mobile-01HXYZ", "type": "TEXT", "content": "오늘도 화이팅!" }
```

```http
GET /api/v1/engagement/chat-rooms/{roomId}/messages?cursorCreatedAt=2026-08-16T10:00:00Z&cursorId={messageId}&size=30
```

`clientMessageId`를 재전송하면 기존 메시지를 반환한다.

### AI 미래 이미지 요청

```http
POST /api/v1/engagement/ai-generations
Content-Type: application/json

{ "targetUserId": "...", "occurrenceId": "...", "clientRequestId": "ai-mobile-01HXYZ" }
```

응답은 `202 Accepted`이며 `id`, `status`, `attemptCount`, `outputObjectKey`, `failureCode`를 반환한다. 클라이언트 프롬프트 입력은 받지 않는다.

### 알림

```http
GET /api/v1/engagement/notifications?size=30
PATCH /api/v1/engagement/notifications/{id}/read
PATCH /api/v1/engagement/notifications/read-all
```

## Flyway 마이그레이션과 인덱스

| 파일 | 테이블/변경 | 주요 인덱스·제약 |
|---|---|---|
| `V100__create_chat_schema.sql` | `chat_rooms`, `chat_room_members`, `chat_messages`, `message_reactions` | 직접방 사용자쌍 unique, 그룹 unique, `chat_messages(room_id, created_at desc, id desc)`, `chat_room_members(user_id, room_id)` |
| `V101__create_ai_generation_jobs_and_notifications.sql` | `ai_generation_jobs`, `notifications` | 요청자/client request unique, `ai_generation_jobs(status, next_attempt_at)`, `notifications(user_id, created_at desc, id desc)` |
| `V102__add_notification_read_and_monthly_recaps.sql` | notification read 상태, `monthly_recaps` | `(group_id, recap_month)` unique |

각 테이블의 Supabase RLS 활성화와 정책은 Developer A 소유다.

## A 통합 결과

- AI 큐 워커가 기본 3초 간격으로 작업을 claim하며 외부 호출 중 DB lock을 유지하지 않는다.
- OpenAI 429/5xx/네트워크 오류만 재시도하고 4xx 검증·moderation 거절은 `BLOCKED` 처리한다.
- 얼굴 참조 요청은 `gpt-image-2` 이미지 edit multipart 계약을 사용한다.
- Storage와 Realtime은 최신 `sb_secret_...` 키를 지원하고 레거시 서비스 역할 JWT도 호환한다.
- 모든 애플리케이션 테이블은 RLS 활성화 및 브라우저 역할 권한 회수 대상이다.
- private Realtime topic은 채팅방 멤버 또는 AI 작업 참여자만 구독할 수 있다.
- 월간 recap과 AI worker scheduler가 연결되었다.
- push webhook이 설정되지 않은 환경에서는 인앱 알림만 제공한다.

현재 AI 요청의 `occurrenceId`는 Core의 `routineId`를 전달한다. Core 어댑터가 해당 루틴에서
최근 1년 이내의 미완료 예정일을 검증한다.
