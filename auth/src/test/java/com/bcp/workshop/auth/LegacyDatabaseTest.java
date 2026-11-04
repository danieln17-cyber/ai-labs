package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LegacyDatabaseTest {
    @Test
    void initializesCleanDatabaseAndCanBeRepeated() throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path databasePath = Files.createTempFile(Path.of("target"), "h-auth-clean-", ".db");
        try {
            LegacyDatabase database = new LegacyDatabase("jdbc:sqlite:" + databasePath.toAbsolutePath());
            database.passwordHashService = new PasswordHashService();
            database.initialize();

            String initialPasswordHash = passwordHash(databasePath, "student");
            assertFalse(initialPasswordHash.contains("Workshop123!"));
            assertNotNull(database.findByCredentials("student", "Workshop123!"));
            assertNull(database.findByCredentials("student", "Incorrecta123!"));

            database.initialize();

            assertEquals(initialPasswordHash, passwordHash(databasePath, "student"));
            assertNotNull(database.findByCredentials("trainer", "Trainer123!"));
        } finally {
            Files.deleteIfExists(databasePath);
        }
    }

    private String passwordHash(Path databasePath, String username) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT password_hash FROM legacy_users WHERE username=?")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new AssertionError("Expected seeded account was missing");
                }
                return result.getString("password_hash");
            }
        }
    }
}
