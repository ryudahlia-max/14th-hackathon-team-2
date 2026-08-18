package com.team2.wellness.infrastructure.notification;

import com.team2.wellness.engagement.port.out.PushNotificationPort;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WebhookPushNotificationAdapter implements PushNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(WebhookPushNotificationAdapter.class);

    private final WebClient client;
    private final String webhookUrl;
    private final String bearerToken;

    public WebhookPushNotificationAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${app.push.webhook-url:}") String webhookUrl,
            @Value("${app.push.bearer-token:}") String bearerToken
    ) {
        this.client = webClientBuilder.build();
        this.webhookUrl = webhookUrl;
        this.bearerToken = bearerToken;
    }

    @Override
    public void send(PushCommand command) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendSafely(command);
                }
            });
            return;
        }
        deliver(command);
    }

    private void sendSafely(PushCommand command) {
        try {
            deliver(command);
        } catch (RuntimeException exception) {
            log.warn("Push delivery failed after notification commit for user {}", command.recipientId(), exception);
        }
    }

    private void deliver(PushCommand command) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Push webhook is not configured; notification {} remains in-app only", command.recipientId());
            return;
        }

        client.post()
                .uri(webhookUrl)
                .headers(headers -> addAuthorization(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(command)
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(5));
    }

    private void addAuthorization(HttpHeaders headers) {
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(bearerToken);
        }
    }
}
