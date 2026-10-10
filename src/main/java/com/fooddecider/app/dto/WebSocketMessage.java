package com.fooddecider.app.dto;

public class WebSocketMessage {

    // Event type constants for clarity across the codebase
    public static final String TYPE_MEMBER_JOINED   = "MEMBER_JOINED";
    public static final String TYPE_SWIPE_PROGRESS  = "SWIPE_PROGRESS";
    public static final String TYPE_MATCH_FOUND     = "MATCH_FOUND";
    public static final String TYPE_ROOM_EXPIRED    = "ROOM_EXPIRED";
    public static final String TYPE_VOTING_STARTED  = "VOTING_STARTED";

    private String type;
    private Object payload;

    public WebSocketMessage() {}

    public WebSocketMessage(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    // --- Getters ---

    public String getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }

    // --- Setters ---

    public void setType(String type) {
        this.type = type;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}
