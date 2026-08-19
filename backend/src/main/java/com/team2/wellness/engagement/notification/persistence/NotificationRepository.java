package com.team2.wellness.engagement.notification.persistence;

import com.team2.wellness.engagement.notification.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

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
