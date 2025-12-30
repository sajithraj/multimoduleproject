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

/**
 * EventBridgeTaskService - Handles all EventBridge events
 * <p>
 * Supports:
 * 1. Scheduled Events - Creates task with "scheduled event " + event.id
 * 2. Custom Events - Persists task from detail (TaskRequestDTO)
 */
@Slf4j
public class EventBridgeTaskService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TaskMapper TASK_MAPPER = TaskMapper.INSTANCE;

    /**
     * Process scheduled task events (cron/rate expressions)
     * Creates a task with name: "scheduled event " + event ID
     *
     * @param event   ScheduledEvent from EventBridge
     * @param context Lambda context
     * @return Success status
     */
    public String processScheduledTask(ScheduledEvent event, Context context) {
        log.info("📅 Processing scheduled event: id={}", event.getId());

        try {
            // Create task with scheduled event name
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

            // Persist to task store
            TaskData.saveTask(task);

            log.info("✅ Scheduled task created: id={}, name={}", taskId, taskName);
            return "OK";

        } catch (Exception e) {
            log.error("❌ Error processing scheduled task: {}", e.getMessage(), e);
            throw new RuntimeException("Scheduled task failed: " + e.getMessage(), e);
        }
    }

    /**
     * Process custom business events
     * Extracts TaskRequestDTO from detail tag and persists it
     * <p>
     * Detail should contain:
     * {
     * "name": "Task Name",
     * "description": "Task Description",
     * "status": "TODO" (optional)
     * }
     *
     * @param event   ScheduledEvent with custom detail
     * @param context Lambda context
     * @return Success status
     */
    public String processCustomEvent(ScheduledEvent event, Context context) {
        log.info("🎯 Processing custom event: detailType={}, id={}", event.getDetailType(), event.getId());

        try {
            Map<String, Object> detail = event.getDetail();

            if (detail == null || detail.isEmpty()) {
                log.warn("⚠️ Custom event has empty detail - skipping");
                return "SKIPPED";
            }

            log.debug("Custom event detail: {}", detail);

            // Convert detail to TaskRequestDTO
            TaskRequestDTO taskRequest = OBJECT_MAPPER.convertValue(detail, TaskRequestDTO.class);

            // Validate required fields
            if (taskRequest.getName() == null || taskRequest.getName().trim().isEmpty()) {
                String errorMsg = "Custom event detail missing required 'name' field";
                log.error("❌ Validation failed: {}", errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            // Map to Task entity
            Task task = TASK_MAPPER.toEntity(taskRequest);

            // Set metadata
            task.setId(UUID.randomUUID().toString());
            task.setCreatedAt(Instant.now().toEpochMilli());
            task.setUpdatedAt(Instant.now().toEpochMilli());

            // Persist to task store
            TaskData.saveTask(task);

            log.info("✅ Custom event task created: id={}, name={}, detailType={}",
                    task.getId(), task.getName(), event.getDetailType());

            return "OK";

        } catch (IllegalArgumentException e) {
            log.error("❌ Validation error in custom event: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Error processing custom event: {}", e.getMessage(), e);
            throw new RuntimeException("Custom event processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Process AWS system events (for future use)
     * Currently just logs the event
     */
    public String processSystemEvent(ScheduledEvent event, Context context) {
        log.info("⚙️ Processing AWS system event: source={}, detailType={}",
                event.getSource(), event.getDetailType());

        // For now, just log system events
        log.info("System event received but not processed");
        return "OK";
    }
}

