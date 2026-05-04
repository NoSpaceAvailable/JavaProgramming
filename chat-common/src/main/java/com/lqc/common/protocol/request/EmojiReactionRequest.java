package com.lqc.common.protocol.request;

public class EmojiReactionRequest {
    private long messageId;
    private String emoji;

    public EmojiReactionRequest() {}

    public EmojiReactionRequest(long messageId, String emoji) {
        this.messageId = messageId;
        this.emoji = emoji;
    }

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
}
