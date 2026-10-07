package com.chat.messaging.reaction;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@PrimaryKeyClass
public class ReactionKey implements Serializable {

    @PrimaryKeyColumn(name = "conversation_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private UUID conversationId;

    @PrimaryKeyColumn(name = "bucket_id", ordinal = 1, type = PrimaryKeyType.PARTITIONED)
    private String bucketId;

    @PrimaryKeyColumn(name = "message_id", ordinal = 2, type = PrimaryKeyType.PARTITIONED)
    private UUID messageId;

    @PrimaryKeyColumn(name = "user_id", ordinal = 3, type = PrimaryKeyType.CLUSTERED)
    private UUID userId;

    public ReactionKey() {}

    public ReactionKey(UUID conversationId, String bucketId, UUID messageId, UUID userId) {
        this.conversationId = conversationId;
        this.bucketId = bucketId;
        this.messageId = messageId;
        this.userId = userId;
    }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public String getBucketId() { return bucketId; }
    public void setBucketId(String bucketId) { this.bucketId = bucketId; }

    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReactionKey that = (ReactionKey) o;
        return Objects.equals(conversationId, that.conversationId) &&
               Objects.equals(bucketId, that.bucketId) &&
               Objects.equals(messageId, that.messageId) &&
               Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conversationId, bucketId, messageId, userId);
    }
}
