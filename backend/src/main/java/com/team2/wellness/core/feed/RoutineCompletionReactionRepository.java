package com.team2.wellness.core.feed;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineCompletionReactionRepository extends JpaRepository<RoutineCompletionReaction, UUID> {
    Optional<RoutineCompletionReaction> findByCompletionIdAndReactorId(UUID completionId, UUID reactorId);
    List<RoutineCompletionReaction> findAllByRoutineOwnerIdOrderByCreatedAtDesc(UUID routineOwnerId);
}
