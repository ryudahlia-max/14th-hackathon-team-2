package com.team2.wellness.engagement.port.out;

import java.util.Optional;
import java.util.UUID;

/** Boundary for Storage; adapters must not leak provider-specific objects into engagement. */
public interface MediaStoragePort {
    Optional<StoredMedia> findFaceAsset(UUID userId);
    byte[] read(String objectKey);
    StoredMedia storeAiOutput(UUID ownerId, byte[] bytes, String contentType);
    String temporaryDownloadUrl(String objectKey);

    record StoredMedia(String objectKey, String contentType) { }
}
