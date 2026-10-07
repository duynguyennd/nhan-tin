package com.chat.auth.controller;

import com.chat.auth.dto.AuthDtos.AuthResponse;
import com.chat.auth.dto.AuthDtos.LoginRequest;
import com.chat.auth.dto.AuthDtos.RefreshRequest;
import com.chat.auth.dto.AuthDtos.RegisterRequest;
import com.chat.auth.dto.AuthDtos.UserProfile;
import com.chat.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/auth/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/users/{id}")
    public UserProfile getProfile(@PathVariable UUID id) {
        return authService.getProfile(id);
    }

    @org.springframework.web.bind.annotation.PutMapping("/users/{id}")
    public UserProfile updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody com.chat.auth.dto.AuthDtos.UpdateProfileRequest request) {
        return authService.updateProfile(id, request);
    }

    @GetMapping("/users")
    public List<UserProfile> getAllUsers() {
        return authService.getAllUsers();
    }
}
