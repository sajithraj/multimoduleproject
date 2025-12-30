package com.project.task.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.task.data.TaskData;
import com.project.task.mapper.TaskMapper;
import com.project.task.model.Task;
import com.project.task.model.dto.TaskRequestDTO;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class EventBridgeTaskService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TaskMapper TASK_MAPPER = TaskMapper.INSTANCE;

    public String processScheduledEvent(ScheduledEvent event, Context context) {
        log.info("📅 Processing scheduled event: id={}", event.getId());

        try {
            String taskId = UUID.randomUUID().toString();
            String taskName = "scheduled event " + event.getId();

            Task task = Task.builder()
                    .id(taskId)
                    .name(taskName)
                    .description("Automatically created from EventBridge scheduled event at " + Instant.now())
                    .status(Task.TaskStatus.TODO)
                    .createdAt(Instant.now().toEpochMilli())
                    .updatedAt(Instant.now().toEpochMilli())
                    .build();

            TaskData.saveTask(task);

            log.info("Scheduled task created: id={}, name={}", taskId, taskName);
            return "OK";

        } catch (Exception e) {
            log.error("Error processing scheduled task: {}", e.getMessage(), e);
            throw new RuntimeException("Scheduled task failed: " + e.getMessage(), e);
        }
    }

    public String processCustomEvent(ScheduledEvent event, Context context) {
        log.info("Processing custom event: detailType={}, id={}", event.getDetailType(), event.getId());

        try {
            Map<String, Object> detail = event.getDetail();

            if (detail == null || detail.isEmpty()) {
                log.warn("Custom event has empty detail - skipping");
                return "SKIPPED";
            }

            log.debug("Custom event detail: {}", detail);

            TaskRequestDTO taskRequest = OBJECT_MAPPER.convertValue(detail, TaskRequestDTO.class);

            if (taskRequest.getName() == null || taskRequest.getName().trim().isEmpty()) {
                String errorMsg = "Custom event detail missing required 'name' field";
                log.error("Validation failed: {}", errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            Task task = TASK_MAPPER.toEntity(taskRequest);

            TaskData.saveTask(task);

            log.info("Custom event task created: id={}, name={}, detailType={}",
                    task.getId(), task.getName(), event.getDetailType());

            return "OK";

        } catch (IllegalArgumentException e) {
            log.error("Validation error in custom event: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error processing custom event: {}", e.getMessage(), e);
            throw new RuntimeException("Custom event processing failed: " + e.getMessage(), e);
        }
    }

}
