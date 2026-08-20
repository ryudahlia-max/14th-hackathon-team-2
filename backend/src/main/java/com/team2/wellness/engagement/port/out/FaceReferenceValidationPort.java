package com.team2.wellness.engagement.port.out;

/** Checks whether an image contains enough real facial detail to preserve a person's identity. */
public interface FaceReferenceValidationPort {

    boolean isUsableIdentityReference(byte[] image, String contentType);
}
