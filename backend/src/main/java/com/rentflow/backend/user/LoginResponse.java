package com.rentflow.backend.user;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresIn,
    Long userId,
    String fullName,
    String email,
    Role role
) {}
