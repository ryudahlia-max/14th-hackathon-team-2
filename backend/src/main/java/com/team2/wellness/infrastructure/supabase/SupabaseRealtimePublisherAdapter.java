package com.team2.wellness.infrastructure.supabase;

import com.team2.wellness.engagement.port.out.RealtimePublisherPort;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class SupabaseRealtimePublisherAdapter implements RealtimePublisherPort {

    private static final Logger log = LoggerFactory.getLogger(SupabaseRealtimePublisherAdapter.class);

    private final WebClient client;
    private final String secretKey;

    public SupabaseRealtimePublisherAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${app.supabase.url}") String supabaseUrl,
            @Value("${app.supabase.secret-key:}") String secretKey
    ) {
        this.client = webClientBuilder.baseUrl(stripTrailingSlash(supabaseUrl)).build();
        this.secretKey = secretKey;
    }

    @Override
    public void publish(String topic, String eventType, Object payload) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishSafely(topic, eventType, payload);
                }
            });
            return;
        }
        send(topic, eventType, payload);
    }

    private void publishSafely(String topic, String eventType, Object payload) {
        try {
            send(topic, eventType, payload);
        } catch (RuntimeException exception) {
            log.warn("Realtime delivery failed after commit for topic {} and event {}", topic, eventType, exception);
        }
    }

    private void send(String topic, String eventType, Object payload) {
        client.post()
                .uri(uriBuilder -> uriBuilder.path("/realtime/v1/api/broadcast")
                        .pathSegment(topic)
                        .pathSegment("events")
                        .pathSegment(eventType)
                        .queryParam("private", true)
                        .build())
                .headers(headers -> SupabaseApiHeaders.authenticate(headers, secretKey))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(5));
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
