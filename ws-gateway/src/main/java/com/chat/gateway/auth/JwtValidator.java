package com.chat.gateway.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Xác minh JWT tại WS handshake (luồng 4.1 trong tài liệu thiết kế).
 * Validate local bằng shared secret thay vì gọi auth-service để giảm độ trễ.
 */
@Component
public class JwtValidator {

    private final SecretKey key;

    public JwtValidator(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** @return userId (claim "sub") nếu token hợp lệ */
    public UUID validate(String token) {
        Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        Boolean banned = jws.getPayload().get("banned", Boolean.class);
        if (Boolean.TRUE.equals(banned)) {
            throw new RuntimeException("Tài khoản đã bị khóa bởi Admin.");
        }
        return UUID.fromString(jws.getPayload().getSubject());
    }
}
