package com.chat.gateway.websocket;

import com.chat.gateway.service.ChatServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatWebSocketHandlerTest {

    private SessionRegistry sessionRegistry;
    private ChatServiceClient chatServiceClient;
    private ObjectMapper objectMapper;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SetOperations<String, String> setOperations;
    private ChatWebSocketHandler handler;

    private UUID userId;
    private String token;
    private Map<String, Object> sessionAttributes;
    private WebSocketSession session;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sessionRegistry = mock(SessionRegistry.class);
        chatServiceClient = mock(ChatServiceClient.class);
        objectMapper = new ObjectMapper();
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        setOperations = mock(SetOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        handler = new ChatWebSocketHandler(sessionRegistry, chatServiceClient, objectMapper, redisTemplate);

        userId = UUID.randomUUID();
        token = "test_jwt_token";

        sessionAttributes = new HashMap<>();
        sessionAttributes.put(JwtHandshakeInterceptor.ATTR_USER_ID, userId);
        sessionAttributes.put(JwtHandshakeInterceptor.ATTR_TOKEN, token);

        session = mock(WebSocketSession.class);
        when(session.getAttributes()).thenReturn(sessionAttributes);
        when(session.isOpen()).thenReturn(true);
    }

    @Test
    void testAfterConnectionEstablished() throws Exception {
        when(sessionRegistry.isConnectedLocally(userId)).thenReturn(false);
        Set<UUID> onlineIds = new HashSet<>(Arrays.asList(userId, UUID.randomUUID()));
        when(sessionRegistry.getOnlineUserIds()).thenReturn(onlineIds);

        handler.afterConnectionEstablished(session);

        verify(sessionRegistry, times(1)).register(userId, session);
        verify(sessionRegistry, times(1)).broadcastAll(contains("PRESENCE"));
        verify(sessionRegistry, times(1)).broadcastAll(contains("ONLINE"));

        ArgumentCaptor<TextMessage> textMessageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(textMessageCaptor.capture());

        List<TextMessage> sentMessages = textMessageCaptor.getAllValues();
        assertTrue(sentMessages.stream().anyMatch(m -> m.getPayload().contains("CONNECTED")));
        assertTrue(sentMessages.stream().anyMatch(m -> m.getPayload().contains("ONLINE_USERS")));
    }

    @Test
    void testHandleTextMessage_Ping() throws Exception {
        TextMessage pingMessage = new TextMessage("{\"type\":\"PING\"}");
        handler.handleTextMessage(session, pingMessage);

        ArgumentCaptor<TextMessage> textMessageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(1)).sendMessage(textMessageCaptor.capture());
        assertTrue(textMessageCaptor.getValue().getPayload().contains("PONG"));
    }

    @Test
    void testHandleTextMessage_Chat_Success() throws Exception {
        UUID conversationId = UUID.randomUUID();
        String content = "Hello Alice";
        TextMessage chatMessage = new TextMessage(String.format(
                "{\"type\":\"CHAT\",\"conversationId\":\"%s\",\"content\":\"%s\",\"mediaUrls\":[]}",
                conversationId, content));

        when(valueOperations.increment(anyString())).thenReturn(1L);

        UUID responseMessageId = UUID.randomUUID();
        Map<String, Object> serviceResponse = Map.of("messageId", responseMessageId.toString());
        when(chatServiceClient.sendMessage(eq(token), eq(conversationId), eq(content), anyList()))
                .thenReturn(serviceResponse);

        handler.handleTextMessage(session, chatMessage);

        verify(chatServiceClient, times(1)).sendMessage(eq(token), eq(conversationId), eq(content), anyList());

        ArgumentCaptor<TextMessage> textMessageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(1)).sendMessage(textMessageCaptor.capture());
        String responsePayload = textMessageCaptor.getValue().getPayload();
        assertTrue(responsePayload.contains("ACK"));
        assertTrue(responsePayload.contains("SENT"));
        assertTrue(responsePayload.contains(responseMessageId.toString()));
    }

    @Test
    void testHandleTextMessage_Chat_RateLimitExceeded() throws Exception {
        UUID conversationId = UUID.randomUUID();
        String content = "Spamming message";
        TextMessage chatMessage = new TextMessage(String.format(
                "{\"type\":\"CHAT\",\"conversationId\":\"%s\",\"content\":\"%s\",\"mediaUrls\":[]}",
                conversationId, content));

        when(valueOperations.increment(anyString())).thenReturn(31L);

        handler.handleTextMessage(session, chatMessage);

        verify(chatServiceClient, never()).sendMessage(any(), any(), any(), any());

        ArgumentCaptor<TextMessage> textMessageCaptor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(1)).sendMessage(textMessageCaptor.capture());
        String responsePayload = textMessageCaptor.getValue().getPayload();
        assertTrue(responsePayload.contains("ERROR"));
        assertTrue(responsePayload.contains("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void testHandleTextMessage_Typing() throws Exception {
        UUID conversationId = UUID.randomUUID();
        TextMessage typingMessage = new TextMessage(String.format(
                "{\"type\":\"TYPING\",\"conversationId\":\"%s\",\"isTyping\":true}", conversationId));

        handler.handleTextMessage(session, typingMessage);

        verify(sessionRegistry, times(1)).broadcastAll(contains("TYPING"));
        verify(sessionRegistry, times(1)).broadcastAll(contains(conversationId.toString()));
        verify(sessionRegistry, times(1)).broadcastAll(contains(userId.toString()));
    }

    @Test
    void testAfterConnectionClosed() throws Exception {
        when(sessionRegistry.isConnectedLocally(userId)).thenReturn(false);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(sessionRegistry, times(1)).unregister(userId, session);
        verify(sessionRegistry, times(1)).broadcastAll(contains("PRESENCE"));
        verify(sessionRegistry, times(1)).broadcastAll(contains("OFFLINE"));
    }
}
