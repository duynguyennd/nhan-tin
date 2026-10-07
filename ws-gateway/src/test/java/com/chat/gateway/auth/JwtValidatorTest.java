package com.chat.gateway.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtValidatorTest {

    private JwtValidator jwtValidator;
    private final String secret = "dev-only-secret-key-change-me-0123456789abcdef0123456789abcdef";
    private SecretKey key;

    @BeforeEach
    void setUp() {
        jwtValidator = new JwtValidator(secret);
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void testValidateValidToken() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("username", "testuser")
                .claim("role", "USER")
                .claim("banned", false)
                .claim("typ", "access")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(60, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        UUID result = jwtValidator.validate(token);

        assertEquals(userId, result);
    }

    @Test
    void testValidateBannedUserThrowsException() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("username", "banned")
                .claim("role", "USER")
                .claim("banned", true)
                .claim("typ", "access")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(60, ChronoUnit.MINUTES)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThrows(RuntimeException.class, () -> jwtValidator.validate(token));
    }

    @Test
    void testValidateExpiredTokenThrowsException() {
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("typ", "access")
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThrows(Exception.class, () -> jwtValidator.validate(token));
    }

    @Test
    void testValidateInvalidSignatureThrowsException() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-secret-key-0123456789abcdef0123456789abcdef01234567".getBytes(StandardCharsets.UTF_8));
        UUID userId = UUID.randomUUID();
        String token = Jwts.builder()
                .subject(userId.toString())
                .claim("typ", "access")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(60, ChronoUnit.MINUTES)))
                .signWith(wrongKey, Jwts.SIG.HS256)
                .compact();

        assertThrows(Exception.class, () -> jwtValidator.validate(token));
    }

    @Test
    void testValidateGarbageTokenThrowsException() {
        assertThrows(Exception.class, () -> jwtValidator.validate("not.a.valid.jwt"));
    }
}
