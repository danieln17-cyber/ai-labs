package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void hashesAndVerifiesPasswordWithoutStoringPlaintext() {
        String encodedHash = passwordHasher.hash("Workshop123!");

        assertTrue(passwordHasher.verify("Workshop123!", encodedHash));
        assertFalse(passwordHasher.verify("wrong-password", encodedHash));
        assertFalse(encodedHash.contains("Workshop123!"));
    }

    @Test
    void usesDifferentSaltForEachHash() {
        String firstHash = passwordHasher.hash("Workshop123!");
        String secondHash = passwordHasher.hash("Workshop123!");

        assertNotEquals(firstHash, secondHash);
    }
}
