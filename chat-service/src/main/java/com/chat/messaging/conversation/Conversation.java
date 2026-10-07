package com.chat.messaging.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class Conversation {

    public static final String TYPE_DIRECT = "DIRECT";
    public static final String TYPE_GROUP = "GROUP";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** DIRECT hoặc GROUP */
    @Column(nullable = false, length = 20)
    private String type;

    /** Tên nhóm (chỉ dùng khi type = GROUP) */
    @Column(length = 100)
    private String name;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
