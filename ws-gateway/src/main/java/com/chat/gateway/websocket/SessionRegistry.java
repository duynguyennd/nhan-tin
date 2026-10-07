package com.chat.gateway.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Bản đồ ánh xạ User_ID -> các WebSocket session đang kết nối tới gateway instance này
 * (một user có thể mở nhiều thiết bị/tab).
 */
@Component
public class SessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SessionRegistry.class);

    private final Map<UUID, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public void register(UUID userId, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("Registered session {} for user {} (total users online: {})",
                session.getId(), userId, sessionsByUser.size());
    }

    public void unregister(UUID userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByUser.remove(userId, sessions);
            }
        }
        log.info("Unregistered session {} for user {}", session.getId(), userId);
    }

    public boolean isConnectedLocally(UUID userId) {
        return sessionsByUser.containsKey(userId);
    }

    public Set<UUID> getOnlineUserIds() {
        return sessionsByUser.keySet();
    }

    /** Trả về danh sách userId kèm số session kết nối (cho admin dashboard) */
    public Map<UUID, Integer> getSessionCounts() {
        Map<UUID, Integer> counts = new ConcurrentHashMap<>();
        sessionsByUser.forEach((userId, sessions) -> counts.put(userId, sessions.size()));
        return counts;
    }

    public void broadcastAll(String payload) {
        for (UUID userId : sessionsByUser.keySet()) {
            sendTo(userId, payload);
        }
    }

    /** Gửi payload tới mọi session local của user; dọn dẹp session đã đóng. */
    public int sendTo(UUID userId, String payload) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }
        int sent = 0;
        for (WebSocketSession session : List.copyOf(sessions)) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(payload));
                    }
                    sent++;
                } else {
                    sessions.remove(session);
                }
            } catch (IOException e) {
                log.warn("Gửi message tới session {} thất bại: {}", session.getId(), e.getMessage());
                sessions.remove(session);
            }
        }
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId, sessions);
        }
        return sent;
    }

    public void closeSession(UUID userId) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions != null && !sessions.isEmpty()) {
            for (WebSocketSession session : List.copyOf(sessions)) {
                try {
                    if (session.isOpen()) {
                        session.close(org.springframework.web.socket.CloseStatus.POLICY_VIOLATION.withReason("Tài khoản đã bị khóa bởi Admin."));
                    }
                } catch (IOException e) {
                    log.warn("Lỗi đóng session của user bị ban {}: {}", userId, e.getMessage());
                }
            }
            sessionsByUser.remove(userId);
            log.info("Đã ngắt toàn bộ kết nối WebSocket của user bị ban: {}", userId);
        }
    }
}
