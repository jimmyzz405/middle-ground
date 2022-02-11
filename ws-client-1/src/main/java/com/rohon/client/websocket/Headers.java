package com.rohon.client.websocket;

public enum Headers {

    TOKEN("Rohon-Token"),

    SERVICE_NAME("Service-Name");

    private final String header;

    Headers(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }
}
