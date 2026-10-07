package com.chat.messaging.message;

import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Khóa chính của bảng messages trong Cassandra:
 * partition key = (conversation_id, bucket_id), clustering key = message_id DESC.
 */
@PrimaryKeyClass
public class MessageKey implements Serializable {

    @PrimaryKeyColumn(name = "conversation_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID conversationId;

    /** YYYY-MM: phân vùng theo tháng tránh partition quá lớn */
    @PrimaryKeyColumn(name = "bucket_id", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    private String bucketId;

    /** TimeUUID: duy nhất + chứa mốc thời gian, sắp xếp giảm dần (mới nhất trước) */
    @PrimaryKeyColumn(name = "message_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private UUID messageId;

    public MessageKey() {
    }

    public MessageKey(UUID conversationId, String bucketId, UUID messageId) {
        this.conversationId = conversationId;
        this.bucketId = bucketId;
        this.messageId = messageId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageKey that)) {
            return false;
        }
        return Objects.equals(conversationId, that.conversationId)
                && Objects.equals(bucketId, that.bucketId)
                && Objects.equals(messageId, that.messageId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, bucketId, messageId);
    }
}
