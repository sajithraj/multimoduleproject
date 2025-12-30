package com.project.task.integration;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Base test class with common utilities for integration tests
 */
public abstract class BaseIntegrationTest {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Convert typed AWS event to Map as AWS Lambda does
     */
    protected Map<String, Object> convertToMap(Object event) {
        return objectMapper.convertValue(event, Map.class);
    }
}

