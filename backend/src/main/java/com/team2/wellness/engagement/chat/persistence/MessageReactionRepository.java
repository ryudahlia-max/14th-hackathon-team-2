package com.team2.wellness.engagement.chat.persistence;

import com.team2.wellness.engagement.chat.domain.MessageReaction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {
    boolean existsByMessageIdAndUserIdAndType(UUID messageId, UUID userId, String type);
    void deleteByMessageIdAndUserIdAndType(UUID messageId, UUID userId, String type);
}
