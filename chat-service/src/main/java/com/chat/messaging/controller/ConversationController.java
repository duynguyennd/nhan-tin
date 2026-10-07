package com.chat.messaging.controller;

import com.chat.messaging.dto.ChatDtos.ConversationResponse;
import com.chat.messaging.dto.ChatDtos.CreateDirectConversationRequest;
import com.chat.messaging.dto.ChatDtos.CreateGroupConversationRequest;
import com.chat.messaging.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ChatService chatService;

    public ConversationController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/direct")
    public ResponseEntity<ConversationResponse> createDirect(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateDirectConversationRequest request) {
        ConversationResponse response = chatService.createDirectConversation(currentUserId(jwt), request.targetUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/group")
    public ResponseEntity<ConversationResponse> createGroup(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateGroupConversationRequest request) {
        ConversationResponse response = chatService.createGroupConversation(
                currentUserId(jwt), request.name(), request.avatarUrl(), request.memberUserIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<ConversationResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return chatService.listMyConversations(currentUserId(jwt));
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
