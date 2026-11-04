package com.bcp.workshop.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class LegacyDatabase {
    private static final String DEFAULT_DB_URL =
            System.getenv().getOrDefault("AUTH_DB_URL", "jdbc:sqlite:./data/workshop.db");
    private static final String CREATE_USERS_TABLE = "CREATE TABLE legacy_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password_hash TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)";
    private static final String STUDENT_USERNAME = "student";
    private static final String STUDENT_PASSWORD = "Workshop123!";
    private static final String TRAINER_USERNAME = "trainer";
    private static final String TRAINER_PASSWORD = "Trainer123!";

    private final String databaseUrl;

    @Inject
    PasswordHashService passwordHashService;

    public LegacyDatabase() {
        this(DEFAULT_DB_URL);
    }

    LegacyDatabase(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection connection = DriverManager.getConnection(databaseUrl)) {
                connection.setAutoCommit(false);
                try {
                    migrateLegacySchema(connection);
                    insertSeedUser(connection, STUDENT_USERNAME, STUDENT_PASSWORD, "Workshop Student", "STUDENT");
                    insertSeedUser(connection, TRAINER_USERNAME, TRAINER_PASSWORD, "Workshop Trainer", "TRAINER");
                    connection.commit();
                } catch (SQLException | RuntimeException error) {
                    connection.rollback();
                    throw error;
                }
            }
        } catch (IOException | SQLException error) {
            throw new IllegalStateException("Unable to initialize authentication database", error);
        }
    }

    private void migrateLegacySchema(Connection connection) throws SQLException {
        if (!tableExists(connection)) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(CREATE_USERS_TABLE);
            }
            return;
        }

        Set<String> columns = getUserColumns(connection);
        if (!columns.contains("password")) {
            if (columns.contains("password_hash")) {
                return;
            }
            throw new IllegalStateException("legacy_users has no password column to migrate");
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS legacy_users_migration");
            statement.executeUpdate(CREATE_USERS_TABLE.replace("legacy_users", "legacy_users_migration"));
        }

        String passwordColumn = columns.contains("password_hash") ? "password_hash" : "password";
        String selectSql = "SELECT id, username, " + passwordColumn + ", display_name, role FROM legacy_users";
        String insertSql = "INSERT INTO legacy_users_migration(id, username, password_hash, display_name, role) VALUES (?, ?, ?, ?, ?)";
        try (Statement query = connection.createStatement();
             ResultSet users = query.executeQuery(selectSql);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {
            while (users.next()) {
                String storedPassword = users.getString(passwordColumn);
                String passwordHash = columns.contains("password_hash")
                        && storedPassword != null
                        && storedPassword.startsWith(PasswordHashService.HASH_PREFIX)
                        ? storedPassword
                        : passwordHashService.hash(storedPassword);
                insert.setLong(1, users.getLong("id"));
                insert.setString(2, users.getString("username"));
                insert.setString(3, passwordHash);
                insert.setString(4, users.getString("display_name"));
                insert.setString(5, users.getString("role"));
                insert.executeUpdate();
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE legacy_users");
            statement.executeUpdate("ALTER TABLE legacy_users_migration RENAME TO legacy_users");
        }
    }

    private boolean tableExists(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?")) {
            statement.setString(1, "legacy_users");
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private Set<String> getUserColumns(Connection connection) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(legacy_users)")) {
            while (result.next()) {
                columns.add(result.getString("name"));
            }
        }
        return columns;
    }

    private void insertSeedUser(Connection connection, String username, String password,
                                String displayName, String role) throws SQLException {
        try (PreparedStatement lookup = connection.prepareStatement(
                "SELECT 1 FROM legacy_users WHERE username=?")) {
            lookup.setString(1, username);
            try (ResultSet result = lookup.executeQuery()) {
                if (result.next()) {
                    return;
                }
            }
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO legacy_users(username, password_hash, display_name, role) VALUES (?, ?, ?, ?)")) {
            insert.setString(1, username);
            insert.setString(2, passwordHashService.hash(password));
            insert.setString(3, displayName);
            insert.setString(4, role);
            insert.executeUpdate();
        }
    }

    public UserRecord findByCredentials(String username, String password) throws SQLException {
        String sql = "SELECT id, username, display_name, role, password_hash FROM legacy_users WHERE username=?";
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !passwordHashService.matches(password, result.getString("password_hash"))) {
                    return null;
                }
                return new UserRecord(result.getLong("id"), result.getString("username"), result.getString("display_name"), result.getString("role"));
            }
        }
    }
}
