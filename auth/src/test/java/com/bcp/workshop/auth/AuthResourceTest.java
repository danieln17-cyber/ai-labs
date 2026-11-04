package com.bcp.workshop.auth;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.Map;
import java.security.Signature;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@QuarkusTestResource(AuthJwtTestResource.class)
class AuthResourceTest {
    @Test
    void reportsHealthStatusWithoutAuthorizationHeader() {
        given()
                .accept("application/json")
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("expiresIn", is(3600))
                .body("debug", is("authenticated-with-legacy-flow"))
                .body("token", notNullValue());
    }

    @Test
    void rejectsInvalidCredentials() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Incorrecta123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user student or password"));
    }

    @Test
    void logsInWithWorkshopTrainer() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"trainer\",\"password\":\"Trainer123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("trainer"))
                .body("user.role", is("TRAINER"))
                .body("expiresIn", is(3600))
                .body("token", notNullValue());
    }

    @Test
    void rejectsUnknownUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"unknown-user\",\"password\":\"AnyPassword123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user unknown-user or password"));
    }

    @Test
    void rejectsEmptyUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":\" \",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsEmptyPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\" \"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void rejectsNullRequest() {
        given()
                .contentType("application/json")
                .body("null")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Request is null"));
    }

    @Test
    void rejectsNullUsername() {
        given()
                .contentType("application/json")
                .body("{\"username\":null,\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Username is required"));
    }

    @Test
    void rejectsNullPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":null}")
                .when().post("/auth/login")
                .then()
                .statusCode(400)
                .body("message", is("Password is required"));
    }

    @Test
    void issuesSignedJwtWithExpectedClaims() throws Exception {
        String token = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");
        String[] segments = token.split("\\.");
        assertEquals(3, segments.length);

        byte[] payloadBytes = Base64.getUrlDecoder().decode(segments[1]);
        JsonNode claims = new ObjectMapper().readTree(payloadBytes);
        assertEquals("student", claims.get("sub").asText());
        assertEquals("STUDENT", claims.get("groups").get(0).asText());
        assertEquals(3600, claims.get("exp").asLong() - claims.get("iat").asLong());
        assertTrue(hasValidSignature(token));

        byte[] tamperedSignature = Base64.getUrlDecoder().decode(segments[2]);
        tamperedSignature[0] ^= 1;
        String tamperedToken = segments[0] + "." + segments[1] + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(tamperedSignature);
        assertFalse(hasValidSignature(tamperedToken));
    }

    @Test
    void allowsConfiguredCorsOrigin() {
        given()
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .header("Access-Control-Allow-Origin", "http://localhost:4200");
    }

    @Test
    void rejectsUnconfiguredCorsOrigin() {
        given()
                .header("Origin", "https://unrelated.example")
                .header("Access-Control-Request-Method", "POST")
                .when().options("/auth/login")
                .then()
                .header("Access-Control-Allow-Origin", nullValue());
    }

    @Test
    void doesNotExposeInternalErrorDetails() {
        AuthResource resource = new AuthResource();
        resource.authService = new AuthService();
        resource.authService.database = new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                throw new IllegalStateException("fixture database failure");
            }
        };
        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = "Workshop123!";

        Response response = resource.login(request);
        Map<?, ?> body = (Map<?, ?>) response.getEntity();

        assertEquals(500, response.getStatus());
        assertEquals("Authentication failed", body.get("message"));
        assertFalse(body.containsKey("exception"));
        assertFalse(body.get("message").toString().contains("fixture database failure"));
    }

    private boolean hasValidSignature(String token) throws Exception {
        String[] segments = token.split("\\.");
        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(AuthJwtTestResource.verificationKey());
        verifier.update((segments[0] + "." + segments[1]).getBytes(StandardCharsets.US_ASCII));
        return verifier.verify(Base64.getUrlDecoder().decode(segments[2]));
    }

    @Test
    void migratesLegacyPlainTextPasswordStorage(@TempDir Path databaseDirectory) throws Exception {
        String databaseUrl = "jdbc:sqlite:" + databaseDirectory.resolve("legacy.db");
        try (Connection connection = DriverManager.getConnection(databaseUrl)) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE legacy_users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT NOT NULL, display_name TEXT NOT NULL, role TEXT NOT NULL)")) {
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO legacy_users(username, password, display_name, role) VALUES (?, ?, ?, ?)")) {
                statement.setString(1, "student");
                statement.setString(2, "Workshop123!");
                statement.setString(3, "Workshop Student");
                statement.setString(4, "STUDENT");
                statement.executeUpdate();
            }
        }

        LegacyDatabase database = new LegacyDatabase(databaseUrl);
        database.passwordHashService = new PasswordHashService();
        database.initialize();

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT password_hash FROM legacy_users WHERE username=?")) {
            statement.setString(1, "student");
            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next());
                String passwordHash = result.getString("password_hash");
                assertFalse(passwordHash.contains("Workshop123!"));
                assertTrue(passwordHash.startsWith(PasswordHashService.HASH_PREFIX));
            }
        }

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             ResultSet columns = connection.createStatement().executeQuery("PRAGMA table_info(legacy_users)")) {
            boolean plaintextPasswordColumnExists = false;
            while (columns.next()) {
                plaintextPasswordColumnExists |= columns.getString("name").equals("password");
            }
            assertFalse(plaintextPasswordColumnExists);
        }
    }

    @Test
    void rejectsUsernameContainingQuote() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student'\",\"password\":\"Workshop123!\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401)
                .body("message", is("Invalid user student' or password"));
    }
}
