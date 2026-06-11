package com.micord.common.protocol.request;

/** Joins a server using its invite code. */
public class JoinServerRequest {
    private String inviteCode;

    public JoinServerRequest() {}

    public JoinServerRequest(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
}
