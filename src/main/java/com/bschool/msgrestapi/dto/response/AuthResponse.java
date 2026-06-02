package com.bschool.msgrestapi.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        Long userId,
        String email,
        String firstName,
        String lastName
) {
}
