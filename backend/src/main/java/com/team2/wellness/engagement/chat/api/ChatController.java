package com.team2.wellness.engagement.chat.api;

import com.team2.wellness.engagement.chat.application.ChatService;
import com.team2.wellness.engagement.chat.domain.ChatMessage;
import com.team2.wellness.engagement.port.out.CurrentUserPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/engagement")
public class ChatController {
    private final CurrentUserPort currentUser; private final ChatService chat;
    public ChatController(CurrentUserPort currentUser, ChatService chat) { this.currentUser = currentUser; this.chat = chat; }
    @PostMapping("/chat-rooms") @ResponseStatus(HttpStatus.CREATED)
    ChatService.RoomView createRoom(@Valid @RequestBody CreateRoomRequest request) { UUID userId = currentUser.currentUserId(); return request.type() == RoomType.DIRECT ? ChatService.RoomView.from(chat.createDirect(userId, request.targetUserId())) : ChatService.RoomView.from(chat.createGroup(userId, request.groupId())); }
    @GetMapping("/chat-rooms") List<ChatService.RoomView> rooms() { return chat.rooms(currentUser.currentUserId()); }
    @GetMapping("/chat-rooms/{roomId}/messages") ChatService.MessagePage messages(@PathVariable UUID roomId, @RequestParam(required = false) Instant cursorCreatedAt, @RequestParam(required = false) UUID cursorId, @RequestParam(defaultValue = "30") int size) { return chat.messages(currentUser.currentUserId(), roomId, cursorCreatedAt, cursorId, size); }
    @PostMapping("/chat-rooms/{roomId}/messages") @ResponseStatus(HttpStatus.CREATED)
    ChatService.MessageView send(@PathVariable UUID roomId, @Valid @RequestBody SendMessageRequest request) { return chat.send(currentUser.currentUserId(), roomId, new ChatService.SendCommand(request.clientMessageId(), request.type(), request.content(), request.mediaUrl())); }
    @PostMapping("/messages/{messageId}/reactions") @ResponseStatus(HttpStatus.CREATED)
    void react(@PathVariable UUID messageId, @Valid @RequestBody ReactionRequest request) { chat.addReaction(currentUser.currentUserId(), messageId, request.type()); }
    @DeleteMapping("/messages/{messageId}/reactions/{type}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void unreact(@PathVariable UUID messageId, @PathVariable String type) { chat.removeReaction(currentUser.currentUserId(), messageId, type); }
    enum RoomType { DIRECT, GROUP }
    record CreateRoomRequest(@NotNull RoomType type, UUID targetUserId, UUID groupId) { }
    record SendMessageRequest(String clientMessageId, @NotNull ChatMessage.Type type, String content, String mediaUrl) { }
    record ReactionRequest(@NotBlank String type) { }
}
