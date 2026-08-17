package com.team2.wellness.infrastructure.supabase;

import org.springframework.http.HttpHeaders;

final class SupabaseApiHeaders {

    private SupabaseApiHeaders() {
    }

    static void authenticate(HttpHeaders headers, String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("SUPABASE_SECRET_KEY is not configured");
        }
        headers.set("apikey", secretKey);
        if (!secretKey.startsWith("sb_secret_")) {
            headers.setBearerAuth(secretKey);
        }
    }
}
