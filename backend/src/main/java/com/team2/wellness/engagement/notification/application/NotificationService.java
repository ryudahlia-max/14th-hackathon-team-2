package com.team2.wellness.engagement.notification.application;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.engagement.notification.domain.Notification;
import com.team2.wellness.engagement.notification.persistence.NotificationRepository;
import com.team2.wellness.engagement.port.out.PushNotificationPort;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repo;
    private final PushNotificationPort push;

    public NotificationService(NotificationRepository repo, PushNotificationPort push) {
        this.repo = repo;
        this.push = push;
    }

    public Notification create(UUID user, String type, String content) {
        Notification notification = repo.save(new Notification(user, type, content));
        try {
            push.send(new PushNotificationPort.PushCommand(
                    user,
                    "Wellness",
                    content,
                    Map.of("notificationId", notification.getId().toString())
            ));
        } catch (RuntimeException ignored) {
        }
        return notification;
    }

    public List<Notification> page(UUID user, Instant cursorAt, UUID cursorId, int size) {
        if ((cursorAt == null) != (cursorId == null)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CURSOR",
                    "Both cursorCreatedAt and cursorId are required."
            );
        }

        PageRequest pageRequest = PageRequest.of(0, Math.min(100, Math.max(1, size)));
        return cursorAt == null
                ? repo.findByUserIdOrderByCreatedAtDescIdDesc(user, pageRequest)
                : repo.findPageBefore(user, cursorAt, cursorId, pageRequest);
    }

    public void read(UUID user, UUID id) {
        Notification notification = repo.findById(id)
                .filter(item -> item.getUserId().equals(user))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "NOTIFICATION_NOT_FOUND",
                        "Notification not found"
                ));
        notification.markRead();
    }

    public void readAll(UUID user) {
        repo.findByUserIdAndReadAtIsNull(user).forEach(Notification::markRead);
    }
}
