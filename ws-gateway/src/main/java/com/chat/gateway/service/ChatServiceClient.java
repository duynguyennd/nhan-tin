package com.chat.gateway.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gọi nội bộ tới chat-service (REST) để persist + phát tán tin nhắn.
 * Forward nguyên JWT của client qua header Authorization (chat-service là resource server).
 */
@Component
public class ChatServiceClient {

    private final RestTemplate restTemplate;
    private final String chatServiceUrl;

    public ChatServiceClient(RestTemplate restTemplate,
                             @Value("${app.chat-service.url}") String chatServiceUrl) {
        this.restTemplate = restTemplate;
        this.chatServiceUrl = chatServiceUrl;
    }

    /** @return MessageResponse dạng Map (messageId, createdAt, ...) */
    @SuppressWarnings("unchecked")
    public Map<String, Object> sendMessage(String jwtToken, UUID conversationId,
                                           String content, List<String> mediaUrls) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtToken);

        Map<String, Object> body = Map.of(
                "conversationId", conversationId.toString(),
                "content", content,
                "mediaUrls", mediaUrls == null ? List.of() : mediaUrls);

        return restTemplate.postForObject(
                chatServiceUrl + "/api/messages",
                new HttpEntity<>(body, headers),
                Map.class);
    }
}
