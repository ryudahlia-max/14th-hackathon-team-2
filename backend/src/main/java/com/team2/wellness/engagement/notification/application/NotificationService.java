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
        return persistAndPush(new Notification(user, type, content));
    }

    public Notification createOnce(UUID user, String type, String content, String dedupKey) {
        if (dedupKey == null || dedupKey.isBlank()) {
            return create(user, type, content);
        }
        Notification existing = repo.findByDedupKey(dedupKey).orElse(null);
        if (existing != null) {
            return existing;
        }
        UUID notificationId = UUID.randomUUID();
        int inserted = repo.insertIfAbsent(notificationId, user, type, content, dedupKey, Instant.now());
        Notification notification = repo.findByDedupKey(dedupKey).orElseThrow();
        if (inserted == 1) {
            push(notification);
        }
        return notification;
    }

    private Notification persistAndPush(Notification notification) {
        notification = repo.saveAndFlush(notification);
        push(notification);
        return notification;
    }

    private void push(Notification notification) {
        try {
            push.send(new PushNotificationPort.PushCommand(
                    notification.getUserId(),
                    "Wellness",
                    notification.getContent(),
                    Map.of("notificationId", notification.getId().toString())
            ));
        } catch (RuntimeException ignored) {
        }
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
