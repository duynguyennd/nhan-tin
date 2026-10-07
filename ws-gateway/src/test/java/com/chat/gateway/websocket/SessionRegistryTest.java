package com.chat.gateway.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionRegistryTest {

    private SessionRegistry sessionRegistry;

    @BeforeEach
    void setUp() {
        sessionRegistry = new SessionRegistry();
    }

    @Test
    void testRegisterAndIsConnectedLocally() {
        UUID userId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("sess-1");

        assertFalse(sessionRegistry.isConnectedLocally(userId));

        sessionRegistry.register(userId, session);

        assertTrue(sessionRegistry.isConnectedLocally(userId));
        Set<UUID> onlineUsers = sessionRegistry.getOnlineUserIds();
        assertEquals(1, onlineUsers.size());
        assertTrue(onlineUsers.contains(userId));
    }

    @Test
    void testUnregisterRemovesUserWhenNoSessionsLeft() {
        UUID userId = UUID.randomUUID();
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        when(session1.getId()).thenReturn("sess-1");
        when(session2.getId()).thenReturn("sess-2");

        sessionRegistry.register(userId, session1);
        sessionRegistry.register(userId, session2);

        assertTrue(sessionRegistry.isConnectedLocally(userId));

        sessionRegistry.unregister(userId, session1);
        assertTrue(sessionRegistry.isConnectedLocally(userId)); // Still connected via session2

        sessionRegistry.unregister(userId, session2);
        assertFalse(sessionRegistry.isConnectedLocally(userId)); // No sessions left
    }

    @Test
    void testSendToActiveSessionSuccess() throws IOException {
        UUID userId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("sess-1");
        when(session.isOpen()).thenReturn(true);

        sessionRegistry.register(userId, session);

        int sent = sessionRegistry.sendTo(userId, "Hello World");

        assertEquals(1, sent);
        verify(session, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testSendToClosedSessionRemovesIt() throws IOException {
        UUID userId = UUID.randomUUID();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("sess-1");
        when(session.isOpen()).thenReturn(false); // Session is closed

        sessionRegistry.register(userId, session);
        assertTrue(sessionRegistry.isConnectedLocally(userId));

        int sent = sessionRegistry.sendTo(userId, "Hello World");

        assertEquals(0, sent);
        verify(session, never()).sendMessage(any(TextMessage.class));
        assertFalse(sessionRegistry.isConnectedLocally(userId)); // It should be removed dynamically
    }

    @Test
    void testBroadcastAll() throws IOException {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);

        when(session1.getId()).thenReturn("sess-1");
        when(session1.isOpen()).thenReturn(true);
        when(session2.getId()).thenReturn("sess-2");
        when(session2.isOpen()).thenReturn(true);

        sessionRegistry.register(user1, session1);
        sessionRegistry.register(user2, session2);

        sessionRegistry.broadcastAll("Broadcast Msg");

        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
    }
}
