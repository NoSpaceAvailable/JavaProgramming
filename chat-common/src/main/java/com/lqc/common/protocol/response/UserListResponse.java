package com.lqc.common.protocol.response;

import com.lqc.common.model.User;

import java.util.List;

public class UserListResponse {
    private List<User> users;

    public UserListResponse() {}

    public UserListResponse(List<User> users) {
        this.users = users;
    }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }
}
