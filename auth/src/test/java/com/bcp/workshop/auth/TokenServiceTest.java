package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokenServiceTest {
    @Test
    void rejectsMissingSigningKeyConfiguration() {
        TokenService tokenService = new TokenService();

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                tokenService::validateSigningKeyConfiguration);

        assertEquals(TokenService.SIGNING_KEY_CONFIGURATION_ERROR, error.getMessage());
    }

    @Test
    void acceptsConfiguredSigningKeyLocation() {
        TokenService tokenService = new TokenService();
        tokenService.signingKeyLocation = "target/test-private-key.pem";

        assertDoesNotThrow(tokenService::validateSigningKeyConfiguration);
    }
}
