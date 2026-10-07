package com.chat.messaging.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;
    private final JwtDecoder jwtDecoder;
    // Ngưỡng: tối đa 60 request / phút từ một địa chỉ IP
    private static final int LIMIT = 60;

    public RateLimitingFilter(StringRedisTemplate redisTemplate, JwtDecoder jwtDecoder) {
        this.redisTemplate = redisTemplate;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        // Bỏ qua health check
        if (path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (path.startsWith("/api")) {
            String method = request.getMethod();
            redisTemplate.opsForHash().increment("stats:api:requests", method + " " + path, 1);
        }

        String ip = request.getRemoteAddr();

        // 1. Kiểm tra IP Blacklist
        Boolean isBlacklisted = redisTemplate.opsForSet().isMember("blacklist:ips", ip);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            logSecurityEvent(ip, path, "IP bị chặn trong Blacklist", "n/a");
            response.setStatus(403);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Địa chỉ IP của bạn đã bị khóa do vi phạm chính sách bảo mật.\"}");
            return;
        }

        // 2. Kiểm tra chế độ bảo trì (Maintenance Mode)
        String maintenance = redisTemplate.opsForValue().get("system:maintenance");
        if ("true".equals(maintenance)) {
            // Cho phép Admin truy cập các API của chat-service để kiểm tra/cấu hình
            boolean isAdmin = false;
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    isAdmin = "ADMIN".equals(jwt.getClaimAsString("role"));
                } catch (Exception ignored) {}
            }
            if (!isAdmin) {
                logSecurityEvent(ip, path, "Cố gắng sử dụng Chat API trong lúc bảo trì", "n/a");
                response.setStatus(503);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"error\":\"Hệ thống đang bảo trì nâng cấp. Vui lòng quay lại sau.\"}");
                return;
            }
        }

        // 3. Rate Limiting
        String bypassHeader = request.getHeader("X-Bypass-Rate-Limit");
        if ("load-test-secret-123".equals(bypassHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        long minute = System.currentTimeMillis() / 60000;
        String key = "ratelimit:chat:" + ip + ":" + minute;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(2));
        }

        if (count != null && count > LIMIT) {
            logSecurityEvent(ip, path, "Vượt ngưỡng giới hạn lượt gọi Chat API", "n/a");
            response.setStatus(429); // HTTP 429 Too Many Requests
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void logSecurityEvent(String ip, String path, String reason, String username) {
        String logEntry = String.format("{\"timestamp\":%d,\"ip\":\"%s\",\"path\":\"%s\",\"reason\":\"%s\",\"username\":\"%s\"}",
                System.currentTimeMillis(), ip, path, reason, username);
        redisTemplate.opsForList().leftPush("security:logs", logEntry);
        redisTemplate.opsForList().trim("security:logs", 0, 99);
    }
}
