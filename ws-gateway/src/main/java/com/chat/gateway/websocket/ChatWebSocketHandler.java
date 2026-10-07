package com.chat.gateway.websocket;

import com.chat.gateway.service.ChatServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handler chính của WS Gateway:
 *  - Kết nối mới -> đăng ký session vào SessionRegistry
 *  - Frame {"type":"CHAT", ...} -> forward tới chat-service -> trả ACK
 *  - Frame {"type":"PING"}    -> trả {"type":"PONG"} (heartbeat)
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final SessionRegistry sessionRegistry;
    private final ChatServiceClient chatServiceClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public ChatWebSocketHandler(SessionRegistry sessionRegistry,
                                ChatServiceClient chatServiceClient,
                                ObjectMapper objectMapper,
                                StringRedisTemplate redisTemplate) {
        this.sessionRegistry = sessionRegistry;
        this.chatServiceClient = chatServiceClient;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID userId = userId(session);
        boolean wasOnline = sessionRegistry.isConnectedLocally(userId);
        sessionRegistry.register(userId, session);

        // Lưu trạng thái online vào Redis
        redisTemplate.opsForSet().add("online:users", userId.toString());

        sendJson(session, objectMapper.createObjectNode()
                .put("type", "CONNECTED")
                .put("userId", userId.toString()));

        ArrayNode userIdsNode = objectMapper.createArrayNode();
        for (UUID onlineId : sessionRegistry.getOnlineUserIds()) {
            userIdsNode.add(onlineId.toString());
        }
        sendJson(session, objectMapper.createObjectNode()
                .put("type", "ONLINE_USERS")
                .set("userIds", userIdsNode));

        if (!wasOnline) {
            String presenceMsg = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("type", "PRESENCE")
                    .put("userId", userId.toString())
                    .put("status", "ONLINE"));
            sessionRegistry.broadcastAll(presenceMsg);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        UUID userId = userId(session);
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = payload.path("type").asText("");
            switch (type) {
                case "PING" -> sendJson(session, objectMapper.createObjectNode().put("type", "PONG"));
                case "CHAT" -> handleChatFrame(session, userId, payload);
                case "TYPING" -> handleTypingFrame(session, userId, payload);
                default -> sendError(session, "UNKNOWN_TYPE", "Loại frame không hỗ trợ: " + type);
            }
        } catch (Exception e) {
            log.warn("Frame không hợp lệ từ user {}: {}", userId, e.getMessage());
            sendError(session, "BAD_FRAME", "Frame JSON không hợp lệ");
        }
    }

    private void handleTypingFrame(WebSocketSession session, UUID userId, JsonNode payload) {
        String conversationId = payload.path("conversationId").asText(null);
        boolean isTyping = payload.path("isTyping").asBoolean(true);
        if (conversationId == null) return;

        try {
            String typingMsg = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("type", "TYPING")
                    .put("conversationId", conversationId)
                    .put("userId", userId.toString())
                    .put("isTyping", isTyping));
            // TODO: Tối ưu - chỉ gửi typing indicator tới members của conversation
            // Hiện tại broadcastAll vì ws-gateway không lưu member list;
            // client sẽ filter theo conversationId ở phía frontend.
            sessionRegistry.broadcastAll(typingMsg);
        } catch (Exception e) {
            log.warn("Lỗi xử lý TYPING frame cho user {}: {}", userId, e.getMessage());
        }
    }

    private void handleChatFrame(WebSocketSession session, UUID userId, JsonNode payload) {
        // Kiểm tra xem user có đang bị MUTE (cấm chat tạm thời) không
        if (Boolean.TRUE.equals(redisTemplate.hasKey("mute:user:" + userId))) {
            Long ttl = redisTemplate.getExpire("mute:user:" + userId);
            long mins = ttl != null && ttl > 0 ? (ttl / 60) + 1 : 1;
            sendError(session, "MUTED", "Tài khoản của bạn tạm thời bị cấm chat. Vui lòng thử lại sau " + mins + " phút.");
            return;
        }

        long minute = System.currentTimeMillis() / 60000;
        String limitKey = "ratelimit:ws:msg:" + userId + ":" + minute;
        Long count = redisTemplate.opsForValue().increment(limitKey);
        if (count != null && count == 1) {
            redisTemplate.expire(limitKey, Duration.ofMinutes(2));
        }
        if (count != null && count > 30) {
            log.warn("User {} bị giới hạn gửi tin nhắn (Rate limit exceeded)", userId);
            sendError(session, "RATE_LIMIT_EXCEEDED", "Gửi tin nhắn quá nhanh. Giới hạn là 30 tin nhắn/phút.");
            return;
        }

        String conversationId = payload.path("conversationId").asText(null);
        String content = payload.path("content").asText(null);
        if (conversationId == null || content == null || content.isBlank()) {
            sendError(session, "BAD_FRAME", "CHAT frame cần conversationId và content");
            return;
        }
        List<String> mediaUrls = new ArrayList<>();
        payload.path("mediaUrls").forEach(node -> mediaUrls.add(node.asText()));

        String token = (String) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_TOKEN);
        try {
            Map<String, Object> response = chatServiceClient.sendMessage(
                    token, UUID.fromString(conversationId), content, mediaUrls);
            sendJson(session, objectMapper.createObjectNode()
                    .put("type", "ACK")
                    .put("conversationId", conversationId)
                    .put("messageId", String.valueOf(response.get("messageId")))
                    .put("status", "SENT"));
        } catch (Exception e) {
            log.warn("Forward message tới chat-service thất bại (user {}): {}", userId, e.getMessage());
            sendError(session, "SEND_FAILED", "Không gửi được tin nhắn: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = userId(session);
        sessionRegistry.unregister(userId, session);
        if (!sessionRegistry.isConnectedLocally(userId)) {
            // Xóa trạng thái online khỏi Redis
            redisTemplate.opsForSet().remove("online:users", userId.toString());

            try {
                String presenceMsg = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                        .put("type", "PRESENCE")
                        .put("userId", userId.toString())
                        .put("status", "OFFLINE"));
                sessionRegistry.broadcastAll(presenceMsg);
            } catch (Exception e) {
                log.warn("Error broadcasting offline status for user {}: {}", userId, e.getMessage());
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("Transport error session {}: {}", session.getId(), exception.getMessage());
    }

    private UUID userId(WebSocketSession session) {
        return (UUID) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_USER_ID);
    }

    private void sendError(WebSocketSession session, String code, String message) {
        sendJson(session, objectMapper.createObjectNode()
                .put("type", "ERROR")
                .put("code", code)
                .put("message", message));
    }

    private void sendJson(WebSocketSession session, ObjectNode node) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(node)));
                }
            }
        } catch (IOException e) {
            log.warn("Không gửi được frame tới session {}: {}", session.getId(), e.getMessage());
        }
    }
}
