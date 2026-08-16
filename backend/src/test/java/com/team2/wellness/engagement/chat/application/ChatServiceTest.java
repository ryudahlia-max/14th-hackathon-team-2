package com.team2.wellness.engagement.chat.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.engagement.chat.domain.*;
import com.team2.wellness.engagement.chat.persistence.*;
import com.team2.wellness.engagement.port.out.CoreAccessPort;
import com.team2.wellness.engagement.port.out.RealtimePublisherPort;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatServiceTest {
    private ChatRoomRepository rooms = mock(ChatRoomRepository.class); private ChatRoomMemberRepository members = mock(ChatRoomMemberRepository.class); private ChatMessageRepository messages = mock(ChatMessageRepository.class); private MessageReactionRepository reactions = mock(MessageReactionRepository.class); private FakeCoreAccess core = new FakeCoreAccess(); private RecordingRealtime realtime = new RecordingRealtime(); private ChatService service;
    @BeforeEach void setUp() { service = new ChatService(rooms, members, messages, reactions, core, realtime); }
    @Test void nonFriendsCannotCreateDirectRoom() { UUID user = UUID.randomUUID(), target = UUID.randomUUID(); assertThatThrownBy(() -> service.createDirect(user, target)).isInstanceOf(ApiException.class).extracting(e -> ((ApiException)e).code()).isEqualTo("DIRECT_ROOM_REQUIRES_FRIENDSHIP"); verifyNoInteractions(rooms); }
    @Test void duplicateDirectRoomUsesExistingRoom() { UUID user = UUID.randomUUID(), target = UUID.randomUUID(); core.friends = true; ChatRoom existing = ChatRoom.direct(user, target); when(rooms.findByDirectPairKey(anyString())).thenReturn(Optional.of(existing)); assertThat(service.createDirect(user, target).getId()).isEqualTo(existing.getId()); verify(rooms, never()).save(any()); }
    @Test void unauthorizedUserCannotSend() { UUID user = UUID.randomUUID(), roomId = UUID.randomUUID(); when(rooms.findById(roomId)).thenReturn(Optional.of(ChatRoom.direct(user, UUID.randomUUID()))); when(members.existsByRoomIdAndUserId(roomId, user)).thenReturn(false); assertThatThrownBy(() -> service.send(user, roomId, new ChatService.SendCommand("c", ChatMessage.Type.TEXT, "hello", null))).isInstanceOf(ApiException.class).extracting(e -> ((ApiException)e).code()).isEqualTo("CHAT_ROOM_ACCESS_DENIED"); }
    @Test void unauthorizedUserCannotRead() { UUID user = UUID.randomUUID(), roomId = UUID.randomUUID(); when(rooms.findById(roomId)).thenReturn(Optional.of(ChatRoom.direct(user, UUID.randomUUID()))); when(members.existsByRoomIdAndUserId(roomId, user)).thenReturn(false); assertThatThrownBy(() -> service.messages(user, roomId, null, null, 30)).isInstanceOf(ApiException.class).extracting(e -> ((ApiException)e).code()).isEqualTo("CHAT_ROOM_ACCESS_DENIED"); verifyNoInteractions(messages); }
    @Test void duplicateClientMessageReturnsExistingMessage() { UUID user = UUID.randomUUID(), room = UUID.randomUUID(); ChatMessage existing = new ChatMessage(room, user, "client-1", ChatMessage.Type.TEXT, "first", null); when(rooms.findById(room)).thenReturn(Optional.of(ChatRoom.direct(user, UUID.randomUUID()))); when(members.existsByRoomIdAndUserId(room, user)).thenReturn(true); when(messages.findByRoomIdAndSenderIdAndClientMessageId(room, user, "client-1")).thenReturn(Optional.of(existing)); assertThat(service.send(user, room, new ChatService.SendCommand("client-1", ChatMessage.Type.TEXT, "retry", null)).id()).isEqualTo(existing.getId()); verify(messages, never()).save(any()); }
    @Test void realtimeFailureDoesNotRemovePersistedMessage() { UUID user = UUID.randomUUID(), room = UUID.randomUUID(); when(rooms.findById(room)).thenReturn(Optional.of(ChatRoom.direct(user, UUID.randomUUID()))); when(members.existsByRoomIdAndUserId(room, user)).thenReturn(true); when(messages.save(any())).thenAnswer(invocation -> invocation.getArgument(0)); realtime.fail = true; assertThat(service.send(user, room, new ChatService.SendCommand("client-2", ChatMessage.Type.TEXT, "saved", null)).content()).isEqualTo("saved"); verify(messages).save(any(ChatMessage.class)); }
    @Test void nonMemberCannotCreateGroupRoom() { assertThatThrownBy(() -> service.createGroup(UUID.randomUUID(), UUID.randomUUID())).isInstanceOf(ApiException.class).extracting(e -> ((ApiException)e).code()).isEqualTo("GROUP_ROOM_REQUIRES_MEMBERSHIP"); }
    static final class FakeCoreAccess implements CoreAccessPort { boolean friends; public boolean areAcceptedFriends(UUID a, UUID b) { return friends; } public boolean isGroupMember(UUID u, UUID g) { return false; } public List<UUID> getGroupMemberIds(UUID g) { return List.of(); } public Optional<MissedRoutineOccurrence> getMissedRoutineOccurrence(UUID occurrenceId, UUID targetUserId) { return Optional.empty(); } public Optional<UserSummary> getUserSummary(UUID userId) { return Optional.empty(); } }
    static final class RecordingRealtime implements RealtimePublisherPort { boolean fail; public void publish(String topic, String eventType, Object payload) { if (fail) throw new IllegalStateException("offline"); } }
}
