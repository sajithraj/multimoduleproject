package com.project.task.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Task request model that encapsulates the input from any event source.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    private EventSourceType sourceType;
    private String eventId;
    private String requestBody;
    private Map<String, Object> metadata;
    @Builder.Default
    private long timestamp = System.currentTimeMillis();
}

