package com.bcp.workshop.auth;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthErrorResponseTest {
    @Test
    void hidesInternalExceptionDetails() {
        AuthResource resource = new AuthResource();
        resource.authService = new FailingAuthService();
        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = "Workshop123!";

        Response response = resource.login(request);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getEntity();

        assertEquals(500, response.getStatus());
        assertEquals("Authentication service unavailable", body.get("message"));
        assertFalse(body.containsKey("exception"));
        assertFalse(body.toString().contains("database-details"));
    }

    private static class FailingAuthService extends AuthService {
        private FailingAuthService() {
            super(null, null, null);
        }

        @Override
        public java.util.Optional<LoginResponse> authenticate(String username, String password) {
            throw new IllegalStateException("database-details");
        }
    }
}
