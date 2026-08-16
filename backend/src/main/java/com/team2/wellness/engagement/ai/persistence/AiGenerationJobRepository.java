package com.team2.wellness.engagement.ai.persistence;
import com.team2.wellness.engagement.ai.domain.AiGenerationJob;
import java.time.Instant; import java.util.*; import org.springframework.data.domain.Pageable; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface AiGenerationJobRepository extends JpaRepository<AiGenerationJob, UUID> {
    Optional<AiGenerationJob> findByRequesterIdAndClientRequestId(UUID requesterId, String clientRequestId);
    long countByRequesterIdAndCreatedAtGreaterThanEqual(UUID requesterId, Instant start);
    @Query("select j from AiGenerationJob j where j.status = 'QUEUED' and j.nextAttemptAt <= :now order by j.nextAttemptAt") List<AiGenerationJob> findReady(@Param("now") Instant now, Pageable pageable);
}
