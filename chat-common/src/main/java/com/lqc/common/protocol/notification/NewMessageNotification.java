package com.lqc.common.protocol.notification;

public class NewMessageNotification {
    private long messageId;
    private long roomId;
    private long senderId;
    private String senderName;
    private String content;
    private String messageType;
    private long timestamp;
    private Long fileAttachmentId;
    private String fileName;

    public NewMessageNotification() {}

    public long getMessageId() { return messageId; }
    public void setMessageId(long messageId) { this.messageId = messageId; }

    public long getRoomId() { return roomId; }
    public void setRoomId(long roomId) { this.roomId = roomId; }

    public long getSenderId() { return senderId; }
    public void setSenderId(long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Long getFileAttachmentId() { return fileAttachmentId; }
    public void setFileAttachmentId(Long fileAttachmentId) { this.fileAttachmentId = fileAttachmentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
