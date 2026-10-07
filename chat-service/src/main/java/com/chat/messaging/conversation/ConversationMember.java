package com.chat.messaging.conversation;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "conversation_members")
public class ConversationMember {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MEMBER = "MEMBER";

    @EmbeddedId
    private ConversationMemberId id;

    /** ADMIN hoặc MEMBER */
    @Column(nullable = false, length = 20)
    private String role = ROLE_MEMBER;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    public ConversationMember() {
    }

    public ConversationMember(ConversationMemberId id, String role) {
        this.id = id;
        this.role = role;
    }

    public ConversationMemberId getId() {
        return id;
    }

    public void setId(ConversationMemberId id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
