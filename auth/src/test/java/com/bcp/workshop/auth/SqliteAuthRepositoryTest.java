package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteAuthRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void loadSqliteDriver() throws ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
    }

    @Test
    void initializesHashedUsersAndFindsByParameterizedUsername() {
        PasswordHasher hasher = new PasswordHasher();
        SqliteAuthRepository repository = repositoryAt(temporaryDirectory.resolve("auth.db"), hasher);

        repository.initialize();

        UserRecord student = repository.findByUsername("student").orElseThrow();
        assertTrue(hasher.verify("Workshop123!", student.passwordHash()));
        assertFalse(student.passwordHash().contains("Workshop123!"));
        assertTrue(repository.findByUsername("fictitious'user").isEmpty());
        assertTrue(repository.findByUsername("unknown-user").isEmpty());
    }

    @Test
    void reportsStorageFailuresWithoutReturningAnInvalidUser() {
        Path directoryInsteadOfDatabase = temporaryDirectory.resolve("database-directory");
        directoryInsteadOfDatabase.toFile().mkdirs();
        SqliteAuthRepository repository = repositoryAt(directoryInsteadOfDatabase, new PasswordHasher());

        assertThrows(IllegalStateException.class, () -> repository.findByUsername("student"));
    }

    private SqliteAuthRepository repositoryAt(Path databasePath, PasswordHasher hasher) {
        return new SqliteAuthRepository("jdbc:sqlite:" + databasePath.toAbsolutePath(), hasher);
    }
}
