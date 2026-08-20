package com.team2.wellness.infrastructure.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team2.wellness.engagement.ai.application.ImageGenerationException;
import com.team2.wellness.engagement.port.out.ImageGenerationPort;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
@Primary
public class OpenAiImageAdapter implements ImageGenerationPort {

    private final WebClient client;
    private final String apiKey;
    private final String model;
    private final Duration timeout;

    public OpenAiImageAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${app.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${app.openai.api-key:}") String apiKey,
            @Value("${app.openai.image-model:gpt-image-2}") String model,
            @Value("${app.openai.timeout-seconds:90}") long timeoutSeconds
    ) {
        this.client = webClientBuilder
                .baseUrl(stripTrailingSlash(baseUrl))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(25 * 1024 * 1024))
                .build();
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public ImageResult generate(ImageCommand command) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ImageGenerationException("OPENAI_API_KEY_MISSING", false);
        }
        if (command == null || command.prompt() == null || command.prompt().isBlank()) {
            throw new ImageGenerationException("OPENAI_PROMPT_MISSING", false);
        }

        try {
            OpenAiImageResponse response = hasReferenceImage(command)
                    ? edit(command)
                    : generateFromPrompt(command.prompt());
            if (response == null || response.data() == null || response.data().length == 0
                    || response.data()[0].base64Json() == null) {
                throw new ImageGenerationException("OPENAI_EMPTY_RESPONSE");
            }
            return new ImageResult(
                    Base64.getDecoder().decode(response.data()[0].base64Json()),
                    "image/png"
            );
        } catch (ImageGenerationException exception) {
            throw exception;
        } catch (WebClientRequestException exception) {
            throw new ImageGenerationException("OPENAI_NETWORK_ERROR");
        } catch (IllegalArgumentException exception) {
            throw new ImageGenerationException("OPENAI_INVALID_RESPONSE");
        }
    }

    private OpenAiImageResponse generateFromPrompt(String prompt) {
        return client.post()
                .uri("/v1/images/generations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new GenerationRequest(model, prompt, "1024x1024"))
                .retrieve()
                .onStatus(status -> status.value() == 429, this::classifyRateLimit)
                .onStatus(status -> status.is5xxServerError(), response -> retryable("OPENAI_SERVER_ERROR"))
                .onStatus(status -> status.is4xxClientError(), response -> nonRetryable("OPENAI_REJECTED"))
                .bodyToMono(OpenAiImageResponse.class)
                .block(timeout);
    }

    private OpenAiImageResponse edit(ImageCommand command) {
        MultipartBodyBuilder multipart = new MultipartBodyBuilder();
        multipart.part("model", model);
        multipart.part("prompt", command.prompt());
        multipart.part("size", "1024x1024");
        multipart.part("image[]", new NamedByteArrayResource(command.referenceImage()))
                .filename("reference" + extension(command.contentType()))
                .contentType(safeMediaType(command.contentType()));

        return client.post()
                .uri("/v1/images/edits")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipart.build()))
                .retrieve()
                .onStatus(status -> status.value() == 429, this::classifyRateLimit)
                .onStatus(status -> status.is5xxServerError(), response -> retryable("OPENAI_SERVER_ERROR"))
                .onStatus(status -> status.is4xxClientError(), response -> nonRetryable("OPENAI_REJECTED"))
                .bodyToMono(OpenAiImageResponse.class)
                .block(timeout);
    }

    private boolean hasReferenceImage(ImageCommand command) {
        return command.referenceImage() != null && command.referenceImage().length > 0;
    }

    private Mono<? extends Throwable> retryable(String code) {
        return Mono.just(new ImageGenerationException(code, true));
    }

    private Mono<? extends Throwable> nonRetryable(String code) {
        return Mono.just(new ImageGenerationException(code, false));
    }

    private Mono<? extends Throwable> classifyRateLimit(ClientResponse response) {
        return response.bodyToMono(OpenAiErrorResponse.class)
                .defaultIfEmpty(new OpenAiErrorResponse(null))
                .map(body -> {
                    OpenAiError error = body.error();
                    String code = error == null ? null : error.code();
                    String message = error == null ? null : error.message();
                    if (requiresBilling(code, message)) {
                        return new ImageGenerationException("OPENAI_BILLING_REQUIRED", false);
                    }
                    return new ImageGenerationException("OPENAI_RATE_LIMIT", true);
                });
    }

    private boolean requiresBilling(String code, String message) {
        if (code != null && (code.equals("credit_balance_exhausted")
                || code.equals("organization_spend_limit_exceeded")
                || code.equals("project_spend_limit_exceeded")
                || code.equals("organization_usage_limit_exceeded")
                || code.equals("insufficient_quota"))) {
            return true;
        }
        if (message == null) return false;
        String normalized = message.toLowerCase();
        return normalized.contains("limit 0")
                || normalized.contains("payment method")
                || normalized.contains("billing");
    }

    private MediaType safeMediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (RuntimeException exception) {
            throw new ImageGenerationException("OPENAI_INVALID_REFERENCE_TYPE", false);
        }
    }

    private String extension(String contentType) {
        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return ".jpg";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return ".webp";
        }
        return ".png";
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record GenerationRequest(String model, String prompt, String size) {
    }

    private record OpenAiImageResponse(ImageData[] data) {
    }

    private record ImageData(@JsonProperty("b64_json") String base64Json) {
    }

    private record OpenAiErrorResponse(OpenAiError error) {
    }

    private record OpenAiError(String message, String type, String code) {
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private NamedByteArrayResource(byte[] bytes) {
            super(bytes);
        }

        @Override
        public String getFilename() {
            return "reference";
        }
    }
}
