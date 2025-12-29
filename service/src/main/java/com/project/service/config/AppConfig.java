package com.project.service.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service Application Configuration.
 * Reads configuration from Lambda environment variables.
 * All values must be provided via Lambda environment variables.
 */
public final class AppConfig {

    private static final Logger log = LogManager.getLogger(AppConfig.class);

    // Read from Lambda environment variables (all required)
    public static final String TOKEN_ENDPOINT_URL = getRequiredEnv("TOKEN_ENDPOINT_URL");
    public static final String EXTERNAL_API_URL = getRequiredEnv("EXTERNAL_API_URL");
    public static final String TOKEN_SECRET_NAME = getRequiredEnv("TOKEN_SECRET_NAME");

    static {
        log.info("Service configuration initialized successfully");
        log.debug("Token endpoint: {}", TOKEN_ENDPOINT_URL);
        log.debug("External API URL: {}", EXTERNAL_API_URL);
        log.debug("Token secret name: {}", TOKEN_SECRET_NAME);
    }

    /**
     * Retrieves required environment variable.
     * Throws exception if not set.
     * Supports system properties as fallback for testing.
     *
     * @param key Environment variable name
     * @return Environment variable value
     * @throws IllegalStateException if variable not set
     */
    private static String getRequiredEnv(String key) {
        // Try environment variable first (production)
        String value = System.getenv(key);

        // Fallback to system property (testing)
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

    /**
     * Masks sensitive values in logs (shows only first and last 3 chars).
     *
     * @param value Value to mask
     * @return Masked value
     */
    private static String maskSensitive(String value) {
        if (value == null || value.length() <= 6) {
            return "***";
        }
        return value.substring(0, 3) + "***" + value.substring(value.length() - 3);
    }

    private AppConfig() {
    }
}

