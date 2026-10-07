package com.chat.messaging.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** Tìm cuộc trò chuyện DIRECT đã tồn tại giữa đúng 2 user (bất kể thứ tự). */
    @Query(value = """
            SELECT c.id FROM conversations c
            JOIN conversation_members m1 ON m1.conversation_id = c.id AND m1.user_id = :userA
            JOIN conversation_members m2 ON m2.conversation_id = c.id AND m2.user_id = :userB
            WHERE c.type = 'DIRECT'
            LIMIT 1
            """, nativeQuery = true)
    Optional<UUID> findDirectConversationBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
