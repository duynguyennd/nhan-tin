package com.chat.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password) {
    }

    public record LoginRequest(
            @NotBlank String usernameOrEmail,
            @NotBlank String password) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            UUID userId,
            String username,
            String role,
            long expiresInSeconds) {
    }

    public record UserProfile(
            UUID id,
            String username,
            String email,
            String role,
            boolean banned,
            Instant createdAt) {
    }

    public record UpdateProfileRequest(
            @Size(min = 3, max = 50) String username,
            @Email String email) {
    }
}
