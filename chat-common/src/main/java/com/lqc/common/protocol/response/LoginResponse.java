package com.lqc.common.protocol.response;

import com.lqc.common.model.Room;
import com.lqc.common.model.User;

import java.util.List;

public class LoginResponse {
    private boolean success;
    private String message;
    private long userId;
    private String displayName;
    private List<Room> rooms;

    public LoginResponse() {}

    public LoginResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public LoginResponse(boolean success, long userId, String displayName, List<Room> rooms) {
        this.success = success;
        this.userId = userId;
        this.displayName = displayName;
        this.rooms = rooms;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public List<Room> getRooms() { return rooms; }
    public void setRooms(List<Room> rooms) { this.rooms = rooms; }
}
