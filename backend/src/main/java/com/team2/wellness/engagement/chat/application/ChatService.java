package com.team2.wellness.engagement.chat.application;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.engagement.chat.domain.*;
import com.team2.wellness.engagement.chat.persistence.*;
import com.team2.wellness.engagement.port.out.CoreAccessPort;
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
    private final ChatRoomRepository rooms; private final ChatRoomMemberRepository members; private final ChatMessageRepository messages; private final MessageReactionRepository reactions; private final CoreAccessPort core; private final RealtimePublisherPort realtime;
    public ChatService(ChatRoomRepository rooms, ChatRoomMemberRepository members, ChatMessageRepository messages, MessageReactionRepository reactions, CoreAccessPort core, RealtimePublisherPort realtime) { this.rooms = rooms; this.members = members; this.messages = messages; this.reactions = reactions; this.core = core; this.realtime = realtime; }
    public ChatRoom createDirect(UUID userId, UUID targetUserId) {
        if (targetUserId == null) throw bad("DIRECT_TARGET_REQUIRED", "A direct room target is required.");
        if (userId.equals(targetUserId)) throw bad("SELF_DIRECT_ROOM", "A direct room requires another user.");
        if (!core.areAcceptedFriends(userId, targetUserId)) throw forbidden("DIRECT_ROOM_REQUIRES_FRIENDSHIP");
        String pair = ChatRoom.pairKey(userId, targetUserId);
        return rooms.findByDirectPairKey(pair).orElseGet(() -> { ChatRoom room = rooms.save(ChatRoom.direct(userId, targetUserId)); members.saveAll(List.of(new ChatRoomMember(room.getId(), userId), new ChatRoomMember(room.getId(), targetUserId))); return room; });
    }
    public ChatRoom createGroup(UUID userId, UUID groupId) {
        if (groupId == null) throw bad("GROUP_REQUIRED", "A group is required.");
        if (!core.isGroupMember(userId, groupId)) throw forbidden("GROUP_ROOM_REQUIRES_MEMBERSHIP");
        return rooms.findByGroupId(groupId).orElseGet(() -> { ChatRoom room = rooms.save(ChatRoom.group(groupId)); members.saveAll(core.getGroupMemberIds(groupId).stream().distinct().map(id -> new ChatRoomMember(room.getId(), id)).toList()); return room; });
    }
    public List<RoomView> rooms(UUID userId) { return members.findAllByUserIdOrderByJoinedAtDesc(userId).stream().map(m -> rooms.findById(m.getRoomId()).map(RoomView::from).orElse(null)).filter(Objects::nonNull).toList(); }
    public MessagePage messages(UUID userId, UUID roomId, Instant cursorAt, UUID cursorId, int size) { requireMember(roomId, userId); List<ChatMessage> page = messages.findPage(roomId, cursorAt, cursorId, PageRequest.of(0, Math.min(Math.max(size, 1), 100))); MessageView next = page.isEmpty() ? null : MessageView.from(page.getLast()); return new MessagePage(page.stream().map(MessageView::from).toList(), next == null ? null : next.createdAt(), next == null ? null : next.id()); }
    public MessageView send(UUID userId, UUID roomId, SendCommand command) {
        requireMember(roomId, userId);
        if (command.clientMessageId() != null && !command.clientMessageId().isBlank()) { Optional<ChatMessage> existing = messages.findByRoomIdAndSenderIdAndClientMessageId(roomId, userId, command.clientMessageId()); if (existing.isPresent()) return MessageView.from(existing.get()); }
        ChatMessage message = messages.save(new ChatMessage(roomId, userId, command.clientMessageId(), command.type(), command.content(), command.mediaUrl()));
        MessageView view = MessageView.from(message);
        try { realtime.publish("chat-room:" + roomId, "message.created", view); } catch (RuntimeException ex) { log.warn("Realtime delivery failed for message {}", message.getId(), ex); }
        return view;
    }
    public void addReaction(UUID userId, UUID messageId, String type) { ChatMessage message = messages.findById(messageId).orElseThrow(() -> notFound("MESSAGE_NOT_FOUND")); requireMember(message.getRoomId(), userId); if (!reactions.existsByMessageIdAndUserIdAndType(messageId, userId, type)) reactions.save(new MessageReaction(messageId, userId, type)); }
    public void removeReaction(UUID userId, UUID messageId, String type) { ChatMessage message = messages.findById(messageId).orElseThrow(() -> notFound("MESSAGE_NOT_FOUND")); requireMember(message.getRoomId(), userId); reactions.deleteByMessageIdAndUserIdAndType(messageId, userId, type); }
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
    public record RoomView(UUID id, ChatRoom.Type type, UUID groupId) { public static RoomView from(ChatRoom r) { return new RoomView(r.getId(), r.getType(), r.getGroupId()); } }
    public record MessageView(UUID id, UUID roomId, UUID senderId, ChatMessage.Type type, String content, String mediaUrl, Instant createdAt) { public static MessageView from(ChatMessage m) { return new MessageView(m.getId(), m.getRoomId(), m.getSenderId(), m.getType(), m.getContent(), m.getMediaUrl(), m.getCreatedAt()); } }
    public record MessagePage(List<MessageView> items, Instant nextCursorCreatedAt, UUID nextCursorId) { }
}
