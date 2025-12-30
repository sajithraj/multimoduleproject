package com.project.task.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Task {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @JsonProperty("createdAt")
    @Builder.Default
    private Long createdAt = Instant.now().toEpochMilli();

    @JsonProperty("updatedAt")
    @Builder.Default
    private Long updatedAt = Instant.now().toEpochMilli();

    public enum TaskStatus {
        TODO,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}

