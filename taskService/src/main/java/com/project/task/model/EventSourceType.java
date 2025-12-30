package com.project.task.model;

import lombok.Getter;

@Getter
public enum EventSourceType {

    API_GATEWAY("API Gateway"),
    SQS("SQS"),
    EVENT_BRIDGE("EventBridge");

    private final String displayName;

    EventSourceType(String displayName) {
        this.displayName = displayName;
    }

}

