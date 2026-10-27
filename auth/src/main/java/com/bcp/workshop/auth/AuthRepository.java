package com.bcp.workshop.auth;

import java.util.Optional;

public interface AuthRepository {
    Optional<UserRecord> findByUsername(String username);
}
