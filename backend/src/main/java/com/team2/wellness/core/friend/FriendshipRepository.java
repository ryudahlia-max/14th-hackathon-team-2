package com.team2.wellness.core.friend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    Optional<Friendship> findByFirstUserIdAndSecondUserId(UUID firstUserId, UUID secondUserId);

    @Query("""
            select friendship from Friendship friendship
            where friendship.status = :status
              and (friendship.firstUserId = :userId or friendship.secondUserId = :userId)
            """)
    List<Friendship> findAllForUser(
            @Param("userId") UUID userId,
            @Param("status") Friendship.Status status
    );
}
