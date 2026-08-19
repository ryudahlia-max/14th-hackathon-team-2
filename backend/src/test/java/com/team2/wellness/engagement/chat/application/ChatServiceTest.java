package com.team2.wellness.engagement.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.engagement.chat.domain.ChatMessage;
import com.team2.wellness.engagement.chat.domain.ChatRoom;
import com.team2.wellness.engagement.chat.domain.ChatRoomMember;
import com.team2.wellness.engagement.chat.domain.MessageReaction;
import com.team2.wellness.engagement.chat.persistence.ChatMessageRepository;
import com.team2.wellness.engagement.chat.persistence.ChatRoomMemberRepository;
import com.team2.wellness.engagement.chat.persistence.ChatRoomRepository;
import com.team2.wellness.engagement.chat.persistence.MessageReactionRepository;
import com.team2.wellness.engagement.notification.application.NotificationService;
import com.team2.wellness.engagement.port.out.CoreAccessPort;
import com.team2.wellness.engagement.port.out.MediaStoragePort;
import com.team2.wellness.engagement.port.out.RealtimePublisherPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatServiceTest {

    private final ChatRoomRepository rooms = mock(ChatRoomRepository.class);
    private final ChatRoomMemberRepository members = mock(ChatRoomMemberRepository.class);
    private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
    private final MessageReactionRepository reactions = mock(MessageReactionRepository.class);
    private final FakeCoreAccess core = new FakeCoreAccess();
    private final RecordingRealtime realtime = new RecordingRealtime();
    private final MediaStoragePort storage = mock(MediaStoragePort.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private ChatService service;

    @BeforeEach
    void setUp() {
        when(storage.temporaryDownloadUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ChatService(rooms, members, messages, reactions, core, realtime, storage, notifications);
    }

    @Test
    void nonFriendsCannotCreateDirectRoom() {
        UUID user = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        assertThatThrownBy(() -> service.createDirect(user, target))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("DIRECT_ROOM_REQUIRES_FRIENDSHIP");
        verifyNoInteractions(rooms);
    }

    @Test
    void duplicateDirectRoomUsesExistingRoom() {
        UUID user = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        core.friends = true;
        ChatRoom existing = ChatRoom.direct(user, target);
        when(rooms.findByDirectPairKey(anyString())).thenReturn(Optional.of(existing));

        assertThat(service.createDirect(user, target).getId()).isEqualTo(existing.getId());

        verify(rooms, never()).save(any());
    }

    @Test
    void removedFriendCannotReadSendOrSeeExistingDirectRoom() {
        UUID user = UUID.randomUUID();
        UUID peer = UUID.randomUUID();
        ChatRoom room = directRoom(user, peer);
        when(members.findAllByUserIdOrderByJoinedAtDesc(user))
                .thenReturn(List.of(new ChatRoomMember(room.getId(), user)));
        core.friends = false;

        assertThatThrownBy(() -> service.messages(user, room.getId(), null, null, 30))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("CHAT_ROOM_ACCESS_DENIED");
        assertThatThrownBy(() -> service.send(
                user,
                room.getId(),
                new ChatService.SendCommand("client", ChatMessage.Type.TEXT, "hello", null)
        )).isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("CHAT_ROOM_ACCESS_DENIED");
        assertThat(service.rooms(user)).isEmpty();
        verifyNoInteractions(messages);
    }

    @Test
    void unauthorizedUserCannotSend() {
        UUID user = UUID.randomUUID();
        UUID peer = UUID.randomUUID();
        ChatRoom room = ChatRoom.direct(user, peer);
        when(rooms.findById(room.getId())).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.send(
                user,
                room.getId(),
                new ChatService.SendCommand("c", ChatMessage.Type.TEXT, "hello", null)
        )).isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("CHAT_ROOM_ACCESS_DENIED");
    }

    @Test
    void firstMessagePageUsesLookaheadAndReturnsOnlyRealNextCursor() {
        UUID user = UUID.randomUUID();
        UUID peer = UUID.randomUUID();
        ChatRoom room = directRoom(user, peer);
        ChatMessage newest = new ChatMessage(room.getId(), peer, "1", ChatMessage.Type.TEXT, "newest", null);
        ChatMessage older = new ChatMessage(room.getId(), peer, "2", ChatMessage.Type.TEXT, "older", null);
        ChatMessage lookahead = new ChatMessage(room.getId(), peer, "3", ChatMessage.Type.TEXT, "lookahead", null);
        when(messages.findByRoomIdOrderByCreatedAtDescIdDesc(
                eq(room.getId()), argThat(pageable -> pageable.getPageSize() == 3)))
                .thenReturn(List.of(newest, older, lookahead));

        ChatService.MessagePage page = service.messages(user, room.getId(), null, null, 2);

        assertThat(page.items()).extracting(ChatService.MessageView::content)
                .containsExactly("newest", "older");
        assertThat(page.nextCursorId()).isEqualTo(older.getId());
    }

    @Test
    void incompleteMessageCursorIsRejected() {
        UUID user = UUID.randomUUID();
        UUID peer = UUID.randomUUID();
        ChatRoom room = directRoom(user, peer);

        assertThatThrownBy(() -> service.messages(user, room.getId(), Instant.now(), null, 30))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("INVALID_CURSOR");
        verifyNoInteractions(messages);
    }

    @Test
    void duplicateClientMessageReturnsExistingMessage() {
        UUID user = UUID.randomUUID();
        UUID peer = UUID.randomUUID();
        ChatRoom room = directRoom(user, peer);
        ChatMessage existing = new ChatMessage(
                room.getId(), user, "client-1", ChatMessage.Type.TEXT, "first", null);
        when(messages.findByRoomIdAndSenderIdAndClientMessageId(room.getId(), user, "client-1"))
                .thenReturn(Optional.of(existing));

        assertThat(service.send(
                user,
                room.getId(),
                new ChatService.SendCommand("client-1", ChatMessage.Type.TEXT, "retry", null)
        ).id()).isEqualTo(existing.getId());

        verify(messages, never()).save(any());
    }

    @Test
    void sendPersistsPublishesAndNotifiesOtherMembers() {
        UUID user = UUID.randomUUID();
        UUID peer = UUID.randomUUID();
        ChatRoom room = directRoom(user, peer);
        when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ChatService.MessageView saved = service.send(
                user,
                room.getId(),
                new ChatService.SendCommand("client-2", ChatMessage.Type.TEXT, "saved", null)
        );

        assertThat(saved.content()).isEqualTo("saved");
        assertThat(realtime.events).contains("message.created");
        verify(notifications).createOnce(
                eq(peer), eq("CHAT_MESSAGE"), anyString(), argThat(key -> key.startsWith("chat-message:")));
    }

    @Test
    void messageResponsesIncludeStoredReactions() {
        UUID user = UUID.randomUUID();
        UUID peer = UUID.randomUUID();
        ChatRoom room = directRoom(user, peer);
        ChatMessage message = new ChatMessage(room.getId(), peer, "1", ChatMessage.Type.TEXT, "hello", null);
        when(messages.findByRoomIdOrderByCreatedAtDescIdDesc(eq(room.getId()), any()))
                .thenReturn(List.of(message));
        when(reactions.findAllByMessageIdIn(List.of(message.getId())))
                .thenReturn(List.of(new MessageReaction(message.getId(), user, "HEART")));

        ChatService.MessageView result = service.messages(user, room.getId(), null, null, 30).items().getFirst();

        assertThat(result.reactions()).extracting(ChatService.ReactionView::type).containsExactly("HEART");
    }

    @Test
    void nonMemberCannotCreateGroupRoom() {
        assertThatThrownBy(() -> service.createGroup(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("GROUP_ROOM_REQUIRES_MEMBERSHIP");
    }

    private ChatRoom directRoom(UUID user, UUID peer) {
        core.friends = true;
        ChatRoom room = ChatRoom.direct(user, peer);
        when(rooms.findById(room.getId())).thenReturn(Optional.of(room));
        when(members.existsByRoomIdAndUserId(room.getId(), user)).thenReturn(true);
        when(members.findAllByRoomId(room.getId())).thenReturn(List.of(
                new ChatRoomMember(room.getId(), user),
                new ChatRoomMember(room.getId(), peer)
        ));
        return room;
    }

    static final class FakeCoreAccess implements CoreAccessPort {
        boolean friends;
        public boolean areAcceptedFriends(UUID first, UUID second) { return friends; }
        public boolean isGroupMember(UUID user, UUID group) { return false; }
        public List<UUID> getGroupMemberIds(UUID group) { return List.of(); }
        public Optional<MissedRoutineOccurrence> getMissedRoutineOccurrence(UUID occurrenceId, UUID targetUserId) { return Optional.empty(); }
        public Optional<UserSummary> getUserSummary(UUID userId) { return Optional.empty(); }
        public Optional<GroupSummary> getGroupSummary(UUID groupId) { return Optional.empty(); }
        public boolean hasAiImageConsent(UUID userId) { return false; }
    }

    static final class RecordingRealtime implements RealtimePublisherPort {
        final List<String> events = new java.util.ArrayList<>();
        public void publish(String topic, String eventType, Object payload) { events.add(eventType); }
    }
}
