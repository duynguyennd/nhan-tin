package com.chat.gateway.websocket;

import com.chat.gateway.auth.JwtValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Chặn WS handshake, xác minh JWT trong query param "token":
 *   ws://host:8083/ws?token=<JWT>
 * Token hợp lệ -> gắn userId + token gốc vào session attributes.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_TOKEN = "token";

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final JwtValidator jwtValidator;
    private final StringRedisTemplate redisTemplate;

    public JwtHandshakeInterceptor(JwtValidator jwtValidator, StringRedisTemplate redisTemplate) {
        this.jwtValidator = jwtValidator;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String ip = "unknown";
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        }

        long minute = System.currentTimeMillis() / 60000;
        String limitKey = "ratelimit:ws:connect:" + ip + ":" + minute;
        Long count = redisTemplate.opsForValue().increment(limitKey);
        if (count != null && count == 1) {
            redisTemplate.expire(limitKey, Duration.ofMinutes(2));
        }
        if (count != null && count > 5) {
            log.warn("WS handshake bị từ chối do vượt quá giới hạn kết nối (IP: {})", ip);
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            return false;
        }

        String token = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("token");
        }
        if (token == null || token.isBlank()) {
            log.warn("WS handshake bị từ chối: thiếu token");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            UUID userId = jwtValidator.validate(token);
            attributes.put(ATTR_USER_ID, userId);
            attributes.put(ATTR_TOKEN, token);
            return true;
        } catch (Exception e) {
            log.warn("WS handshake bị từ chối: token không hợp lệ ({})", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
