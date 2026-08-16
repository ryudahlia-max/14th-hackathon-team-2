package com.team2.wellness.engagement.chat.persistence;

import com.team2.wellness.engagement.chat.domain.ChatRoom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
    Optional<ChatRoom> findByDirectPairKey(String directPairKey);
    Optional<ChatRoom> findByGroupId(UUID groupId);
}
