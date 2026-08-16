package com.team2.wellness.engagement.chat.persistence;

import com.team2.wellness.engagement.chat.domain.ChatRoomMember;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {
    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);
    List<ChatRoomMember> findAllByUserIdOrderByJoinedAtDesc(UUID userId);
    List<ChatRoomMember> findAllByRoomId(UUID roomId);
    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);
}
