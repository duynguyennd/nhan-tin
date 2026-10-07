package com.chat.messaging.message;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.List;
import java.util.UUID;

@Table("messages")
public class Message {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_READ = "READ";

    @PrimaryKey
    private MessageKey key;

    @Column("sender_id")
    private UUID senderId;

    @Column("content")
    private String content;

    @Column("media_urls")
    private List<String> mediaUrls;

    /** SENT, DELIVERED, READ */
    @Column("status")
    private String status;

    @Column("reply_to_id")
    private UUID replyToId;

    @Column("reply_to_content")
    private String replyToContent;

    @Column("reply_to_sender_id")
    private UUID replyToSenderId;

    public MessageKey getKey() {
        return key;
    }

    public void setKey(MessageKey key) {
        this.key = key;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getReplyToId() {
        return replyToId;
    }

    public void setReplyToId(UUID replyToId) {
        this.replyToId = replyToId;
    }

    public String getReplyToContent() {
        return replyToContent;
    }

    public void setReplyToContent(String replyToContent) {
        this.replyToContent = replyToContent;
    }

    public UUID getReplyToSenderId() {
        return replyToSenderId;
    }

    public void setReplyToSenderId(UUID replyToSenderId) {
        this.replyToSenderId = replyToSenderId;
    }
}
