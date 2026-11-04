package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.text.StringEscapeUtils;

import java.sql.SQLException;

@ApplicationScoped
public class AuthService {
    private static final String DEBUG_FLOW = "authenticated-with-legacy-flow";

    @Inject
    LegacyDatabase database;

    @Inject
    TokenService tokenService;

    public AuthenticationResult authenticate(String username, String password) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return AuthenticationResult.failure(Failure.MISSING_USERNAME, "Username is required");
        }
        if (password == null || password.trim().isEmpty()) {
            return AuthenticationResult.failure(Failure.MISSING_PASSWORD, "Password is required");
        }

        UserRecord user = database.findByCredentials(username, password);
        if (user == null) {
            return AuthenticationResult.failure(
                    Failure.INVALID_CREDENTIALS,
                    "Invalid user " + username + " or password");
        }

        return AuthenticationResult.success(toLoginResponse(user));
    }

    private LoginResponse toLoginResponse(UserRecord user) {
        LoginResponse.User responseUser = new LoginResponse.User(
                user.username(),
                StringEscapeUtils.escapeHtml4(user.displayName()),
                user.role());
        return new LoginResponse(
                tokenService.create(user.username(), user.role()),
                TokenService.TOKEN_LIFETIME_SECONDS,
                responseUser,
                DEBUG_FLOW);
    }

    public enum Failure {
        NONE,
        MISSING_USERNAME,
        MISSING_PASSWORD,
        INVALID_CREDENTIALS
    }

    public record AuthenticationResult(Failure failure, String message, LoginResponse response) {
        private static AuthenticationResult failure(Failure failure, String message) {
            return new AuthenticationResult(failure, message, null);
        }

        private static AuthenticationResult success(LoginResponse response) {
            return new AuthenticationResult(Failure.NONE, null, response);
        }
    }
}
