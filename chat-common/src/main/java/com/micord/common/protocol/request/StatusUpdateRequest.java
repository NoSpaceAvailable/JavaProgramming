package com.micord.common.protocol.request;

import com.micord.common.model.UserStatus;

public class StatusUpdateRequest {
    private UserStatus status;

    public StatusUpdateRequest() {}

    public StatusUpdateRequest(UserStatus status) {
        this.status = status;
    }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
}
