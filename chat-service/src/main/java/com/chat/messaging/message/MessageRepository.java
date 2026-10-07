package com.chat.messaging.message;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface MessageRepository extends CassandraRepository<Message, MessageKey> {

    /** Phân trang theo thời gian ngược trong một bucket (YYYY-MM). */
    Slice<Message> findByKeyConversationIdAndKeyBucketId(UUID conversationId, String bucketId, Pageable pageable);

    /** Phân trang cursor-based theo thời gian ngược: lấy các tin nhắn cũ hơn messageId (tức messageId nhỏ hơn messageId mốc). */
    Slice<Message> findByKeyConversationIdAndKeyBucketIdAndKeyMessageIdLessThan(UUID conversationId, String bucketId, UUID messageId, Pageable pageable);
}
