package com.chat.auth.security;

import com.chat.auth.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "dev-only-secret-key-change-me-0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, 60, 7);
    }

    @Test
    void testGenerateAndParseAccessToken() {
        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        user.setUsername("testuser");

        String token = jwtService.generateAccessToken(user);
        assertNotNull(token);

        Jws<Claims> claimsJws = jwtService.parse(token);
        assertEquals(userId.toString(), claimsJws.getPayload().getSubject());
        assertEquals("testuser", claimsJws.getPayload().get("username"));
        assertEquals("access", claimsJws.getPayload().get("typ"));
    }

    @Test
    void testGenerateAndParseRefreshToken() {
        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        user.setUsername("testuser");

        String token = jwtService.generateRefreshToken(user);
        assertNotNull(token);

        Jws<Claims> claimsJws = jwtService.parse(token);
        assertEquals(userId.toString(), claimsJws.getPayload().getSubject());
        assertEquals("refresh", claimsJws.getPayload().get("typ"));
    }

    @Test
    void testInvalidSignatureThrowsException() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");

        String token = jwtService.generateAccessToken(user);
        String tamperedToken = token + "tampered";

        assertThrows(Exception.class, () -> jwtService.parse(tamperedToken));
    }
}
