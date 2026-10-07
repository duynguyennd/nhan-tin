package com.chat.messaging.meme;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "memes")
public class Meme {

    @Id
    private UUID id;
    private String url;
    private String emotion;

    public Meme() {
        this.id = UUID.randomUUID();
    }

    public Meme(UUID id, String url, String emotion) {
        this.id = id != null ? id : UUID.randomUUID();
        this.url = url;
        this.emotion = emotion;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }
}
