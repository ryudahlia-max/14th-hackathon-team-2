package com.team2.wellness.engagement.notification.persistence;

import com.team2.wellness.engagement.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByDedupKey(String dedupKey);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            insert into notifications (id, user_id, type, content, dedup_key, created_at)
            values (:id, :userId, :type, :content, :dedupKey, :createdAt)
            on conflict (dedup_key) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("type") String type,
            @Param("content") String content,
            @Param("dedupKey") String dedupKey,
            @Param("createdAt") Instant createdAt
    );

    List<Notification> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

    @Query("select n from Notification n where n.userId = :userId and "
            + "(n.createdAt < :cursorAt or (n.createdAt = :cursorAt and n.id < :cursorId)) "
            + "order by n.createdAt desc, n.id desc")
    List<Notification> findPageBefore(
            @Param("userId") UUID userId,
            @Param("cursorAt") Instant cursorAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    List<Notification> findByUserIdAndReadAtIsNull(UUID userId);
}
