package com.project.task.model;

/**
 * Enum representing different types of Lambda invocations.
 */
public enum InvocationType {
    API_GATEWAY("API Gateway"),
    SQS("SQS"),
    EVENT_BRIDGE("EventBridge");

    private final String displayName;

    InvocationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

