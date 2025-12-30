package com.project.task.model;

import lombok.Getter;

@Getter
public enum InvocationType {
    API_GATEWAY("API Gateway"),
    SQS("SQS"),
    EVENT_BRIDGE_SCHEDULED("EventBridge Scheduled Event"),
    EVENT_BRIDGE_CUSTOM("EventBridge Custom Event");

    private final String displayName;

    InvocationType(String displayName) {
        this.displayName = displayName;
    }

}

