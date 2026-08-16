package com.team2.wellness.core.routine;

import java.time.LocalDate;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoutineCompletionRepository extends JpaRepository<RoutineCompletion, UUID> {

    Optional<RoutineCompletion> findByRoutineIdAndCompletionDate(UUID routineId, LocalDate completionDate);

    List<RoutineCompletion> findAllByUserIdAndCompletionDateBetweenOrderByCompletionDateAsc(
            UUID userId,
            LocalDate start,
            LocalDate end
    );

    List<RoutineCompletion> findAllByUserIdInOrderByCompletedAtDesc(
            Collection<UUID> userIds,
            Pageable pageable
    );

    @Query("""
            select completion from RoutineCompletion completion
            where completion.userId in :userIds
              and (:cursor is null or completion.completedAt < :cursor)
            order by completion.completedAt desc, completion.id desc
            """)
    List<RoutineCompletion> findFeed(
            @Param("userIds") Collection<UUID> userIds,
            @Param("cursor") Instant cursor,
            Pageable pageable
    );
}
