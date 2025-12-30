package com.project.service.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Utils {

    private static final Logger log = LogManager.getLogger(Utils.class);

    public static String getRequiredEnv(String key) {
        String value = System.getenv(key);

        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }

        if (value == null || value.trim().isEmpty()) {
            log.error("Required environment variable not set: {}", key);
            throw new IllegalStateException("Required environment variable not set: " + key);
        }
        log.debug("Using environment variable: {} = {}", key, maskSensitive(value));
        return value.trim();
    }

    private static String maskSensitive(String value) {
        if (value == null || value.length() <= 6) {
            return "***";
        }
        return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
    }

}
