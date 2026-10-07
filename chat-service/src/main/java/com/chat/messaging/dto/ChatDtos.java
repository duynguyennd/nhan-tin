package com.chat.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ChatDtos {

    private ChatDtos() {
    }

    public record CreateDirectConversationRequest(
            @NotNull UUID targetUserId) {
    }

    public record CreateGroupConversationRequest(
            @NotBlank String name,
            String avatarUrl,
            @NotNull @Size(min = 1) List<UUID> memberUserIds) {
    }

    public record ConversationResponse(
            UUID id,
            String type,
            String name,
            String avatarUrl,
            List<UUID> memberIds,
            Instant createdAt) {
    }

    public record SendMessageRequest(
            @NotNull UUID conversationId,
            @NotBlank @Size(max = 4000) String content,
            List<String> mediaUrls,
            UUID replyToId) {
    }

    public record MessageResponse(
            UUID conversationId,
            UUID messageId,
            UUID senderId,
            String content,
            List<String> mediaUrls,
            String status,
            Instant createdAt,
            UUID replyToId,
            String replyToContent,
            UUID replyToSenderId) {
    }

    /** Sự kiện phát qua Redis Pub/Sub tới các WS Gateway instance. */
    public record ChatMessageEvent(
            UUID conversationId,
            UUID messageId,
            UUID senderId,
            String content,
            List<String> mediaUrls,
            String status,
            Instant createdAt,
            List<UUID> memberIds,
            UUID replyToId,
            String replyToContent,
            UUID replyToSenderId) {
    }

    public record AddReactionRequest(
            @NotNull UUID conversationId,
            String bucketId,
            @NotBlank String emoji) {
    }

    public record ReactionResponse(
            UUID conversationId,
            String bucketId,
            UUID messageId,
            UUID userId,
            String emoji,
            List<UUID> memberIds) {
    }

    public record MessageRecalledEvent(
            UUID conversationId,
            String bucketId,
            UUID messageId,
            List<UUID> memberIds) {
    }
}
