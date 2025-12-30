package com.project.task.model;

import lombok.Getter;

@Getter
public enum EventBridgeEventType {

    SCHEDULED_EVENT("Scheduled Task"),

    CUSTOM_EVENT("Custom Business Event");

    private final String displayName;

    EventBridgeEventType(String displayName) {
        this.displayName = displayName;
    }

}
