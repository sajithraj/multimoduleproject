package com.project.task.model;

public record EventDetectionResult(
        InvocationType invocationType,
        Object deserializedEvent
) {
}

