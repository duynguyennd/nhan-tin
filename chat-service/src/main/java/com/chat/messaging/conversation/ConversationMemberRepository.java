package com.chat.messaging.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

    List<ConversationMember> findByIdUserId(UUID userId);

    List<ConversationMember> findByIdConversationId(UUID conversationId);

    boolean existsByIdConversationIdAndIdUserId(UUID conversationId, UUID userId);
}
