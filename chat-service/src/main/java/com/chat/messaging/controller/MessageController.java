package com.chat.messaging.controller;

import com.chat.messaging.dto.ChatDtos.MessageResponse;
import com.chat.messaging.dto.ChatDtos.SendMessageRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import com.chat.messaging.dto.ChatDtos.AddReactionRequest;
import com.chat.messaging.dto.ChatDtos.ReactionResponse;
import com.chat.messaging.reaction.Reaction;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ChatService chatService;

    public MessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> send(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = chatService.postMessage(currentUserId(jwt), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<MessageResponse> history(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID conversationId,
            @RequestParam(required = false) String bucket,
            @RequestParam(required = false) UUID before,
            @RequestParam(defaultValue = "30") int size) {
        return chatService.getHistory(currentUserId(jwt), conversationId, bucket, before, size);
    }

    /** Thả emoji Reaction vào tin nhắn */
    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<ReactionResponse> addReaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID messageId,
            @Valid @RequestBody AddReactionRequest request) {
        ReactionResponse response = chatService.addReaction(currentUserId(jwt), messageId, request);
        return ResponseEntity.ok(response);
    }

    /** Xóa emoji Reaction */
    @DeleteMapping("/{messageId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID messageId,
            @RequestParam UUID conversationId,
            @RequestParam(required = false) String bucket) {
        chatService.removeReaction(currentUserId(jwt), messageId, conversationId, bucket);
        return ResponseEntity.ok().build();
    }

    /** Lấy danh sách Reactions của tin nhắn */
    @GetMapping("/{messageId}/reactions")
    public List<Reaction> getReactions(
            @PathVariable UUID messageId,
            @RequestParam UUID conversationId,
            @RequestParam(required = false) String bucket) {
        return chatService.getReactions(conversationId, bucket, messageId);
    }

    /** Thu hồi tin nhắn (trong 5 phút) */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> recallMessage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID messageId,
            @RequestParam UUID conversationId,
            @RequestParam(required = false) String bucket) {
        chatService.recallMessage(currentUserId(jwt), conversationId, bucket, messageId);
        return ResponseEntity.ok().build();
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
