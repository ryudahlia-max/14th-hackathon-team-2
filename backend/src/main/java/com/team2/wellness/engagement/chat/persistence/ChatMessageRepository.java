package com.team2.wellness.engagement.chat.persistence;

import com.team2.wellness.engagement.chat.domain.ChatMessage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    Optional<ChatMessage> findFirstByRoomIdOrderByCreatedAtDesc(UUID roomId);
    Optional<ChatMessage> findByRoomIdAndSenderIdAndClientMessageId(UUID roomId, UUID senderId, String clientMessageId);
    List<ChatMessage> findByRoomIdOrderByCreatedAtDescIdDesc(UUID roomId, Pageable pageable);

    @Query("select m from ChatMessage m where m.roomId = :roomId and "
            + "(m.createdAt < :cursorAt or (m.createdAt = :cursorAt and m.id < :cursorId)) "
            + "order by m.createdAt desc, m.id desc")
    List<ChatMessage> findPageBefore(
            @Param("roomId") UUID roomId,
            @Param("cursorAt") Instant cursorAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
