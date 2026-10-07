package com.chat.messaging.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AiDtos {

    private AiDtos() {}

    public record AiChatRequest(
            @NotNull UUID conversationId,
            @NotBlank String prompt
    ) {}

    public record AiChatResponse(
            UUID conversationId,
            String prompt,
            String reply,
            String model,
            Instant timestamp
    ) {}

    public record AiSummarizeRequest(
            @NotNull UUID conversationId
    ) {}

    public record AiSummaryResponse(
            UUID conversationId,
            String topic,
            String summary,
            List<String> keyPoints,
            List<String> actionItems,
            String sentiment,
            int messageCount,
            double confidence
    ) {}

    public record AiSmartRepliesRequest(
            UUID conversationId,
            String lastMessage
    ) {}

    public record AiSmartRepliesResponse(
            List<String> replies
    ) {}

    public record AiTranslateRequest(
            @NotBlank String text,
            String targetLang
    ) {}

    public record AiTranslateResponse(
            String originalText,
            String sourceLang,
            String targetLang,
            String translatedText
    ) {}

    public record AiToneRequest(
            @NotBlank String text,
            @NotBlank String tone
    ) {}

    public record AiToneResponse(
            String originalText,
            String tone,
            String rewrittenText
    ) {}

    public record AiImagineRequest(
            @NotBlank String prompt,
            String style
    ) {}

    public record AiImagineResponse(
            String prompt,
            String style,
            String imageUrl,
            String altText
    ) {}
}
