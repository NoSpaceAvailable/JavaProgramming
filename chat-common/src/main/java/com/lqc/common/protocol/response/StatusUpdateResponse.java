package com.lqc.common.protocol.response;

import com.lqc.common.model.UserStatus;

public class StatusUpdateResponse {
    private boolean success;
    private String message;
    private UserStatus status;

    public StatusUpdateResponse() {}

    public StatusUpdateResponse(boolean success, String message, UserStatus status) {
        this.success = success;
        this.message = message;
        this.status = status;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
}
