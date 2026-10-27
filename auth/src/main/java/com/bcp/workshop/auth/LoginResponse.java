package com.bcp.workshop.auth;

public record LoginResponse(String token, int expiresIn, UserResponse user, String debug) {}
