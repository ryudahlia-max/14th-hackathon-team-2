package com.team2.wellness.engagement.ai.persistence;
import com.team2.wellness.engagement.ai.domain.AiGenerationJob;
import java.time.Instant; import java.util.*; import jakarta.persistence.LockModeType; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface AiGenerationJobRepository extends JpaRepository<AiGenerationJob, UUID> {
    Optional<AiGenerationJob> findByRequesterIdAndClientRequestId(UUID requesterId, String clientRequestId);
    long countByRequesterIdAndCreatedAtGreaterThanEqual(UUID requesterId, Instant start);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from AiGenerationJob j where j.status = 'QUEUED' and j.nextAttemptAt <= :now order by j.nextAttemptAt") List<AiGenerationJob> findReadyForUpdate(@Param("now") Instant now, Pageable pageable);
    default List<AiGenerationJob> findReady(Instant now, Pageable pageable) { return findReadyForUpdate(now, pageable); }
}
