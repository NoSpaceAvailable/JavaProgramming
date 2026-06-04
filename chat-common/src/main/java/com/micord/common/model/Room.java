package com.micord.common.model;

import java.time.LocalDateTime;

public class Room {
    private long id;
    private String name;
    private String description;
    private long ownerId;
    private boolean isPrivate;
    private LocalDateTime createdAt;

    public Room() {}

    public Room(long id, String name, long ownerId) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name; }
}
