package com.team2.wellness.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class SecurityConfigTest {

    @Test
    void decodesSupabaseEs256AccessToken() throws Exception {
        String issuer = "https://example.supabase.co/auth/v1";
        String subject = UUID.randomUUID().toString();
        ECKey signingKey = new ECKeyGenerator(Curve.P_256)
                .keyID("supabase-es256-test-key")
                .generate();

        try (MockWebServer jwksServer = new MockWebServer()) {
            jwksServer.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(new JWKSet(signingKey.toPublicJWK()).toString()));
            jwksServer.start();

            JwtDecoder decoder = new SecurityConfig().jwtDecoder(
                    issuer,
                    jwksServer.url("/.well-known/jwks.json").toString(),
                    "authenticated"
            );
            SignedJWT token = signedToken(signingKey, issuer, subject);

            Jwt decoded = decoder.decode(token.serialize());

            assertThat(decoded.getSubject()).isEqualTo(subject);
            assertThat(decoded.getAudience()).containsExactly("authenticated");
        }
    }

    private SignedJWT signedToken(ECKey signingKey, String issuer, String subject) throws Exception {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience("authenticated")
                .subject(subject)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(300)))
                .build();
        SignedJWT token = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256)
                        .keyID(signingKey.getKeyID())
                        .type(JOSEObjectType.JWT)
                        .build(),
                claims
        );
        token.sign(new ECDSASigner(signingKey));
        return token;
    }
}
