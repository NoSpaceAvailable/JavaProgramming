package com.micord.common.protocol.request;

/** Creates a new community server (an initial "general" text channel is created with it). */
public class CreateServerRequest {
    private String name;

    public CreateServerRequest() {}

    public CreateServerRequest(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
