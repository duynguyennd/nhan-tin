package com.chat.gateway.redis;

import com.chat.gateway.websocket.SessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MessageEventSubscriberTest {

    private SessionRegistry sessionRegistry;
    private ObjectMapper objectMapper;
    private MessageEventSubscriber subscriber;

    @BeforeEach
    void setUp() {
        sessionRegistry = mock(SessionRegistry.class);
        objectMapper = new ObjectMapper();
        subscriber = new MessageEventSubscriber(sessionRegistry, objectMapper);
    }

    @Test
    void testOnMessage_ChatMessage_DeliveredToMembers() {
        UUID senderId = UUID.randomUUID();
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();
        UUID convId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();

        String eventJson = String.format(
                "{\"conversationId\":\"%s\",\"messageId\":\"%s\",\"senderId\":\"%s\"," +
                "\"content\":\"Hello\",\"mediaUrls\":[],\"status\":\"SENT\"," +
                "\"createdAt\":\"2026-09-15T10:00:00Z\",\"memberIds\":[\"%s\",\"%s\"]}",
                convId, msgId, senderId, member1, member2);

        org.springframework.data.redis.connection.Message redisMsg = new DefaultMessage(
                "chat.messages".getBytes(StandardCharsets.UTF_8),
                eventJson.getBytes(StandardCharsets.UTF_8));

        when(sessionRegistry.sendTo(eq(member1), anyString())).thenReturn(1);
        when(sessionRegistry.sendTo(eq(member2), anyString())).thenReturn(1);

        subscriber.onMessage(redisMsg, null);

        verify(sessionRegistry, times(1)).sendTo(eq(member1), contains("MESSAGE"));
        verify(sessionRegistry, times(1)).sendTo(eq(member2), contains("MESSAGE"));
    }

    @Test
    void testOnMessage_ReactionEvent_DeliveredToMembers() {
        UUID userId = UUID.randomUUID();
        UUID member1 = UUID.randomUUID();
        UUID convId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();

        String eventJson = String.format(
                "{\"conversationId\":\"%s\",\"bucketId\":\"2026-09\",\"messageId\":\"%s\"," +
                "\"userId\":\"%s\",\"emoji\":\"👍\",\"memberIds\":[\"%s\"]}",
                convId, msgId, userId, member1);

        org.springframework.data.redis.connection.Message redisMsg = new DefaultMessage(
                "chat.messages".getBytes(StandardCharsets.UTF_8),
                eventJson.getBytes(StandardCharsets.UTF_8));

        when(sessionRegistry.sendTo(eq(member1), anyString())).thenReturn(1);

        subscriber.onMessage(redisMsg, null);

        verify(sessionRegistry, times(1)).sendTo(eq(member1), contains("REACTION"));
    }

    @Test
    void testOnMessage_BanAction_ClosesSession() {
        UUID userId = UUID.randomUUID();

        String body = "BAN:" + userId;
        org.springframework.data.redis.connection.Message redisMsg = new DefaultMessage(
                "admin.actions".getBytes(StandardCharsets.UTF_8),
                body.getBytes(StandardCharsets.UTF_8));

        subscriber.onMessage(redisMsg, null);

        verify(sessionRegistry, times(1)).closeSession(userId);
    }

    @Test
    void testOnMessage_BroadcastAction_BroadcastsToAll() {
        String body = "BROADCAST:WARNING|Hệ thống sẽ bảo trì lúc 23:00";
        org.springframework.data.redis.connection.Message redisMsg = new DefaultMessage(
                "admin.actions".getBytes(StandardCharsets.UTF_8),
                body.getBytes(StandardCharsets.UTF_8));

        subscriber.onMessage(redisMsg, null);

        verify(sessionRegistry, times(1)).broadcastAll(contains("SYSTEM_BROADCAST"));
        verify(sessionRegistry, times(1)).broadcastAll(contains("WARNING"));
    }

    @Test
    void testOnMessage_InvalidJson_DoesNotThrow() {
        org.springframework.data.redis.connection.Message redisMsg = new DefaultMessage(
                "chat.messages".getBytes(StandardCharsets.UTF_8),
                "not-valid-json{{{".getBytes(StandardCharsets.UTF_8));

        // Should not throw, just log the error
        subscriber.onMessage(redisMsg, null);

        verify(sessionRegistry, never()).sendTo(any(), anyString());
    }
}
