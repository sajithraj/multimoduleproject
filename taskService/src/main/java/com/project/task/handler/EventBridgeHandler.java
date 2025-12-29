package com.project.task.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.project.task.model.EventBridgeEventType;
import com.project.task.service.TaskService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Handles EventBridge events - both scheduled tasks and custom business events.
 * Detects event type and routes to appropriate handler.
 */
public class EventBridgeHandler {

    private static final Logger log = LogManager.getLogger(EventBridgeHandler.class);
    private final TaskService taskService;

    public EventBridgeHandler(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Handle EventBridge event - detects type and routes appropriately.
     *
     * @param event   EventBridge/Scheduled event
     * @param context Lambda context
     * @return "OK" string
     */
    public String handle(ScheduledEvent event, Context context) {

        String source = event.getSource();
        String detailType = event.getDetailType();

        log.info("Handling EventBridge event: source={}, detailType={}", source, detailType);

        try {
            EventBridgeEventType eventType = detectEventType(event);

            return switch (eventType) {
                case SCHEDULED_TASK -> handleScheduledTask(event, context);
                case CUSTOM_BUSINESS_EVENT -> handleCustomEvent(event, context);
                case SYSTEM_EVENT -> handleSystemEvent(event, context);
            };

        } catch (Exception e) {
            log.error("Error handling EventBridge event: {}", e.getMessage(), e);
            throw new RuntimeException("EventBridge event processing failed", e);
        }
    }

    /**
     * Detect EventBridge event type based on source and detail-type.
     */
    private EventBridgeEventType detectEventType(ScheduledEvent event) {
        String source = event.getSource();
        String detailType = event.getDetailType();

        // Scheduled tasks from CloudWatch Events
        if ("aws.events".equals(source) && "Scheduled Event".equals(detailType)) {
            log.debug("Detected: Scheduled Task (CloudWatch Events)");
            return EventBridgeEventType.SCHEDULED_TASK;
        }

        // Custom business events from our application
        if (source != null && source.startsWith("com.project")) {
            log.debug("Detected: Custom Business Event");
            return EventBridgeEventType.CUSTOM_BUSINESS_EVENT;
        }

        // AWS system events
        if (source != null && source.startsWith("aws.")) {
            log.debug("Detected: AWS System Event");
            return EventBridgeEventType.SYSTEM_EVENT;
        }

        // Default to custom event
        log.debug("Detected: Custom Event (default)");
        return EventBridgeEventType.CUSTOM_BUSINESS_EVENT;
    }

    /**
     * Handle scheduled tasks (cron/rate expressions).
     * Examples: Daily reports, cleanup jobs, periodic tasks
     */
    private String handleScheduledTask(ScheduledEvent event, Context context) {
        log.info("Processing scheduled task");

        // Extract schedule information from detail
        Map<String, Object> detail = event.getDetail();

        log.debug("Scheduled task detail: {}", detail);

        // Process scheduled task
        taskService.processScheduledTask(event, context);

        log.info("Scheduled task completed successfully");
        return "OK";
    }

    /**
     * Handle custom business events.
     * Examples: OrderCreated, PaymentProcessed, UserRegistered
     */
    private String handleCustomEvent(ScheduledEvent event, Context context) {
        log.info("Processing custom business event: detailType={}", event.getDetailType());

        String detailType = event.getDetailType();
        Map<String, Object> detail = event.getDetail();

        log.debug("Custom event detail: {}", detail);

        // Route based on detail type
        if (detailType.contains("Order")) {
            taskService.processOrderEvent(event, context);
        } else if (detailType.contains("Payment")) {
            taskService.processPaymentEvent(event, context);
        } else if (detailType.contains("User")) {
            taskService.processUserEvent(event, context);
        } else {
            // Generic custom event handler
            taskService.processCustomEvent(event, context);
        }

        log.info("Custom event processed successfully");
        return "OK";
    }

    /**
     * Handle AWS system events.
     * Examples: EC2 state changes, S3 events, CloudWatch alarms
     */
    private String handleSystemEvent(ScheduledEvent event, Context context) {
        log.info("Processing AWS system event: source={}", event.getSource());

        String source = event.getSource();

        log.debug("System event source: {}", source);

        // Process system event
        taskService.processSystemEvent(event, context);

        log.info("System event processed successfully");
        return "OK";
    }
}

