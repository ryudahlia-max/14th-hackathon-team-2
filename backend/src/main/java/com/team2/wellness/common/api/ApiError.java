package com.team2.wellness.common.api;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, String> fieldErrors,
        String traceId,
        Instant timestamp
) {
}
