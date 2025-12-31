package com.project.task.util;

import com.project.task.model.EventbridgeInvocationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class EventbridgeInvocationTypeDetector {

    private static final Logger log = LogManager.getLogger(EventbridgeInvocationTypeDetector.class);

    public EventbridgeInvocationTypeDetector() {
    }

    public static EventbridgeInvocationType detectEventBridgeType(String source, String detailType) {

        if (source.startsWith("com.custom") && detailType.startsWith("custom-event")) {
            log.debug("Detected EventBridge Custom Event");
            return EventbridgeInvocationType.EVENT_BRIDGE_CUSTOM_EVENT;
        }

        if ("aws.events".equals(source) && "Scheduled Event".equals(detailType)) {
            log.debug("Detected EventBridge Scheduled Event");
            return EventbridgeInvocationType.EVENT_BRIDGE_SCHEDULED_EVENT;
        }

        if ("aws.s3".equals(source) && "Scheduled Event".equals(detailType)) {
            log.debug("Detected Eventbridge S3 Event");
            return EventbridgeInvocationType.EVENT_BRIDGE_S3_EVENT;
        }

        log.error("Unable to determine event bridge based on the source {} & detail type {} .", source, detailType);
        throw new IllegalArgumentException("Unable to determine event bridge based on the source " + source + " & detail type " + detailType);
    }

}
