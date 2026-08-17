package com.team2.wellness.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

    private final AudienceValidator validator = new AudienceValidator("authenticated");

    @Test
    void acceptsRequiredAudience() {
        Jwt jwt = jwt(List.of("authenticated"));

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void rejectsMissingAudience() {
        Jwt jwt = jwt(List.of("anon"));

        OAuth2TokenValidatorResult result = validator.validate(jwt);
        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt jwt(List<String> audience) {
        Instant now = Instant.now();
        return new Jwt(
                "token",
                now,
                now.plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("sub", "00000000-0000-0000-0000-000000000001", "aud", audience)
        );
    }
}
