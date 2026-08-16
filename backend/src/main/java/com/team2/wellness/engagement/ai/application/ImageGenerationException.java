package com.team2.wellness.engagement.ai.application;

public class ImageGenerationException extends RuntimeException {

    private final String code;
    private final boolean retryable;

    public ImageGenerationException(String code) {
        this(code, true);
    }

    public ImageGenerationException(String code, boolean retryable) {
        super(code);
        this.code = code;
        this.retryable = retryable;
    }

    public String code() {
        return code;
    }

    public boolean retryable() {
        return retryable;
    }
}
