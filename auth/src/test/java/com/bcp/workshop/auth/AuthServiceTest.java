package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    private static final String PASSWORD = "Workshop123!";

    @Test
    void authenticatesThroughRepositoryAndBuildsResponse() {
        PasswordHasher hasher = new PasswordHasher();
        UserRecord storedUser = new UserRecord(1, "student", hasher.hash(PASSWORD), "Workshop Student", "STUDENT");
        AuthRepository repository = username -> Optional.of(storedUser);
        AuthService service = new AuthService(repository, hasher, tokenService());

        Optional<LoginResponse> response = service.authenticate("student", PASSWORD);

        assertTrue(response.isPresent());
        assertEquals("student", response.orElseThrow().user().username());
        assertEquals(TokenService.EXPIRES_IN_SECONDS, response.orElseThrow().expiresIn());
    }

    @Test
    void rejectsIncorrectPassword() {
        PasswordHasher hasher = new PasswordHasher();
        UserRecord storedUser = new UserRecord(1, "student", hasher.hash(PASSWORD), "Workshop Student", "STUDENT");
        AuthRepository repository = username -> Optional.of(storedUser);
        AuthService service = new AuthService(repository, hasher, tokenService());

        assertTrue(service.authenticate("student", "wrong-password").isEmpty());
    }

    private TokenService tokenService() {
        return new TokenService(
                "test-only-signing-key-with-at-least-32-bytes",
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC));
    }
}
