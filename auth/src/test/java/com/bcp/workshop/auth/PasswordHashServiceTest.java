package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashServiceTest {
    private static final int TEST_ITERATION_COUNT = 210_000;

    private final PasswordHashService passwordHashService = new PasswordHashService();

    @Test
    void hashesUseDifferentSaltsAndMatchOnlyTheirPassword() {
        String firstHash = passwordHashService.hash("fixture-password");
        String secondHash = passwordHashService.hash("fixture-password");

        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordHashService.matches("fixture-password", firstHash));
        assertFalse(passwordHashService.matches("incorrect-password", firstHash));
    }

    @Test
    void rejectsNullAndMalformedHashes() {
        assertFalse(passwordHashService.matches(null, "$pbkdf2-sha256$"));
        assertFalse(passwordHashService.matches("fixture-password", null));
        assertFalse(passwordHashService.matches("fixture-password", "not-a-hash"));
        assertFalse(passwordHashService.matches("fixture-password", "$pbkdf2-sha1$1$AA==$AA=="));
        assertFalse(passwordHashService.matches(
                "fixture-password",
                "$pbkdf2-sha256$not-a-number$AA==$AA=="));
        assertFalse(passwordHashService.matches(
                "fixture-password",
                "$pbkdf2-sha256$1$AA==$AA=="));
        assertFalse(passwordHashService.matches(
                "fixture-password",
                "$pbkdf2-sha256$" + TEST_ITERATION_COUNT + "$!$!"));
    }
}
