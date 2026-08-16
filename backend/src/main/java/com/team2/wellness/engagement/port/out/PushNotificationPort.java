package com.team2.wellness.engagement.port.out;

import java.util.Map;
import java.util.UUID;

public interface PushNotificationPort {
    void send(PushCommand command);
    record PushCommand(UUID recipientId, String title, String body, Map<String, String> data) { }
}
