package com.chat.messaging.reaction;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReactionRepository extends CassandraRepository<Reaction, ReactionKey> {

    List<Reaction> findByKeyConversationIdAndKeyBucketIdAndKeyMessageId(
            UUID conversationId, String bucketId, UUID messageId);
}
