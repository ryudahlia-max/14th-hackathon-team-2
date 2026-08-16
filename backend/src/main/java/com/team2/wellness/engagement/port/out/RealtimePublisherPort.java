package com.team2.wellness.engagement.port.out;

public interface RealtimePublisherPort {
    void publish(String topic, String eventType, Object payload);
}
