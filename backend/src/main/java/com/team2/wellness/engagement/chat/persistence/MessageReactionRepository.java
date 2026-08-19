package com.team2.wellness.engagement.chat.persistence;

import com.team2.wellness.engagement.chat.domain.MessageReaction;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, UUID> {
    boolean existsByMessageIdAndUserIdAndType(UUID messageId, UUID userId, String type);
    void deleteByMessageIdAndUserIdAndType(UUID messageId, UUID userId, String type);
    List<MessageReaction> findAllByMessageIdIn(Collection<UUID> messageIds);
}
