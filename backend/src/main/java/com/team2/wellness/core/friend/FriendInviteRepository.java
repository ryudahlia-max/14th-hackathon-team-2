package com.team2.wellness.core.friend;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendInviteRepository extends JpaRepository<FriendInvite, UUID> {

    Optional<FriendInvite> findByToken(String token);
}
