package com.project.task.model;

import lombok.Getter;

@Getter
public enum InvocationType {
    API_GATEWAY("API Gateway"),
    SQS("SQS"),
    EVENT_BRIDGE("EventBridge");

    private final String displayName;

    InvocationType(String displayName) {
        this.displayName = displayName;
    }

}

