package com.team2.wellness.infrastructure.supabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.team2.wellness.core.profile.ProfileRepository;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.web.reactive.function.client.WebClient;

class SupabaseAdaptersTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void currentSecretKeyUsesApiKeyHeaderWithoutBearerJwt() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        SupabaseMediaStorageAdapter adapter = storageAdapter("sb_secret_test");

        var stored = adapter.storeAiOutput(UUID.randomUUID(), new byte[]{1, 2, 3}, "image/png");

        assertThat(stored.objectKey()).startsWith("ai-results/ai-generations/").endsWith(".png");
        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("apikey")).isEqualTo("sb_secret_test");
        assertThat(request.getHeader("Authorization")).isNull();
        assertThat(request.getPath()).startsWith("/storage/v1/object/ai-results/ai-generations/");
    }

    @Test
    void legacyServiceRoleKeyAlsoUsesBearerHeader() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        SupabaseMediaStorageAdapter adapter = storageAdapter("legacy-service-role-jwt");

        adapter.storeChatMedia(UUID.randomUUID(), new byte[]{1}, "image/jpeg");

        RecordedRequest request = server.takeRequest();
        assertThat(request.getHeader("apikey")).isEqualTo("legacy-service-role-jwt");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer legacy-service-role-jwt");
    }

    @Test
    void signedStoragePathIsResolvedAgainstStorageApiBase() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"signedURL":"/object/sign/avatars/user/avatar.png?token=test"}
                        """));
        SupabaseMediaStorageAdapter adapter = storageAdapter("sb_secret_test");

        String signedUrl = adapter.temporaryDownloadUrl("avatars/user/avatar.png");

        assertThat(signedUrl).isEqualTo(
                server.url("/storage/v1/object/sign/avatars/user/avatar.png?token=test").toString()
        );
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/storage/v1/object/sign/avatars/user/avatar.png");
    }

    @Test
    void realtimeBroadcastUsesPrivateTopicRestEndpoint() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(202));
        SupabaseRealtimePublisherAdapter adapter = new SupabaseRealtimePublisherAdapter(
                WebClient.builder(),
                server.url("/").toString(),
                "sb_secret_test"
        );

        adapter.publish("chat-room:" + UUID.randomUUID(), "message.created", Map.of("id", "message"));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath())
                .startsWith("/realtime/v1/api/broadcast/chat-room:")
                .contains("/events/message.created?private=true");
        assertThat(request.getHeader("apikey")).isEqualTo("sb_secret_test");
    }

    @Test
    void realtimeBroadcastWaitsUntilDatabaseCommit() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(202));
        SupabaseRealtimePublisherAdapter adapter = new SupabaseRealtimePublisherAdapter(
                WebClient.builder(),
                server.url("/").toString(),
                "sb_secret_test"
        );
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            adapter.publish("chat-room:" + UUID.randomUUID(), "message.created", Map.of("id", "message"));
            assertThat(server.getRequestCount()).isZero();

            TransactionSynchronizationUtils.triggerAfterCommit();

            assertThat(server.takeRequest().getPath()).contains("message.created");
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private SupabaseMediaStorageAdapter storageAdapter(String key) {
        return new SupabaseMediaStorageAdapter(
                mock(ProfileRepository.class),
                WebClient.builder(),
                server.url("/").toString(),
                key,
                "avatars",
                "ai-results",
                "chat-media",
                300,
                10 * 1024 * 1024
        );
    }
}
