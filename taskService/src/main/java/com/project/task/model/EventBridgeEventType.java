package com.project.task.model;

/**
 * Types of EventBridge events supported.
 */
public enum EventBridgeEventType {
    /**
     * Scheduled tasks triggered by CloudWatch Events (cron/rate expressions).
     * Example: Daily reports, cleanup jobs
     */
    SCHEDULED_TASK("Scheduled Task"),

    /**
     * Custom business events from our application.
     * Example: OrderCreated, PaymentProcessed, UserRegistered
     */
    CUSTOM_BUSINESS_EVENT("Custom Business Event"),

    /**
     * AWS system events.
     * Example: EC2 state changes, S3 events, CloudWatch alarms
     */
    SYSTEM_EVENT("System Event");

    private final String displayName;

    EventBridgeEventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

