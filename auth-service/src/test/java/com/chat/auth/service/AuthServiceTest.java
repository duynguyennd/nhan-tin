package com.chat.auth.service;

import com.chat.auth.dto.AuthDtos.AuthResponse;
import com.chat.auth.dto.AuthDtos.LoginRequest;
import com.chat.auth.dto.AuthDtos.RefreshRequest;
import com.chat.auth.dto.AuthDtos.RegisterRequest;
import com.chat.auth.exception.AuthExceptions.DuplicateUserException;
import com.chat.auth.exception.AuthExceptions.InvalidCredentialsException;
import com.chat.auth.exception.AuthExceptions.InvalidTokenException;
import com.chat.auth.security.JwtService;
import com.chat.auth.user.User;
import com.chat.auth.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = new JwtService("dev-only-secret-key-change-me-0123456789abcdef0123456789abcdef", 15, 7);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, null);
    }

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest("newuser", "new@test.com", "pass123");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AuthResponse resp = authService.register(req);

        assertNotNull(resp);
        assertEquals("newuser", resp.username());
        assertNotNull(resp.accessToken());
        assertNotNull(resp.refreshToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_duplicateUsername_throwsDuplicateUserException() {
        RegisterRequest req = new RegisterRequest("existing", "new@test.com", "pass123");
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_throwsDuplicateUserException() {
        RegisterRequest req = new RegisterRequest("newuser", "existing@test.com", "pass123");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest("testuser", "pass123");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        user.setPasswordHash("hashedPass");

        when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass123", "hashedPass")).thenReturn(true);

        AuthResponse resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("testuser", resp.username());
        assertNotNull(resp.accessToken());
    }

    @Test
    void login_invalidUser_throwsInvalidCredentialsException() {
        LoginRequest req = new LoginRequest("unknown", "pass123");
        when(userRepository.findByUsernameOrEmail("unknown", "unknown")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void login_invalidPassword_throwsInvalidCredentialsException() {
        LoginRequest req = new LoginRequest("testuser", "wrongpass");
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash("hashedPass");

        when(userRepository.findByUsernameOrEmail("testuser", "testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashedPass")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void refresh_success() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");

        String refreshToken = jwtService.generateRefreshToken(user);
        RefreshRequest req = new RefreshRequest(refreshToken);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AuthResponse resp = authService.refresh(req);

        assertNotNull(resp);
        assertNotNull(resp.accessToken());
        assertNotNull(resp.refreshToken());
    }

    @Test
    void refresh_invalidToken_throwsInvalidTokenException() {
        RefreshRequest req = new RefreshRequest("invalid_token");
        assertThrows(InvalidTokenException.class, () -> authService.refresh(req));
    }

    @Test
    void refresh_wrongTokenType_throwsInvalidTokenException() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        String accessToken = jwtService.generateAccessToken(user);
        RefreshRequest req = new RefreshRequest(accessToken);

        assertThrows(InvalidTokenException.class, () -> authService.refresh(req));
    }

    @Test
    void login_bannedUser_throwsInvalidCredentialsException() {
        LoginRequest req = new LoginRequest("banned", "pass123");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("banned");
        user.setPasswordHash("hashedPass");
        user.setBanned(true);

        when(userRepository.findByUsernameOrEmail("banned", "banned")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void getProfile_success() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("profileuser");
        user.setEmail("profile@test.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var profile = authService.getProfile(userId);

        assertNotNull(profile);
        assertEquals(userId, profile.id());
        assertEquals("profileuser", profile.username());
        assertEquals("profile@test.com", profile.email());
    }

    @Test
    void getProfile_notFound_throwsUserNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(com.chat.auth.exception.AuthExceptions.UserNotFoundException.class,
                () -> authService.getProfile(userId));
    }

    @Test
    void updateProfile_duplicateUsername_throwsDuplicateUserException() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("oldname");
        user.setEmail("old@test.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        var request = new com.chat.auth.dto.AuthDtos.UpdateProfileRequest("taken", null);

        assertThrows(DuplicateUserException.class, () -> authService.updateProfile(userId, request));
    }

    @Test
    void getAllUsers_returnsList() {
        User u1 = new User();
        u1.setId(UUID.randomUUID());
        u1.setUsername("alice");
        u1.setEmail("alice@test.com");

        User u2 = new User();
        u2.setId(UUID.randomUUID());
        u2.setUsername("bob");
        u2.setEmail("bob@test.com");

        when(userRepository.findAll()).thenReturn(java.util.List.of(u1, u2));

        var users = authService.getAllUsers();

        assertNotNull(users);
        assertEquals(2, users.size());
        assertEquals("alice", users.get(0).username());
        assertEquals("bob", users.get(1).username());
    }
}
