package com.lqc.common.protocol.request;

import com.lqc.common.model.UserStatus;

public class StatusUpdateRequest {
    private UserStatus status;

    public StatusUpdateRequest() {}

    public StatusUpdateRequest(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
}
