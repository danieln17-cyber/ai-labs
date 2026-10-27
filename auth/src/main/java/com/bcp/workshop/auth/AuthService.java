package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Optional;

@ApplicationScoped
public class AuthService {
    private static final String DEBUG_FLOW = "authenticated-with-legacy-flow";

    private final AuthRepository repository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    @Inject
    public AuthService(AuthRepository repository, PasswordHasher passwordHasher, TokenService tokenService) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
    }

    public Optional<LoginResponse> authenticate(String username, String password) {
        return repository.findByUsername(username)
                .filter(user -> passwordHasher.verify(password, user.passwordHash()))
                .map(this::createResponse);
    }

    private LoginResponse createResponse(UserRecord user) {
        String token = tokenService.create(user.username(), user.role());
        UserResponse responseUser = new UserResponse(
                user.username(),
                StringEscapeUtils.escapeHtml4(user.displayName()),
                user.role());
        return new LoginResponse(token, TokenService.EXPIRES_IN_SECONDS, responseUser, DEBUG_FLOW);
    }
}
