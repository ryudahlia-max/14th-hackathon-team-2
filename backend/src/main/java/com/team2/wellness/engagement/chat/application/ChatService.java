package com.team2.wellness.engagement.chat.application;

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
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final Set<String> ALLOWED_REACTIONS = Set.of("HEART", "SAD", "THUMBS_UP", "FIRE", "SMILE");

    private final ChatRoomRepository rooms;
    private final ChatRoomMemberRepository members;
    private final ChatMessageRepository messages;
    private final MessageReactionRepository reactions;
    private final CoreAccessPort core;
    private final RealtimePublisherPort realtime;
    private final MediaStoragePort storage;
    private final NotificationService notifications;

    public ChatService(
            ChatRoomRepository rooms,
            ChatRoomMemberRepository members,
            ChatMessageRepository messages,
            MessageReactionRepository reactions,
            CoreAccessPort core,
            RealtimePublisherPort realtime,
            MediaStoragePort storage,
            NotificationService notifications
    ) {
        this.rooms = rooms;
        this.members = members;
        this.messages = messages;
        this.reactions = reactions;
        this.core = core;
        this.realtime = realtime;
        this.storage = storage;
        this.notifications = notifications;
    }

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

    public List<RoomView> rooms(UUID userId) {
        return members.findAllByUserIdOrderByJoinedAtDesc(userId).stream()
                .map(member -> rooms.findById(member.getRoomId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(room -> canAccess(userId, room))
                .map(room -> roomView(userId, room))
                .toList();
    }

    public MessagePage messages(UUID userId, UUID roomId, Instant cursorAt, UUID cursorId, int size) {
        requireAccess(roomId, userId);
        if ((cursorAt == null) != (cursorId == null)) {
            throw bad("INVALID_CURSOR", "Both cursorCreatedAt and cursorId are required.");
        }

        int pageSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        List<ChatMessage> fetched = cursorAt == null
                ? messages.findByRoomIdOrderByCreatedAtDescIdDesc(roomId, pageRequest)
                : messages.findPageBefore(roomId, cursorAt, cursorId, pageRequest);
        boolean hasNext = fetched.size() > pageSize;
        List<ChatMessage> page = hasNext ? fetched.subList(0, pageSize) : fetched;
        List<MessageView> views = messageViews(page);
        MessageView next = hasNext && !views.isEmpty() ? views.getLast() : null;
        return new MessagePage(
                views,
                next == null ? null : next.createdAt(),
                next == null ? null : next.id()
        );
    }

    public MessageView send(UUID userId, UUID roomId, SendCommand command) {
        ChatRoom room = requireAccess(roomId, userId);
        if (command.clientMessageId() != null && !command.clientMessageId().isBlank()) {
            Optional<ChatMessage> existing = messages.findByRoomIdAndSenderIdAndClientMessageId(
                    roomId, userId, command.clientMessageId());
            if (existing.isPresent()) return messageView(existing.get());
        }
        ChatMessage message = messages.save(new ChatMessage(
                roomId, userId, command.clientMessageId(), command.type(), command.content(), command.mediaUrl()));
        MessageView view = messageView(message);
        publish(roomId, "message.created", view);
        String sender = core.getUserSummary(userId).map(CoreAccessPort.UserSummary::nickname).orElse("친구");
        notificationRecipients(room).stream().filter(recipient -> !recipient.equals(userId)).forEach(recipient ->
                notifications.createOnce(
                        recipient,
                        "CHAT_MESSAGE",
                        sender + "님이 새 메시지를 보냈습니다.",
                        "chat-message:" + message.getId() + ":" + recipient
                ));
        return view;
    }

    public void addReaction(UUID userId, UUID messageId, String rawType) {
        ChatMessage message = messages.findById(messageId).orElseThrow(() -> notFound("MESSAGE_NOT_FOUND"));
        requireAccess(message.getRoomId(), userId);
        String type = normalizeReaction(rawType);
        if (!reactions.existsByMessageIdAndUserIdAndType(messageId, userId, type)) {
            reactions.save(new MessageReaction(messageId, userId, type));
        }
        publish(message.getRoomId(), "message.reaction.updated", new ReactionEvent(messageId, userId, type, true));
        if (message.getSenderId() != null && !message.getSenderId().equals(userId)) {
            String reactor = core.getUserSummary(userId).map(CoreAccessPort.UserSummary::nickname).orElse("친구");
            notifications.createOnce(
                    message.getSenderId(),
                    "CHAT_REACTION",
                    reactor + "님이 메시지에 공감을 남겼습니다.",
                    "chat-reaction:" + messageId + ":" + userId + ":" + type
            );
        }
    }

    public void removeReaction(UUID userId, UUID messageId, String rawType) {
        ChatMessage message = messages.findById(messageId).orElseThrow(() -> notFound("MESSAGE_NOT_FOUND"));
        requireAccess(message.getRoomId(), userId);
        String type = normalizeReaction(rawType);
        reactions.deleteByMessageIdAndUserIdAndType(messageId, userId, type);
        publish(message.getRoomId(), "message.reaction.updated", new ReactionEvent(messageId, userId, type, false));
    }

    public void leave(UUID userId, UUID roomId) {
        requireAccess(roomId, userId);
        members.deleteByRoomIdAndUserId(roomId, userId);
    }

    private void ensureMember(UUID roomId, UUID userId) {
        if (!members.existsByRoomIdAndUserId(roomId, userId)) {
            members.save(new ChatRoomMember(roomId, userId));
        }
    }

    public RoomView roomView(UUID userId, ChatRoom room) {
        List<UUID> memberIds = memberIds(room.getId());
        String name = room.getType() == ChatRoom.Type.GROUP
                ? core.getGroupSummary(room.getGroupId()).map(CoreAccessPort.GroupSummary::name).orElse("그룹 채팅")
                : memberIds.stream().filter(id -> !id.equals(userId)).findFirst()
                        .flatMap(core::getUserSummary).map(CoreAccessPort.UserSummary::nickname).orElse("알 수 없는 사용자");
        MessageView lastMessage = messages.findFirstByRoomIdOrderByCreatedAtDesc(room.getId())
                .map(this::messageView).orElse(null);
        return new RoomView(room.getId(), room.getType(), room.getGroupId(), name, memberIds, room.getCreatedAt(), lastMessage);
    }

    private List<MessageView> messageViews(List<ChatMessage> page) {
        if (page.isEmpty()) return List.of();
        Map<UUID, List<ReactionView>> byMessage = new HashMap<>();
        reactions.findAllByMessageIdIn(page.stream().map(ChatMessage::getId).toList()).forEach(reaction ->
                byMessage.computeIfAbsent(reaction.getMessageId(), ignored -> new ArrayList<>())
                        .add(ReactionView.from(reaction)));
        return page.stream().map(message -> messageView(message, byMessage.getOrDefault(message.getId(), List.of()))).toList();
    }

    private MessageView messageView(ChatMessage message) {
        List<ReactionView> reactionViews = reactions.findAllByMessageIdIn(List.of(message.getId())).stream()
                .map(ReactionView::from)
                .toList();
        return messageView(message, reactionViews);
    }

    private MessageView messageView(ChatMessage message, List<ReactionView> reactionViews) {
        String mediaUrl = message.getMediaUrl();
        if (mediaUrl != null && !mediaUrl.isBlank()) {
            try {
                mediaUrl = storage.temporaryDownloadUrl(mediaUrl);
            } catch (RuntimeException ignored) {
                // The message remains readable even if a signed URL cannot be issued temporarily.
            }
        }
        return new MessageView(
                message.getId(), message.getRoomId(), message.getSenderId(), message.getType(),
                message.getContent(), mediaUrl, message.getCreatedAt(), reactionViews);
    }

    private ChatRoom requireAccess(UUID roomId, UUID userId) {
        ChatRoom room = rooms.findById(roomId).orElseThrow(() -> notFound("CHAT_ROOM_NOT_FOUND"));
        if (!canAccess(userId, room)) {
            throw forbidden("CHAT_ROOM_ACCESS_DENIED");
        }
        return room;
    }

    private boolean canAccess(UUID userId, ChatRoom room) {
        if (!members.existsByRoomIdAndUserId(room.getId(), userId)) return false;
        if (room.getType() == ChatRoom.Type.GROUP) {
            return core.isGroupMember(userId, room.getGroupId());
        }
        return memberIds(room.getId()).stream()
                .filter(memberId -> !memberId.equals(userId))
                .findFirst()
                .map(peerId -> core.areAcceptedFriends(userId, peerId))
                .orElse(false);
    }

    private List<UUID> memberIds(UUID roomId) {
        return members.findAllByRoomId(roomId).stream().map(ChatRoomMember::getUserId).toList();
    }

    private List<UUID> notificationRecipients(ChatRoom room) {
        return room.getType() == ChatRoom.Type.GROUP
                ? core.getGroupMemberIds(room.getGroupId())
                : memberIds(room.getId());
    }

    private String normalizeReaction(String rawType) {
        String type = rawType == null ? "" : rawType.trim().toUpperCase();
        if (!ALLOWED_REACTIONS.contains(type)) {
            throw bad("INVALID_REACTION_TYPE", "Unsupported reaction type.");
        }
        return type;
    }

    private void publish(UUID roomId, String eventType, Object payload) {
        try {
            realtime.publish("chat-room:" + roomId, eventType, payload);
        } catch (RuntimeException exception) {
            log.warn("Realtime delivery failed for room {} and event {}", roomId, eventType, exception);
        }
    }

    private ApiException forbidden(String code) {
        return new ApiException(HttpStatus.FORBIDDEN, code, "You do not have access to this chat room.");
    }

    private ApiException bad(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    private ApiException notFound(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, code, "Requested chat resource was not found.");
    }

    public record SendCommand(String clientMessageId, ChatMessage.Type type, String content, String mediaUrl) { }

    public record RoomView(
            UUID id,
            ChatRoom.Type type,
            UUID groupId,
            String name,
            List<UUID> memberIds,
            Instant createdAt,
            MessageView lastMessage
    ) { }

    public record ReactionView(UUID userId, String type, Instant createdAt) {
        static ReactionView from(MessageReaction reaction) {
            return new ReactionView(reaction.getUserId(), reaction.getType(), reaction.getCreatedAt());
        }
    }

    public record ReactionEvent(UUID messageId, UUID userId, String type, boolean active) { }

    public record MessageView(
            UUID id,
            UUID roomId,
            UUID senderId,
            ChatMessage.Type type,
            String content,
            String mediaUrl,
            Instant createdAt,
            List<ReactionView> reactions
    ) {
        public static MessageView from(ChatMessage message) {
            return new MessageView(
                    message.getId(), message.getRoomId(), message.getSenderId(), message.getType(),
                    message.getContent(), message.getMediaUrl(), message.getCreatedAt(), List.of());
        }
    }

    public record MessagePage(List<MessageView> items, Instant nextCursorCreatedAt, UUID nextCursorId) { }
}
