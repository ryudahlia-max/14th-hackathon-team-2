package com.team2.wellness.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team2.wellness.engagement.ai.application.ImageGenerationException;
import com.team2.wellness.engagement.port.out.ImageGenerationPort;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class OpenAiImageAdapterTest {

    private MockWebServer server;
    private OpenAiImageAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        adapter = new OpenAiImageAdapter(
                WebClient.builder(),
                server.url("/").toString(),
                "test-api-key",
                "gpt-image-2",
                5
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void referenceImageUsesMultipartEditEndpoint() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"data\":[{\"b64_json\":\"AQID\"}]}"));

        ImageGenerationPort.ImageResult result = adapter.generate(new ImageGenerationPort.ImageCommand(
                "safe prompt",
                new byte[]{9, 8, 7},
                "image/png"
        ));

        assertThat(result.bytes()).containsExactly(1, 2, 3);
        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/v1/images/edits");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-api-key");
        assertThat(request.getBody().readUtf8())
                .contains("name=\"model\"")
                .contains("gpt-image-2")
                .contains("name=\"image[]\"");
    }

    @Test
    void promptOnlyUsesGenerationEndpoint() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"data\":[{\"b64_json\":\"AQI=\"}]}"));

        adapter.generate(new ImageGenerationPort.ImageCommand("recap prompt", new byte[0], "image/png"));

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/v1/images/generations");
        assertThat(request.getBody().readUtf8())
                .contains("\"model\":\"gpt-image-2\"")
                .doesNotContain("response_format");
    }

    @Test
    void rateLimitIsRetryableButValidationFailureIsNot() {
        server.enqueue(new MockResponse().setResponseCode(429));
        server.enqueue(new MockResponse().setResponseCode(400));
        ImageGenerationPort.ImageCommand command = new ImageGenerationPort.ImageCommand(
                "prompt",
                new byte[0],
                "image/png"
        );

        assertThatThrownBy(() -> adapter.generate(command))
                .isInstanceOfSatisfying(ImageGenerationException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("OPENAI_RATE_LIMIT");
                    assertThat(exception.retryable()).isTrue();
                });
        assertThatThrownBy(() -> adapter.generate(command))
                .isInstanceOfSatisfying(ImageGenerationException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("OPENAI_REJECTED");
                    assertThat(exception.retryable()).isFalse();
                });
    }
}
