package com.team2.wellness.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team2.wellness.engagement.ai.application.ImageGenerationException;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class OpenAiFaceReferenceValidationAdapterTest {

    private MockWebServer server;
    private OpenAiFaceReferenceValidationAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        adapter = new OpenAiFaceReferenceValidationAdapter(
                WebClient.builder(),
                server.url("/").toString(),
                "test-api-key",
                "gpt-4o-mini",
                5
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void clearSingleFaceIsAcceptedAndCachedByImageHash() throws InterruptedException {
        server.enqueue(response("VALID"));

        assertThat(adapter.isUsableIdentityReference(new byte[]{1, 2, 3}, "image/png")).isTrue();
        assertThat(adapter.isUsableIdentityReference(new byte[]{1, 2, 3}, "image/png")).isTrue();

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/v1/responses");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-api-key");
        assertThat(request.getBody().readUtf8())
                .contains("\"model\":\"gpt-4o-mini\"")
                .contains("data:image/png;base64,AQID")
                .contains("silhouettes");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void silhouetteVerdictIsRejected() {
        server.enqueue(response("INVALID"));

        assertThat(adapter.isUsableIdentityReference(new byte[]{4, 5, 6}, "image/jpeg")).isFalse();
    }

    @Test
    void ambiguousProviderResponseFailsClosed() {
        server.enqueue(response("MAYBE"));

        assertThatThrownBy(() -> adapter.isUsableIdentityReference(new byte[]{7}, "image/webp"))
                .isInstanceOfSatisfying(ImageGenerationException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("OPENAI_REFERENCE_CHECK_INVALID_RESPONSE");
                    assertThat(exception.retryable()).isFalse();
                });
    }

    @Test
    void placeholdersAndUnsupportedFilesFailWithoutProviderCall() {
        assertThat(adapter.isUsableIdentityReference(new byte[0], "image/png")).isFalse();
        assertThat(adapter.isUsableIdentityReference(new byte[]{1}, "image/gif")).isFalse();
        assertThat(server.getRequestCount()).isZero();
    }

    private MockResponse response(String verdict) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"output":[{"content":[{"type":"output_text","text":"%s"}]}]}
                        """.formatted(verdict));
    }
}
