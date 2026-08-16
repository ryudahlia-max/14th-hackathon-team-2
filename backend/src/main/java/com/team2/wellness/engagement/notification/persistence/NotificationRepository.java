package com.team2.wellness.engagement.notification.persistence;
import com.team2.wellness.engagement.notification.domain.Notification; import java.util.UUID; import org.springframework.data.jpa.repository.JpaRepository;
public interface NotificationRepository extends JpaRepository<Notification, UUID> { }
