package com.team2.wellness.core.profile;

import java.util.UUID;

public interface AvatarStoragePort {

    StoredAvatar storeAvatar(UUID ownerId, byte[] bytes, String contentType);

    String temporaryDownloadUrl(String objectKey);

    record StoredAvatar(String objectKey, String contentType) {
    }
}
