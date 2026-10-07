package com.chat.gateway.redis;

import com.chat.gateway.websocket.SessionRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lắng nghe channel "chat.messages" + "admin.actions":
 * - chat.messages: forward tin nhắn xuống clients
 * - admin.actions: xử lý BAN + BROADCAST
 */
@Component
public class MessageEventSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(MessageEventSubscriber.class);

    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public MessageEventSubscriber(SessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel(), java.nio.charset.StandardCharsets.UTF_8);
            if ("admin.actions".equals(channel)) {
                String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
                // Loại bỏ dấu nháy kép nếu có do serialize Redis
                body = body.replaceAll("^\"|\"$", "");

                // Handle BROADCAST: Admin phát thông báo tới tất cả user online
                if (body.startsWith("BROADCAST:")) {
                    String broadcastData = body.substring(10);
                    String broadcastType = "INFO";
                    String broadcastMsg = broadcastData;
                    int pipeIdx = broadcastData.indexOf('|');
                    if (pipeIdx > 0) {
                        broadcastType = broadcastData.substring(0, pipeIdx);
                        broadcastMsg = broadcastData.substring(pipeIdx + 1);
                    }
                    ObjectNode frame = objectMapper.createObjectNode();
                    frame.put("type", "SYSTEM_BROADCAST");
                    frame.put("message", broadcastMsg);
                    frame.put("broadcastType", broadcastType);
                    frame.put("timestamp", System.currentTimeMillis());
                    String payload = objectMapper.writeValueAsString(frame);
                    sessionRegistry.broadcastAll(payload);
                    log.info("Đã phát broadcast tới tất cả clients");
                    return;
                }

                // Handle BAN: đóng kết nối WebSocket của user bị ban
                if (body.startsWith("BAN:")) {
                    String userIdStr = body.substring(4);
                    UUID userId = UUID.fromString(userIdStr);
                    sessionRegistry.closeSession(userId);
                }
                return;
            }

            JsonNode event = objectMapper.readTree(message.getBody());
            ObjectNode frame = objectMapper.createObjectNode();

            if (event.has("emoji")) {
                frame.put("type", "REACTION");
            } else if (event.has("bucketId") && !event.has("content")) {
                frame.put("type", "RECALL");
            } else {
                frame.put("type", "MESSAGE");
            }

            frame.setAll((ObjectNode) event);
            frame.remove("memberIds");
            String payload = objectMapper.writeValueAsString(frame);

            int delivered = 0;
            for (JsonNode memberNode : event.path("memberIds")) {
                UUID memberId = UUID.fromString(memberNode.asText());
                delivered += sessionRegistry.sendTo(memberId, payload);
            }
            if (delivered > 0) {
                log.debug("Đã push message {} xuống {} session local",
                        event.path("messageId").asText(), delivered);
            }
        } catch (Exception e) {
            log.error("Không xử lý được sự kiện từ Redis: {}", e.getMessage(), e);
        }
    }
}
