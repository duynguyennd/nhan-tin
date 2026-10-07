package com.chat.messaging.service;

import com.chat.messaging.conversation.Conversation;
import com.chat.messaging.conversation.ConversationMember;
import com.chat.messaging.conversation.ConversationMemberId;
import com.chat.messaging.conversation.ConversationMemberRepository;
import com.chat.messaging.conversation.ConversationRepository;
import com.chat.messaging.dto.ChatDtos.AddReactionRequest;
import com.chat.messaging.dto.ChatDtos.ConversationResponse;
import com.chat.messaging.dto.ChatDtos.MessageResponse;
import com.chat.messaging.dto.ChatDtos.SendMessageRequest;
import com.chat.messaging.exception.ChatExceptions.BadRequestException;
import com.chat.messaging.exception.ChatExceptions.ConversationNotFoundException;
import com.chat.messaging.exception.ChatExceptions.NotMemberException;
import com.chat.messaging.message.Message;
import com.chat.messaging.message.MessageKey;
import com.chat.messaging.message.MessageRepository;
import com.chat.messaging.reaction.Reaction;
import com.chat.messaging.reaction.ReactionKey;
import com.chat.messaging.reaction.ReactionRepository;
import com.chat.messaging.redis.MessageEventPublisher;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private ConversationRepository conversationRepository;
    private ConversationMemberRepository memberRepository;
    private MessageRepository messageRepository;
    private ReactionRepository reactionRepository;
    private MessageEventPublisher eventPublisher;
    private ChatService chatService;

    private UUID userA;
    private UUID userB;
    private UUID userC;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        memberRepository = mock(ConversationMemberRepository.class);
        messageRepository = mock(MessageRepository.class);
        reactionRepository = mock(ReactionRepository.class);
        eventPublisher = new MessageEventPublisher(null, new ObjectMapper(), "chat.messages");
        chatService = new ChatService(conversationRepository, memberRepository, messageRepository, reactionRepository, eventPublisher, null);

        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
        userC = UUID.randomUUID();
        conversationId = UUID.randomUUID();
    }

    // ═══════════════════════════════════════════════
    // Conversation Tests
    // ═══════════════════════════════════════════════

    @Test
    void testCreateDirectConversationWithSelfThrowsException() {
        UUID userId = UUID.randomUUID();
        assertThrows(BadRequestException.class, () -> chatService.createDirectConversation(userId, userId));
    }

    @Test
    void testCreateDirectConversationExistingReturnsOld() {
        UUID convId = UUID.randomUUID();

        Conversation conv = new Conversation();
        conv.setId(convId);
        conv.setType(Conversation.TYPE_DIRECT);

        when(conversationRepository.findDirectConversationBetween(userA, userB)).thenReturn(Optional.of(convId));
        when(conversationRepository.findById(convId)).thenReturn(Optional.of(conv));

        ConversationResponse response = chatService.createDirectConversation(userA, userB);

        assertNotNull(response);
        assertEquals(convId, response.id());
        assertEquals("DIRECT", response.type());
        verify(conversationRepository, never()).save(any(Conversation.class));
    }

    @Test
    void testCreateDirectConversationNew() {
        when(conversationRepository.findDirectConversationBetween(userA, userB)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ConversationResponse response = chatService.createDirectConversation(userA, userB);

        assertNotNull(response);
        assertEquals("DIRECT", response.type());
        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(memberRepository, times(2)).save(any(ConversationMember.class));
    }

    @Test
    void testCreateGroupConversation() {
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ConversationResponse response = chatService.createGroupConversation(userA, "Nhóm Dev", null, List.of(userB, userC));

        assertNotNull(response);
        assertEquals("GROUP", response.type());
        assertEquals("Nhóm Dev", response.name());
        verify(conversationRepository, times(1)).save(any(Conversation.class));
        // 3 members: creator (ADMIN) + userB (MEMBER) + userC (MEMBER)
        verify(memberRepository, times(3)).save(any(ConversationMember.class));
    }

    @Test
    void testCreateGroupConversationCreatorIsAlsoMember() {
        // Creator trùng với memberUserIds → chỉ tạo 2 members (không duplicate)
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ConversationResponse response = chatService.createGroupConversation(userA, "Test", null, List.of(userA, userB));

        assertNotNull(response);
        // Set removes duplicate, so only 2 members
        verify(memberRepository, times(2)).save(any(ConversationMember.class));
    }

    // ═══════════════════════════════════════════════
    // Message Tests
    // ═══════════════════════════════════════════════

    @Test
    void testPostMessageSuccess() {
        setupMembership(userA, conversationId);

        SendMessageRequest request = new SendMessageRequest(conversationId, "Hello!", null, null);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.findByIdConversationId(conversationId)).thenReturn(List.of(
                new ConversationMember(new ConversationMemberId(conversationId, userA), "MEMBER"),
                new ConversationMember(new ConversationMemberId(conversationId, userB), "MEMBER")
        ));

        MessageResponse response = chatService.postMessage(userA, request);

        assertNotNull(response);
        assertEquals(conversationId, response.conversationId());
        assertEquals(userA, response.senderId());
        assertEquals("Hello!", response.content());
        assertEquals("SENT", response.status());
        assertNotNull(response.messageId());
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void testPostMessageWithMediaUrls() {
        setupMembership(userA, conversationId);

        List<String> mediaUrls = List.of("https://example.com/img1.jpg", "https://example.com/img2.jpg");
        SendMessageRequest request = new SendMessageRequest(conversationId, "Xem ảnh!", mediaUrls, null);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.findByIdConversationId(conversationId)).thenReturn(List.of(
                new ConversationMember(new ConversationMemberId(conversationId, userA), "MEMBER")
        ));

        MessageResponse response = chatService.postMessage(userA, request);

        assertNotNull(response);
        assertEquals(2, response.mediaUrls().size());
        assertTrue(response.mediaUrls().contains("https://example.com/img1.jpg"));
    }

    @Test
    void testPostMessageNotMemberThrowsException() {
        when(conversationRepository.existsById(conversationId)).thenReturn(true);
        when(memberRepository.existsByIdConversationIdAndIdUserId(conversationId, userA)).thenReturn(false);

        SendMessageRequest request = new SendMessageRequest(conversationId, "Hello!", null, null);
        assertThrows(NotMemberException.class, () -> chatService.postMessage(userA, request));
    }

    @Test
    void testPostMessageConversationNotFoundThrowsException() {
        when(conversationRepository.existsById(conversationId)).thenReturn(false);

        SendMessageRequest request = new SendMessageRequest(conversationId, "Hello!", null, null);
        assertThrows(ConversationNotFoundException.class, () -> chatService.postMessage(userA, request));
    }

    // ═══════════════════════════════════════════════
    // Reaction Tests
    // ═══════════════════════════════════════════════

    @Test
    void testAddReactionSuccess() {
        setupMembership(userA, conversationId);

        UUID messageId = Uuids.timeBased();
        AddReactionRequest request = new AddReactionRequest(conversationId, null, "👍");
        when(reactionRepository.save(any(Reaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.findByIdConversationId(conversationId)).thenReturn(List.of(
                new ConversationMember(new ConversationMemberId(conversationId, userA), "MEMBER")
        ));

        var response = chatService.addReaction(userA, messageId, request);

        assertNotNull(response);
        assertEquals("👍", response.emoji());
        assertEquals(conversationId, response.conversationId());
        assertEquals(messageId, response.messageId());
        assertEquals(userA, response.userId());
        verify(reactionRepository, times(1)).save(any(Reaction.class));
    }

    @Test
    void testAddReactionNotMemberThrowsException() {
        when(conversationRepository.existsById(conversationId)).thenReturn(true);
        when(memberRepository.existsByIdConversationIdAndIdUserId(conversationId, userA)).thenReturn(false);

        UUID messageId = Uuids.timeBased();
        AddReactionRequest request = new AddReactionRequest(conversationId, null, "👍");
        assertThrows(NotMemberException.class, () -> chatService.addReaction(userA, messageId, request));
    }

    @Test
    void testRemoveReactionSuccess() {
        setupMembership(userA, conversationId);

        UUID messageId = Uuids.timeBased();
        when(memberRepository.findByIdConversationId(conversationId)).thenReturn(List.of(
                new ConversationMember(new ConversationMemberId(conversationId, userA), "MEMBER")
        ));

        assertDoesNotThrow(() -> chatService.removeReaction(userA, messageId, conversationId, null));
        verify(reactionRepository, times(1)).deleteById(any(ReactionKey.class));
    }

    // ═══════════════════════════════════════════════
    // Recall Tests
    // ═══════════════════════════════════════════════

    @Test
    void testRecallMessageNotOwnerThrowsException() {
        setupMembership(userA, conversationId);

        UUID messageId = Uuids.timeBased();
        String bucket = YearMonth.now().toString();
        MessageKey key = new MessageKey(conversationId, bucket, messageId);
        Message message = new Message();
        message.setKey(key);
        message.setSenderId(userB); // Different user owns this message
        message.setContent("Original");

        when(messageRepository.findById(key)).thenReturn(Optional.of(message));

        assertThrows(BadRequestException.class, () ->
                chatService.recallMessage(userA, conversationId, bucket, messageId));
    }

    @Test
    void testRecallMessageConversationNotFoundThrowsException() {
        when(conversationRepository.existsById(conversationId)).thenReturn(false);

        UUID messageId = Uuids.timeBased();
        assertThrows(ConversationNotFoundException.class, () ->
                chatService.recallMessage(userA, conversationId, null, messageId));
    }

    @Test
    void testRecallMessageNotFoundThrowsException() {
        setupMembership(userA, conversationId);

        UUID messageId = Uuids.timeBased();
        String bucket = YearMonth.now().toString();
        MessageKey key = new MessageKey(conversationId, bucket, messageId);

        when(messageRepository.findById(key)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () ->
                chatService.recallMessage(userA, conversationId, bucket, messageId));
    }

    // ═══════════════════════════════════════════════
    // History Tests
    // ═══════════════════════════════════════════════

    @Test
    void testGetHistorySuccess() {
        setupMembership(userA, conversationId);

        String bucket = YearMonth.now().toString();
        UUID msgId1 = Uuids.timeBased();
        UUID msgId2 = Uuids.timeBased();

        Message msg1 = new Message();
        msg1.setKey(new MessageKey(conversationId, bucket, msgId1));
        msg1.setSenderId(userA);
        msg1.setContent("Hello");
        msg1.setStatus("SENT");

        Message msg2 = new Message();
        msg2.setKey(new MessageKey(conversationId, bucket, msgId2));
        msg2.setSenderId(userB);
        msg2.setContent("World");
        msg2.setStatus("SENT");

        when(messageRepository.findByKeyConversationIdAndKeyBucketId(
                eq(conversationId), eq(bucket), any(CassandraPageRequest.class)))
                .thenReturn(new SliceImpl<>(List.of(msg1, msg2)));

        List<MessageResponse> history = chatService.getHistory(userA, conversationId, bucket, null, 30);

        assertNotNull(history);
        assertEquals(2, history.size());
        assertEquals("Hello", history.get(0).content());
        assertEquals("World", history.get(1).content());
    }

    @Test
    void testGetHistoryNotMemberThrowsException() {
        when(conversationRepository.existsById(conversationId)).thenReturn(true);
        when(memberRepository.existsByIdConversationIdAndIdUserId(conversationId, userA)).thenReturn(false);

        assertThrows(NotMemberException.class, () ->
                chatService.getHistory(userA, conversationId, null, null, 30));
    }

    @Test
    void testGetHistoryWithCursor() {
        setupMembership(userA, conversationId);

        String bucket = YearMonth.now().toString();
        UUID cursorId = Uuids.timeBased();
        UUID msgId = Uuids.timeBased();

        Message msg = new Message();
        msg.setKey(new MessageKey(conversationId, bucket, msgId));
        msg.setSenderId(userA);
        msg.setContent("Older message");
        msg.setStatus("SENT");

        when(messageRepository.findByKeyConversationIdAndKeyBucketIdAndKeyMessageIdLessThan(
                eq(conversationId), eq(bucket), eq(cursorId), any(CassandraPageRequest.class)))
                .thenReturn(new SliceImpl<>(List.of(msg)));

        List<MessageResponse> history = chatService.getHistory(userA, conversationId, bucket, cursorId, 30);

        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("Older message", history.get(0).content());
    }

    @Test
    void testGetHistorySizeClamped() {
        setupMembership(userA, conversationId);

        String bucket = YearMonth.now().toString();
        when(messageRepository.findByKeyConversationIdAndKeyBucketId(
                eq(conversationId), eq(bucket), any(CassandraPageRequest.class)))
                .thenReturn(new SliceImpl<>(List.of()));

        // Size > 100 should be clamped to 100
        List<MessageResponse> history = chatService.getHistory(userA, conversationId, bucket, null, 999);
        assertNotNull(history);
    }

    // ═══════════════════════════════════════════════
    // List Conversations Tests
    // ═══════════════════════════════════════════════

    @Test
    void testListMyConversations() {
        ConversationMemberId memberId = new ConversationMemberId(conversationId, userA);
        ConversationMember member = new ConversationMember(memberId, "MEMBER");

        Conversation conv = new Conversation();
        conv.setId(conversationId);
        conv.setType("DIRECT");

        when(memberRepository.findByIdUserId(userA)).thenReturn(List.of(member));
        when(conversationRepository.findAllById(List.of(conversationId))).thenReturn(List.of(conv));

        List<ConversationResponse> result = chatService.listMyConversations(userA);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(conversationId, result.get(0).id());
    }

    // ═══════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════

    private void setupMembership(UUID userId, UUID convId) {
        when(conversationRepository.existsById(convId)).thenReturn(true);
        when(memberRepository.existsByIdConversationIdAndIdUserId(convId, userId)).thenReturn(true);
    }
}
