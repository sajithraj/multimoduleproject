package com.project.task.model;

/**
 * Enum representing different types of event sources.
 */
public enum EventSourceType {
    API_GATEWAY("API Gateway"),
    SQS("SQS"),
    EVENTBRIDGE("EventBridge"),
    EVENT_BRIDGE("EventBridge"),  // Alias for EVENTBRIDGE
    UNKNOWN("Unknown");

    private final String displayName;

    EventSourceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

