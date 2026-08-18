package com.team2.wellness.engagement.chat.application;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.engagement.chat.domain.*;
import com.team2.wellness.engagement.chat.persistence.*;
import com.team2.wellness.engagement.port.out.CoreAccessPort;
import com.team2.wellness.engagement.port.out.MediaStoragePort;
import com.team2.wellness.engagement.port.out.RealtimePublisherPort;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ChatRoomRepository rooms; private final ChatRoomMemberRepository members; private final ChatMessageRepository messages; private final MessageReactionRepository reactions; private final CoreAccessPort core; private final RealtimePublisherPort realtime; private final MediaStoragePort storage;
    public ChatService(ChatRoomRepository rooms, ChatRoomMemberRepository members, ChatMessageRepository messages, MessageReactionRepository reactions, CoreAccessPort core, RealtimePublisherPort realtime, MediaStoragePort storage) { this.rooms = rooms; this.members = members; this.messages = messages; this.reactions = reactions; this.core = core; this.realtime = realtime; this.storage = storage; }
    public ChatRoom createDirect(UUID userId, UUID targetUserId) {
        if (targetUserId == null) throw bad("DIRECT_TARGET_REQUIRED", "A direct room target is required.");
        if (userId.equals(targetUserId)) throw bad("SELF_DIRECT_ROOM", "A direct room requires another user.");
        if (!core.areAcceptedFriends(userId, targetUserId)) throw forbidden("DIRECT_ROOM_REQUIRES_FRIENDSHIP");
        String pair = ChatRoom.pairKey(userId, targetUserId);
        ChatRoom room = rooms.findByDirectPairKey(pair).orElseGet(() -> rooms.save(ChatRoom.direct(userId, targetUserId)));
        ensureMember(room.getId(), userId);
        ensureMember(room.getId(), targetUserId);
        return room;
    }
    public ChatRoom createGroup(UUID userId, UUID groupId) {
        if (groupId == null) throw bad("GROUP_REQUIRED", "A group is required.");
        if (!core.isGroupMember(userId, groupId)) throw forbidden("GROUP_ROOM_REQUIRES_MEMBERSHIP");
        ChatRoom room = rooms.findByGroupId(groupId).orElseGet(() -> rooms.save(ChatRoom.group(groupId)));
        core.getGroupMemberIds(groupId).stream().distinct().forEach(id -> ensureMember(room.getId(), id));
        return room;
    }
    public List<RoomView> rooms(UUID userId) { return members.findAllByUserIdOrderByJoinedAtDesc(userId).stream().map(m -> rooms.findById(m.getRoomId()).map(room -> roomView(userId, room)).orElse(null)).filter(Objects::nonNull).toList(); }
    public MessagePage messages(UUID userId, UUID roomId, Instant cursorAt, UUID cursorId, int size) { requireMember(roomId, userId); List<ChatMessage> page = messages.findPage(roomId, cursorAt, cursorId, PageRequest.of(0, Math.min(Math.max(size, 1), 100))); MessageView next = page.isEmpty() ? null : messageView(page.getLast()); return new MessagePage(page.stream().map(this::messageView).toList(), next == null ? null : next.createdAt(), next == null ? null : next.id()); }
    public MessageView send(UUID userId, UUID roomId, SendCommand command) {
        requireMember(roomId, userId);
        if (command.clientMessageId() != null && !command.clientMessageId().isBlank()) { Optional<ChatMessage> existing = messages.findByRoomIdAndSenderIdAndClientMessageId(roomId, userId, command.clientMessageId()); if (existing.isPresent()) return messageView(existing.get()); }
        ChatMessage message = messages.save(new ChatMessage(roomId, userId, command.clientMessageId(), command.type(), command.content(), command.mediaUrl()));
        MessageView view = messageView(message);
        try { realtime.publish("chat-room:" + roomId, "message.created", view); } catch (RuntimeException ex) { log.warn("Realtime delivery failed for message {}", message.getId(), ex); }
        return view;
    }
    public void addReaction(UUID userId, UUID messageId, String type) { ChatMessage message = messages.findById(messageId).orElseThrow(() -> notFound("MESSAGE_NOT_FOUND")); requireMember(message.getRoomId(), userId); if (!reactions.existsByMessageIdAndUserIdAndType(messageId, userId, type)) reactions.save(new MessageReaction(messageId, userId, type)); }
    public void removeReaction(UUID userId, UUID messageId, String type) { ChatMessage message = messages.findById(messageId).orElseThrow(() -> notFound("MESSAGE_NOT_FOUND")); requireMember(message.getRoomId(), userId); reactions.deleteByMessageIdAndUserIdAndType(messageId, userId, type); }
    public void leave(UUID userId, UUID roomId) { requireMember(roomId, userId); members.deleteByRoomIdAndUserId(roomId, userId); }
    private void ensureMember(UUID roomId, UUID userId) { if (!members.existsByRoomIdAndUserId(roomId, userId)) members.save(new ChatRoomMember(roomId, userId)); }
    public RoomView roomView(UUID userId, ChatRoom room) {
        List<UUID> memberIds = members.findAllByRoomId(room.getId()).stream().map(ChatRoomMember::getUserId).toList();
        String name = room.getType() == ChatRoom.Type.GROUP
                ? core.getGroupSummary(room.getGroupId()).map(CoreAccessPort.GroupSummary::name).orElse("그룹 채팅")
                : memberIds.stream().filter(id -> !id.equals(userId)).findFirst()
                        .flatMap(core::getUserSummary).map(CoreAccessPort.UserSummary::nickname).orElse("알 수 없는 사용자");
        MessageView lastMessage = messages.findFirstByRoomIdOrderByCreatedAtDesc(room.getId()).map(this::messageView).orElse(null);
        return new RoomView(room.getId(), room.getType(), room.getGroupId(), name, memberIds, room.getCreatedAt(), lastMessage);
    }
    private MessageView messageView(ChatMessage message) {
        String mediaUrl = message.getMediaUrl();
        if (mediaUrl != null && !mediaUrl.isBlank()) {
            try { mediaUrl = storage.temporaryDownloadUrl(mediaUrl); } catch (RuntimeException ignored) { }
        }
        return new MessageView(message.getId(), message.getRoomId(), message.getSenderId(), message.getType(), message.getContent(), mediaUrl, message.getCreatedAt());
    }
    private void requireMember(UUID roomId, UUID userId) {
        ChatRoom room = rooms.findById(roomId).orElseThrow(() -> notFound("CHAT_ROOM_NOT_FOUND"));
        if (!members.existsByRoomIdAndUserId(roomId, userId)
                || (room.getType() == ChatRoom.Type.GROUP && !core.isGroupMember(userId, room.getGroupId()))) {
            throw forbidden("CHAT_ROOM_ACCESS_DENIED");
        }
    }
    private ApiException forbidden(String code) { return new ApiException(HttpStatus.FORBIDDEN, code, "You do not have access to this chat room."); }
    private ApiException bad(String code, String message) { return new ApiException(HttpStatus.BAD_REQUEST, code, message); }
    private ApiException notFound(String code) { return new ApiException(HttpStatus.NOT_FOUND, code, "Requested chat resource was not found."); }
    public record SendCommand(String clientMessageId, ChatMessage.Type type, String content, String mediaUrl) { }
    public record RoomView(UUID id, ChatRoom.Type type, UUID groupId, String name, List<UUID> memberIds, Instant createdAt, MessageView lastMessage) { }
    public record MessageView(UUID id, UUID roomId, UUID senderId, ChatMessage.Type type, String content, String mediaUrl, Instant createdAt) { public static MessageView from(ChatMessage m) { return new MessageView(m.getId(), m.getRoomId(), m.getSenderId(), m.getType(), m.getContent(), m.getMediaUrl(), m.getCreatedAt()); } }
    public record MessagePage(List<MessageView> items, Instant nextCursorCreatedAt, UUID nextCursorId) { }
}
