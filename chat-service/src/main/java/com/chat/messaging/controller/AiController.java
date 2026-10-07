package com.chat.messaging.controller;

import com.chat.messaging.ai.AiDtos.*;
import com.chat.messaging.ai.AiService;
import com.chat.messaging.dto.ChatDtos.MessageResponse;
import com.chat.messaging.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final ChatService chatService;

    public AiController(AiService aiService, ChatService chatService) {
        this.aiService = aiService;
        this.chatService = chatService;
    }

    /** Trò chuyện trực tiếp với AI Copilot */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AiChatRequest request) {
        AiChatResponse response = aiService.askAi(request.conversationId(), request.prompt());
        return ResponseEntity.ok(response);
    }

    /** Tóm tắt cuộc hội thoại bằng AI */
    @PostMapping("/summarize")
    public ResponseEntity<AiSummaryResponse> summarize(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AiSummarizeRequest request) {
        UUID userId = currentUserId(jwt);
        List<MessageResponse> history = chatService.getHistory(userId, request.conversationId(), null, null, 30);
        List<String> contents = history.stream().map(MessageResponse::content).toList();
        AiSummaryResponse response = aiService.summarize(request.conversationId(), contents);
        return ResponseEntity.ok(response);
    }

    /** Gợi ý phản hồi thông minh (Smart Replies) */
    @PostMapping("/smart-replies")
    public ResponseEntity<AiSmartRepliesResponse> smartReplies(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody AiSmartRepliesRequest request) {
        List<String> replies = aiService.generateSmartReplies(request.lastMessage());
        return ResponseEntity.ok(new AiSmartRepliesResponse(replies));
    }

    /** Dịch tin nhắn đa ngôn ngữ */
    @PostMapping("/translate")
    public ResponseEntity<AiTranslateResponse> translate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AiTranslateRequest request) {
        AiTranslateResponse response = aiService.translate(request.text(), request.targetLang());
        return ResponseEntity.ok(response);
    }

    /** Chỉnh sửa ngữ điệu tin nhắn (AI Tone Polisher) */
    @PostMapping("/rewrite")
    public ResponseEntity<AiToneResponse> rewrite(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AiToneRequest request) {
        AiToneResponse response = aiService.rewriteTone(request.text(), request.tone());
        return ResponseEntity.ok(response);
    }

    /** Tạo hình ảnh AI theo phong cách (/imagine) */
    @PostMapping("/imagine")
    public ResponseEntity<AiImagineResponse> imagine(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AiImagineRequest request) {
        AiImagineResponse response = aiService.generateImage(request.prompt(), request.style());
        return ResponseEntity.ok(response);
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
