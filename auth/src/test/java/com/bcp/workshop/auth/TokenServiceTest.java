package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {
    private static final String TEST_SECRET = "test-only-signing-key-with-at-least-32-bytes";
    private static final Instant NOW = Instant.parse("2026-10-26T12:00:00Z");

    @Test
    void createsAndVerifiesSignedClaims() {
        TokenService tokenService = tokenServiceAt(NOW);

        String token = tokenService.create("student", "STUDENT");
        TokenService.TokenClaims claims = tokenService.verify(token);

        assertEquals("student", claims.username());
        assertEquals("STUDENT", claims.role());
        assertEquals(NOW.getEpochSecond(), claims.issuedAt());
        assertEquals(NOW.plusSeconds(TokenService.EXPIRES_IN_SECONDS).getEpochSecond(), claims.expiresAt());
    }

    @Test
    void rejectsAlteredToken() {
        TokenService tokenService = tokenServiceAt(NOW);
        String token = tokenService.create("student", "STUDENT");

        assertThrows(SecurityException.class, () -> tokenService.verify(token + "changed"));
    }

    @Test
    void rejectsExpiredToken() {
        TokenService issuer = tokenServiceAt(NOW);
        String token = issuer.create("student", "STUDENT");
        TokenService verifier = tokenServiceAt(NOW.plusSeconds(TokenService.EXPIRES_IN_SECONDS + 1));

        assertThrows(SecurityException.class, () -> verifier.verify(token));
    }

    @Test
    void rejectsMalformedTokenAndEncoding() {
        TokenService tokenService = tokenServiceAt(NOW);

        assertThrows(SecurityException.class, () -> tokenService.verify("not-a-token"));
        assertThrows(SecurityException.class, () -> tokenService.verify("header.payload.%"));
    }

    @Test
    void rejectsShortSigningKey() {
        assertThrows(IllegalArgumentException.class,
                () -> new TokenService("too-short", new ObjectMapper(), Clock.systemUTC()));
    }

    private TokenService tokenServiceAt(Instant instant) {
        return new TokenService(TEST_SECRET, new ObjectMapper(), Clock.fixed(instant, ZoneOffset.UTC));
    }
}
