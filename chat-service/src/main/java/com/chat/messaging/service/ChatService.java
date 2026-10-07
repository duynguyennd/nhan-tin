package com.chat.messaging.service;

import com.chat.messaging.conversation.Conversation;
import com.chat.messaging.conversation.ConversationMember;
import com.chat.messaging.conversation.ConversationMemberId;
import com.chat.messaging.conversation.ConversationMemberRepository;
import com.chat.messaging.conversation.ConversationRepository;
import com.chat.messaging.dto.ChatDtos.ChatMessageEvent;
import com.chat.messaging.dto.ChatDtos.ConversationResponse;
import com.chat.messaging.dto.ChatDtos.MessageResponse;
import com.chat.messaging.dto.ChatDtos.SendMessageRequest;
import com.chat.messaging.exception.ChatExceptions.BadRequestException;
import com.chat.messaging.exception.ChatExceptions.ConversationNotFoundException;
import com.chat.messaging.exception.ChatExceptions.NotMemberException;
import com.chat.messaging.message.Message;
import com.chat.messaging.message.MessageKey;
import com.chat.messaging.message.MessageRepository;
import com.chat.messaging.redis.MessageEventPublisher;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.chat.messaging.reaction.Reaction;
import com.chat.messaging.reaction.ReactionKey;
import com.chat.messaging.reaction.ReactionRepository;
import com.chat.messaging.dto.ChatDtos.AddReactionRequest;
import com.chat.messaging.dto.ChatDtos.ReactionResponse;
import com.chat.messaging.ai.AiService;
import com.chat.messaging.ai.AiDtos.AiChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import java.util.concurrent.CompletableFuture;
import com.chat.messaging.dto.ChatDtos.MessageRecalledEvent;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final ReactionRepository reactionRepository;
    private final MessageEventPublisher eventPublisher;
    private final StringRedisTemplate redisTemplate;
    private final AiService aiService;

    public ChatService(ConversationRepository conversationRepository,
                       ConversationMemberRepository memberRepository,
                       MessageRepository messageRepository,
                       ReactionRepository reactionRepository,
                       MessageEventPublisher eventPublisher,
                       StringRedisTemplate redisTemplate,
                       @Lazy AiService aiService) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.eventPublisher = eventPublisher;
        this.redisTemplate = redisTemplate;
        this.aiService = aiService;
    }

    /** Tạo (hoặc trả về nếu đã có) cuộc trò chuyện DIRECT giữa userId và targetUserId. */
    @Transactional
    public ConversationResponse createDirectConversation(UUID userId, UUID targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BadRequestException("Không thể tạo cuộc trò chuyện với chính mình");
        }
        UUID existingId = conversationRepository.findDirectConversationBetween(userId, targetUserId).orElse(null);
        if (existingId != null) {
            return toResponse(conversationRepository.findById(existingId)
                    .orElseThrow(() -> new ConversationNotFoundException("Conversation không tồn tại: " + existingId)));
        }
        Conversation conversation = new Conversation();
        conversation.setType(Conversation.TYPE_DIRECT);
        conversationRepository.save(conversation);

        memberRepository.save(new ConversationMember(
                new ConversationMemberId(conversation.getId(), userId), ConversationMember.ROLE_MEMBER));
        memberRepository.save(new ConversationMember(
                new ConversationMemberId(conversation.getId(), targetUserId), ConversationMember.ROLE_MEMBER));

        return toResponse(conversation);
    }

    @Transactional
    public ConversationResponse createGroupConversation(UUID creatorId, String name, String avatarUrl, List<UUID> memberUserIds) {
        Conversation conversation = new Conversation();
        conversation.setType(Conversation.TYPE_GROUP);
        conversation.setName(name);
        conversation.setAvatarUrl(avatarUrl);
        conversationRepository.save(conversation);

        Set<UUID> allMembers = new HashSet<>(memberUserIds);
        allMembers.add(creatorId);

        for (UUID memberId : allMembers) {
            String role = memberId.equals(creatorId) ? ConversationMember.ROLE_ADMIN : ConversationMember.ROLE_MEMBER;
            memberRepository.save(new ConversationMember(
                    new ConversationMemberId(conversation.getId(), memberId), role));
        }

        return toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listMyConversations(UUID userId) {
        List<UUID> conversationIds = memberRepository.findByIdUserId(userId).stream()
                .map(m -> m.getId().getConversationId())
                .toList();
        return conversationRepository.findAllById(conversationIds).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Validate quyền gửi -> persist vào Cassandra -> publish sự kiện tới Redis Pub/Sub
     * để WS Gateway đẩy xuống client.
     */
    public MessageResponse postMessage(UUID senderId, SendMessageRequest request) {
        requireMembership(senderId, request.conversationId());

        Message message = new Message();
        message.setKey(new MessageKey(request.conversationId(), currentBucket(), Uuids.timeBased()));
        message.setSenderId(senderId);
        message.setContent(request.content());
        message.setMediaUrls(request.mediaUrls() == null ? List.of() : request.mediaUrls());
        message.setStatus(Message.STATUS_SENT);

        // Xử lý Reply nếu có replyToId
        if (request.replyToId() != null) {
            message.setReplyToId(request.replyToId());
            String replyBucket = extractBucket(null, request.replyToId());
            Message orig = messageRepository.findById(
                    new MessageKey(request.conversationId(), replyBucket, request.replyToId()))
                    .orElse(null);
            if (orig != null) {
                message.setReplyToContent(orig.getContent());
                message.setReplyToSenderId(orig.getSenderId());
            }
        }

        messageRepository.save(message);

        // ── Analytics counters cho Admin Dashboard ──
        if (redisTemplate != null) {
            try {
                String minuteKey = String.valueOf(System.currentTimeMillis() / 60000);
                redisTemplate.opsForHash().increment("stats:msg:throughput", minuteKey, 1);
                redisTemplate.opsForZSet().incrementScore("stats:user:msg_count", senderId.toString(), 1);
                redisTemplate.opsForHash().increment("stats:msg:hourly", String.valueOf(LocalTime.now().getHour()), 1);
            } catch (Exception ignored) {
            }
        }

        List<UUID> memberIds = memberRepository.findByIdConversationId(request.conversationId()).stream()
                .map(m -> m.getId().getUserId())
                .toList();

        MessageResponse response = toResponse(message);
        eventPublisher.publish(new ChatMessageEvent(
                response.conversationId(),
                response.messageId(),
                response.senderId(),
                response.content(),
                response.mediaUrls(),
                response.status(),
                response.createdAt(),
                memberIds,
                response.replyToId(),
                response.replyToContent(),
                response.replyToSenderId()));

        // Tự động kích hoạt phản hồi thông minh từ AI Copilot nếu tin nhắn tag @AI / @Copilot / @Gemini
        if (request.content() != null && request.content().matches("(?i).*@(AI|Copilot|Gemini).*") && !senderId.equals(AiService.AI_BOT_ID)) {
            triggerAiCopilotResponse(request.conversationId(), response, memberIds);
        }

        return response;
    }

    private void triggerAiCopilotResponse(UUID conversationId, MessageResponse userMsg, List<UUID> memberIds) {
        if (aiService == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(750); // Giả lập thời gian AI xử lý và phân tích ngữ cảnh
                AiChatResponse aiResp = aiService.askAi(conversationId, userMsg.content());

                Message botMsg = new Message();
                botMsg.setKey(new MessageKey(conversationId, currentBucket(), Uuids.timeBased()));
                botMsg.setSenderId(AiService.AI_BOT_ID);
                botMsg.setContent(aiResp.reply());
                botMsg.setMediaUrls(List.of());
                botMsg.setStatus(Message.STATUS_SENT);
                botMsg.setReplyToId(userMsg.messageId());
                botMsg.setReplyToContent(userMsg.content());
                botMsg.setReplyToSenderId(userMsg.senderId());
                messageRepository.save(botMsg);

                MessageResponse botResponse = toResponse(botMsg);
                eventPublisher.publish(new ChatMessageEvent(
                        botResponse.conversationId(),
                        botResponse.messageId(),
                        botResponse.senderId(),
                        botResponse.content(),
                        botResponse.mediaUrls(),
                        botResponse.status(),
                        botResponse.createdAt(),
                        memberIds,
                        botResponse.replyToId(),
                        botResponse.replyToContent(),
                        botResponse.replyToSenderId()));
            } catch (Exception e) {
                log.error("Lỗi khi tự động kích hoạt phản hồi từ AI Copilot: {}", e.getMessage());
            }
        });
    }

    /** Thêm / cập nhật Reaction cho tin nhắn */
    public ReactionResponse addReaction(UUID userId, UUID messageId, AddReactionRequest request) {
        requireMembership(userId, request.conversationId());
        String bucket = extractBucket(request.bucketId(), messageId);

        ReactionKey key = new ReactionKey(request.conversationId(), bucket, messageId, userId);
        Reaction reaction = new Reaction(key, request.emoji(), Instant.now());
        reactionRepository.save(reaction);

        List<UUID> memberIds = memberRepository.findByIdConversationId(request.conversationId()).stream()
                .map(m -> m.getId().getUserId())
                .toList();

        ReactionResponse resp = new ReactionResponse(request.conversationId(), bucket, messageId, userId, request.emoji(), memberIds);
        eventPublisher.publishObject(resp);
        return resp;
    }

    /** Xóa Reaction */
    public void removeReaction(UUID userId, UUID messageId, UUID conversationId, String bucketId) {
        requireMembership(userId, conversationId);
        String bucket = extractBucket(bucketId, messageId);

        ReactionKey key = new ReactionKey(conversationId, bucket, messageId, userId);
        reactionRepository.deleteById(key);

        List<UUID> memberIds = memberRepository.findByIdConversationId(conversationId).stream()
                .map(m -> m.getId().getUserId())
                .toList();

        ReactionResponse resp = new ReactionResponse(conversationId, bucket, messageId, userId, "", memberIds);
        eventPublisher.publishObject(resp);
    }

    /** Lấy danh sách Reaction của tin nhắn */
    public List<Reaction> getReactions(UUID conversationId, String bucketId, UUID messageId) {
        String bucket = extractBucket(bucketId, messageId);
        return reactionRepository.findByKeyConversationIdAndKeyBucketIdAndKeyMessageId(conversationId, bucket, messageId);
    }

    /** Thu hồi tin nhắn trong vòng 5 phút */
    public void recallMessage(UUID userId, UUID conversationId, String bucketId, UUID messageId) {
        requireMembership(userId, conversationId);
        String bucket = extractBucket(bucketId, messageId);

        MessageKey key = new MessageKey(conversationId, bucket, messageId);
        Message message = messageRepository.findById(key)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tin nhắn"));

        if (!message.getSenderId().equals(userId)) {
            throw new BadRequestException("Chỉ người gửi mới có quyền thu hồi tin nhắn");
        }

        long epochMs = Uuids.unixTimestamp(messageId);
        if (System.currentTimeMillis() - epochMs > 5 * 60 * 1000) {
            throw new BadRequestException("Chỉ được thu hồi tin nhắn trong vòng 5 phút");
        }

        message.setContent("[Tin nhắn đã được thu hồi]");
        message.setMediaUrls(List.of());
        messageRepository.save(message);

        List<UUID> memberIds = memberRepository.findByIdConversationId(conversationId).stream()
                .map(m -> m.getId().getUserId())
                .toList();

        eventPublisher.publishObject(new MessageRecalledEvent(conversationId, bucket, messageId, memberIds));
    }

    /** Lịch sử tin nhắn của một bucket */
    @Transactional(readOnly = true)
    public List<MessageResponse> getHistory(UUID userId, UUID conversationId, String bucketId, UUID before, int size) {
        requireMembership(userId, conversationId);
        String bucket = extractBucket(bucketId, before);
        int pageSize = Math.min(Math.max(size, 1), 100);
        Slice<Message> slice;
        if (before != null) {
            slice = messageRepository.findByKeyConversationIdAndKeyBucketIdAndKeyMessageIdLessThan(
                    conversationId, bucket, before, CassandraPageRequest.of(0, pageSize));
        } else {
            slice = messageRepository.findByKeyConversationIdAndKeyBucketId(
                    conversationId, bucket, CassandraPageRequest.of(0, pageSize));
        }
        return slice.getContent().stream().map(this::toResponse).toList();
    }

    private void requireMembership(UUID userId, UUID conversationId) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new ConversationNotFoundException("Conversation không tồn tại: " + conversationId);
        }
        if (!memberRepository.existsByIdConversationIdAndIdUserId(conversationId, userId)) {
            throw new NotMemberException("User " + userId + " không thuộc conversation " + conversationId);
        }
    }

    private String extractBucket(String providedBucketId, UUID messageId) {
        if (providedBucketId != null && !providedBucketId.isBlank()) {
            return providedBucketId;
        }
        if (messageId != null && messageId.version() == 1) {
            long timestamp = Uuids.unixTimestamp(messageId);
            return YearMonth.from(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())).toString();
        }
        return currentBucket();
    }

    private String currentBucket() {
        return YearMonth.now().toString(); // YYYY-MM
    }

    private ConversationResponse toResponse(Conversation conversation) {
        List<UUID> memberIds = memberRepository.findByIdConversationId(conversation.getId()).stream()
                .map(m -> m.getId().getUserId())
                .toList();
        return new ConversationResponse(
                conversation.getId(), conversation.getType(), conversation.getName(), conversation.getAvatarUrl(),
                memberIds, conversation.getCreatedAt());
    }

    private MessageResponse toResponse(Message message) {
        UUID messageId = message.getKey().getMessageId();
        return new MessageResponse(
                message.getKey().getConversationId(),
                messageId,
                message.getSenderId(),
                message.getContent(),
                message.getMediaUrls() == null ? List.of() : message.getMediaUrls(),
                message.getStatus(),
                Instant.ofEpochMilli(Uuids.unixTimestamp(messageId)),
                message.getReplyToId(),
                message.getReplyToContent(),
                message.getReplyToSenderId());
    }
}
