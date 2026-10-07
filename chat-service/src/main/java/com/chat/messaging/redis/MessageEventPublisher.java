package com.chat.messaging.redis;

import com.chat.messaging.dto.ChatDtos.ChatMessageEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Phát sự kiện tin nhắn mới vào Redis Pub/Sub channel "chat.messages".
 * Mọi WS Gateway instance đều subscribe channel này và tự lọc theo session local
 * (MVP). Khi scale nhiều gateway, có thể đổi sang routing theo channel
 * gateway-{id} như mục 7.1 của tài liệu thiết kế.
 */
@Component
public class MessageEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MessageEventPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String channel;

    public MessageEventPublisher(StringRedisTemplate redisTemplate,
                                 ObjectMapper objectMapper,
                                 @Value("${app.redis.message-channel}") String channel) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.channel = channel;
    }

    public void publish(ChatMessageEvent event) {
        publishObject(event);
    }

    public void publishObject(Object event) {
        if (redisTemplate == null) return;
        try {
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(channel, payload);
        } catch (JsonProcessingException e) {
            log.error("Không serialize được event {}", event, e);
        }
    }
}
