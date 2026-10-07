package com.chat.gateway.websocket;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/gateway")
public class GatewayAdminController {

    private final SessionRegistry sessionRegistry;

    public GatewayAdminController(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /** Trả về danh sách online user kèm số lượng WebSocket session của từng user */
    @GetMapping("/sessions")
    public Map<UUID, Integer> getActiveSessions() {
        return sessionRegistry.getSessionCounts();
    }

    /** Ngắt kết nối toàn bộ WebSocket sessions của 1 user */
    @PostMapping("/sessions/{userId}/disconnect")
    public ResponseEntity<Void> disconnectUserSessions(@PathVariable UUID userId) {
        sessionRegistry.closeSession(userId);
        return ResponseEntity.ok().build();
    }
}
