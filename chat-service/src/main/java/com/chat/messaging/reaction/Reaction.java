package com.chat.messaging.reaction;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("message_reactions")
public class Reaction {

    @PrimaryKey
    private ReactionKey key;

    @Column("emoji")
    private String emoji;

    @Column("created_at")
    private Instant createdAt;

    public Reaction() {}

    public Reaction(ReactionKey key, String emoji, Instant createdAt) {
        this.key = key;
        this.emoji = emoji;
        this.createdAt = createdAt;
    }

    public ReactionKey getKey() { return key; }
    public void setKey(ReactionKey key) { this.key = key; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
