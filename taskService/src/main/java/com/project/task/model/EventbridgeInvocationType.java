package com.project.task.model;

import lombok.Getter;

@Getter
public enum EventbridgeInvocationType {
    EVENT_BRIDGE_SCHEDULED_EVENT("EventBridge Scheduled Event"),
    EVENT_BRIDGE_CUSTOM_EVENT("EventBridge Custom Event"),
    EVENT_BRIDGE_S3_EVENT("S3 Event");

    private final String displayName;

    EventbridgeInvocationType(String displayName) {
        this.displayName = displayName;
    }

}

