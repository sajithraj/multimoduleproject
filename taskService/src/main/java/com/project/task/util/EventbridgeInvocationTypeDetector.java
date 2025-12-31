package com.project.task.util;

import com.project.task.model.EventbridgeInvocationType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//https://docs.aws.amazon.com/eventbridge/latest/ref/events.html
public final class EventbridgeInvocationTypeDetector {

    private static final Logger log = LogManager.getLogger(EventbridgeInvocationTypeDetector.class);

    private EventbridgeInvocationTypeDetector() {
    }

    public static EventbridgeInvocationType detectEventBridgeType(String source, String detailType) {

        String src = source == null ? "" : source.trim();
        String detail = detailType == null ? "" : detailType.trim();

        if (src.startsWith("com.custom") && detail.startsWith("custom-event")) {
            log.debug("Detected EventBridge Custom Event (source='{}', detailType='{}')", src, detail);
            return EventbridgeInvocationType.EVENT_BRIDGE_CUSTOM_EVENT;
        }

        if ("aws.events".equals(src) && "Scheduled Event".equals(detail)) {
            log.debug("Detected EventBridge Scheduled Event (source='{}', detailType='{}')", src, detail);
            return EventbridgeInvocationType.EVENT_BRIDGE_SCHEDULED_EVENT;
        }

        if ("aws.s3".equals(src) && "Scheduled Event".equals(detail)) {
            log.debug("Detected EventBridge S3 Event (source='{}', detailType='{}')", src, detail);
            return EventbridgeInvocationType.EVENT_BRIDGE_S3_EVENT;
        }

        log.error("Unable to determine EventBridge type from source='{}' and detailType='{}'.", src, detail);
        throw new IllegalArgumentException("Unable to determine EventBridge type from source: " + src + " & detailType: " + detail);
    }

}
