package com.lqc.common.model;

import java.time.LocalDateTime;
import java.util.List;

public class Message {
    private long id;
    private Long roomId;
    private long senderId;
    private String senderName;
    private Long recipientId;
    private String content;
    private MessageType messageType;
    private LocalDateTime createdAt;
    private FileAttachment attachment;
    private List<Reaction> reactions;

    public enum MessageType {
        TEXT, FILE, SYSTEM
    }

    public Message() {}

    public Message(long senderId, String senderName, String content) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.messageType = MessageType.TEXT;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public long getSenderId() { return senderId; }
    public void setSenderId(long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public FileAttachment getAttachment() { return attachment; }
    public void setAttachment(FileAttachment attachment) { this.attachment = attachment; }

    public List<Reaction> getReactions() { return reactions; }
    public void setReactions(List<Reaction> reactions) { this.reactions = reactions; }
}
