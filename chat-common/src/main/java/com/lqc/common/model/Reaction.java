package com.lqc.common.model;

import java.time.LocalDateTime;

public class Reaction {
    private long id;
    private long messageId;
    private long userId;
    private String emoji;
    private LocalDateTime createdAt;

    public Reaction() {}

    public Reaction(long messageId, long userId, String emoji) {
        this.messageId = messageId;
        this.userId = userId;
        this.emoji = emoji;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
