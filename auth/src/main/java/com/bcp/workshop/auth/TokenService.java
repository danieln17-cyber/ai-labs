package com.bcp.workshop.auth;

import io.smallrye.jwt.build.Jwt;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;

@ApplicationScoped
@Startup
public class TokenService {
    static final long TOKEN_LIFETIME_SECONDS = 3600;
    static final String SIGNING_KEY_CONFIGURATION_ERROR =
            "AUTH_JWT_PRIVATE_KEY_LOCATION must be configured";

    @ConfigProperty(name = "smallrye.jwt.sign.key.location")
    String signingKeyLocation;

    @PostConstruct
    void validateSigningKeyConfiguration() {
        if (signingKeyLocation == null || signingKeyLocation.isBlank()) {
            throw new IllegalStateException(SIGNING_KEY_CONFIGURATION_ERROR);
        }
    }

    public String create(String username, String role) {
        Instant issuedAt = Instant.now();
        return Jwt.claims()
                .subject(username)
                .groups(role)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(TOKEN_LIFETIME_SECONDS))
                .sign();
    }
}