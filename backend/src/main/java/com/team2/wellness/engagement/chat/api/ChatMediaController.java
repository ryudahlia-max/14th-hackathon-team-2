package com.team2.wellness.engagement.chat.api;

import com.team2.wellness.common.api.ApiException;
import com.team2.wellness.engagement.port.out.CurrentUserPort;
import com.team2.wellness.engagement.port.out.MediaStoragePort;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/engagement/media")
public class ChatMediaController {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final CurrentUserPort currentUser;
    private final MediaStoragePort storage;
    private final int maxImageBytes;

    public ChatMediaController(
            CurrentUserPort currentUser,
            MediaStoragePort storage,
            @Value("${app.supabase.storage.max-image-bytes:10485760}") int maxImageBytes
    ) {
        this.currentUser = currentUser;
        this.storage = storage;
        this.maxImageBytes = maxImageBytes;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    MediaUploadResponse upload(@RequestPart("file") MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MEDIA_TYPE", "이미지 파일만 업로드할 수 있습니다.");
        }
        if (file.getSize() > maxImageBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "이미지는 10MB 이하만 업로드할 수 있습니다.");
        }
        MediaStoragePort.StoredMedia stored = storage.storeChatMedia(
                currentUser.currentUserId(),
                file.getBytes(),
                contentType
        );
        return new MediaUploadResponse(stored.objectKey(), storage.resolveChatMedia(stored.objectKey()));
    }

    record MediaUploadResponse(String objectKey, String url) {
    }
}
