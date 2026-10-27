package com.bcp.workshop.auth;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SqliteAuthRepository implements AuthRepository {
    private static final Logger LOG = Logger.getLogger(SqliteAuthRepository.class);
    private static final String CREATE_USERS_TABLE = "CREATE TABLE IF NOT EXISTS legacy_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)";
    private static final String INSERT_USER = "INSERT OR IGNORE INTO legacy_users(username, password, display_name, role) VALUES (?, ?, ?, ?)";
    private static final String SELECT_PASSWORDS = "SELECT id, password FROM legacy_users";
    private static final String UPDATE_PASSWORD = "UPDATE legacy_users SET password = ? WHERE id = ?";
    private static final String FIND_USER_BY_USERNAME = "SELECT id, username, password, display_name, role FROM legacy_users WHERE username = ?";
    private static final SeedUser STUDENT = new SeedUser("student", "Workshop123!", "Workshop Student", "STUDENT");
    private static final SeedUser TRAINER = new SeedUser("trainer", "Trainer123!", "Workshop Trainer", "TRAINER");

    private final String databaseUrl;
    private final PasswordHasher passwordHasher;

    @Inject
    public SqliteAuthRepository(
            @ConfigProperty(name = "auth.db.url", defaultValue = "jdbc:sqlite:./data/workshop.db") String databaseUrl,
            PasswordHasher passwordHasher) {
        this.databaseUrl = databaseUrl;
        this.passwordHasher = passwordHasher;
    }

    @PostConstruct
    void initialize() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection connection = DriverManager.getConnection(databaseUrl);
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate(CREATE_USERS_TABLE);
                insertSeedUser(connection, STUDENT);
                insertSeedUser(connection, TRAINER);
                migratePlaintextPasswords(connection);
            }
        } catch (Exception error) {
            LOG.error("Unable to initialize authentication storage", error);
            throw new IllegalStateException("Unable to initialize authentication storage", error);
        }
    }

    @Override
    public Optional<UserRecord> findByUsername(String username) {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(FIND_USER_BY_USERNAME)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapUser(result));
            }
        } catch (Exception error) {
            throw new IllegalStateException("Unable to query authentication storage", error);
        }
    }

    private UserRecord mapUser(ResultSet result) throws Exception {
        return new UserRecord(
                result.getLong("id"),
                result.getString("username"),
                result.getString("password"),
                result.getString("display_name"),
                result.getString("role"));
    }

    private void insertSeedUser(Connection connection, SeedUser user) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_USER)) {
            statement.setString(1, user.username());
            statement.setString(2, passwordHasher.hash(user.password()));
            statement.setString(3, user.displayName());
            statement.setString(4, user.role());
            statement.executeUpdate();
        }
    }

    private void migratePlaintextPasswords(Connection connection) throws Exception {
        for (StoredPassword storedPassword : findPlaintextPasswords(connection)) {
            updatePassword(connection, storedPassword);
        }
    }

    private List<StoredPassword> findPlaintextPasswords(Connection connection) throws Exception {
        List<StoredPassword> plaintextPasswords = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(SELECT_PASSWORDS);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                String storedPassword = result.getString("password");
                if (!passwordHasher.isHash(storedPassword)) {
                    plaintextPasswords.add(new StoredPassword(result.getLong("id"), storedPassword));
                }
            }
        }
        return plaintextPasswords;
    }

    private void updatePassword(Connection connection, StoredPassword storedPassword) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_PASSWORD)) {
            statement.setString(1, passwordHasher.hash(storedPassword.plaintext()));
            statement.setLong(2, storedPassword.userId());
            statement.executeUpdate();
        }
    }

    private record SeedUser(String username, String password, String displayName, String role) {}

    private record StoredPassword(long userId, String plaintext) {}
}
