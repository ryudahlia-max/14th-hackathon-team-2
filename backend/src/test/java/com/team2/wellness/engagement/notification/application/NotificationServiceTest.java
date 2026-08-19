package com.team2.wellness.engagement.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.engagement.notification.persistence.NotificationRepository;
import com.team2.wellness.engagement.notification.domain.Notification;
import com.team2.wellness.engagement.port.out.PushNotificationPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private NotificationRepository notifications;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notifications = mock(NotificationRepository.class);
        service = new NotificationService(notifications, mock(PushNotificationPort.class));
    }

    @Test
    void firstPageDoesNotBindNullCursor() {
        UUID userId = UUID.randomUUID();
        when(notifications.findByUserIdOrderByCreatedAtDescIdDesc(eq(userId), any())).thenReturn(List.of());

        assertThat(service.page(userId, null, null, 30)).isEmpty();

        verify(notifications).findByUserIdOrderByCreatedAtDescIdDesc(eq(userId), any());
        verify(notifications, never()).findPageBefore(any(), any(), any(), any());
    }

    @Test
    void cursorPageUsesTypedCursorQuery() {
        UUID userId = UUID.randomUUID();
        UUID cursorId = UUID.randomUUID();
        Instant cursorAt = Instant.now();
        when(notifications.findPageBefore(eq(userId), eq(cursorAt), eq(cursorId), any())).thenReturn(List.of());

        assertThat(service.page(userId, cursorAt, cursorId, 30)).isEmpty();

        verify(notifications).findPageBefore(eq(userId), eq(cursorAt), eq(cursorId), any());
        verify(notifications, never()).findByUserIdOrderByCreatedAtDescIdDesc(any(), any());
    }

    @Test
    void incompleteCursorIsRejected() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> service.page(userId, Instant.now(), null, 30))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).code())
                .isEqualTo("INVALID_CURSOR");
        verifyNoInteractions(notifications);
    }

    @Test
    void createOnceUsesDatabaseConflictSafeInsert() {
        UUID userId = UUID.randomUUID();
        String key = "routine-reminder:test:2026-08-20";
        Notification saved = new Notification(userId, "ROUTINE_REMINDER", "알림", key);
        when(notifications.findByDedupKey(key))
                .thenReturn(java.util.Optional.empty())
                .thenReturn(java.util.Optional.of(saved));
        when(notifications.insertIfAbsent(any(), eq(userId), eq("ROUTINE_REMINDER"), eq("알림"), eq(key), any()))
                .thenReturn(1);

        assertThat(service.createOnce(userId, "ROUTINE_REMINDER", "알림", key)).isSameAs(saved);

        verify(notifications).insertIfAbsent(any(), eq(userId), eq("ROUTINE_REMINDER"), eq("알림"), eq(key), any());
    }
}
