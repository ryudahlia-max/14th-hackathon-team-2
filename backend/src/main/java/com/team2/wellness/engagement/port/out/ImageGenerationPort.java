package com.team2.wellness.engagement.port.out;

public interface ImageGenerationPort {
    ImageResult generate(ImageCommand command);
    record ImageCommand(String prompt, byte[] referenceImage, String contentType) { }
    record ImageResult(byte[] bytes, String contentType) { }
}
