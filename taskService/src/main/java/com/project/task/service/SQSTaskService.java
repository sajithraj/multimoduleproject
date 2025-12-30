package com.project.task.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.task.data.TaskData;
import com.project.task.mapper.TaskMapper;
import com.project.task.model.Task;
import com.project.task.model.dto.TaskRequestDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQSTaskService - Handles all SQS message processing
 * Features:
 * - Process TaskRequestDTO from SQS message body
 * - Update/Create tasks in TASK_STORE
 * - Batch failure handling - failed messages returned for retry/DLQ
 * - Partial batch processing support
 */
@Slf4j
public class SQSTaskService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TaskMapper TASK_MAPPER = TaskMapper.INSTANCE;

    /**
     * Process SQS batch messages
     *
     * @param event   SQS event containing batch of messages
     * @param context Lambda context
     * @return SQSBatchResponse with failed message IDs
     */
    public SQSBatchResponse processSQSMessages(SQSEvent event, Context context) {
        List<SQSEvent.SQSMessage> records = event.getRecords();
        int totalMessages = records.size();

        log.info("Processing SQS batch: {} messages", totalMessages);

        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        // Process each message in the batch
        for (SQSEvent.SQSMessage message : records) {
            String messageId = message.getMessageId();

            try {
                log.debug("Processing SQS message: messageId={}", messageId);

                // Process single message
                processMessage(message, context);

                successCount++;
                log.info("✅ Message processed successfully: messageId={}", messageId);

            } catch (Exception e) {
                failureCount++;
                log.error("❌ Failed to process message: messageId={}, error={}",
                        messageId, e.getMessage(), e);

                // Add to failure list - will be sent to DLQ or retried
                SQSBatchResponse.BatchItemFailure failure =
                        new SQSBatchResponse.BatchItemFailure(messageId);
                failures.add(failure);
            }
        }

        log.info("SQS batch processing complete: total={}, success={}, failures={}",
                totalMessages, successCount, failureCount);

        // Return batch response with failures
        return new SQSBatchResponse(failures);
    }

    /**
     * Process single SQS message
     * Extracts TaskRequestDTO from message body and creates/updates task
     */
    private void processMessage(SQSEvent.SQSMessage message, Context context) throws Exception {
        String messageId = message.getMessageId();
        String messageBody = message.getBody();

        log.info("📨 Processing message: messageId={}", messageId);
        log.debug("Message body: {}", messageBody);

        // Validate message body
        if (messageBody == null || messageBody.trim().isEmpty()) {
            throw new IllegalArgumentException("Message body is empty");
        }

        // Parse TaskRequestDTO from message body
        TaskRequestDTO taskRequest;
        try {
            taskRequest = OBJECT_MAPPER.readValue(messageBody, TaskRequestDTO.class);
            log.debug("Parsed TaskRequestDTO: name={}", taskRequest.getName());
        } catch (Exception e) {
            log.error("Failed to parse TaskRequestDTO: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JSON format in message body: " + e.getMessage(), e);
        }

        // Validate required fields
        if (taskRequest.getName() == null || taskRequest.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name is required");
        }

        // Map DTO to Entity
        Task task = TASK_MAPPER.toEntity(taskRequest);

        // Check if this is an update (ID provided) or create (no ID)
        if (task.getId() != null && !task.getId().isEmpty()) {
            // Update existing task
            processUpdateTask(task, messageId);
        } else {
            // Create new task
            processCreateTask(task, messageId);
        }
    }

    /**
     * Create new task from SQS message
     */
    private void processCreateTask(Task task, String messageId) {
        // Generate new ID
        String newId = UUID.randomUUID().toString();
        task.setId(newId);

        log.info("➕ Creating new task: id={}, name={}", newId, task.getName());

        // Save to store
        TaskData.saveTask(task);

        log.info("✅ Task created successfully: id={}, name={} (messageId={})",
                newId, task.getName(), messageId);
    }

    /**
     * Update existing task from SQS message
     */
    private void processUpdateTask(Task task, String messageId) {
        String taskId = task.getId();

        log.info("✏️  Updating task: id={}", taskId);

        // Check if task exists
        Task existingTask = TaskData.getTaskById(taskId);

        if (existingTask == null) {
            // Task doesn't exist - create it instead
            log.warn("Task not found for update, creating new task: id={}", taskId);
            TaskData.saveTask(task);
        } else {
            // Merge updates into existing task
            mergeTaskUpdates(existingTask, task);

            // Update timestamp
            existingTask.setUpdatedAt(System.currentTimeMillis());

            // Save updated task
            TaskData.saveTask(existingTask);

            log.info("✅ Task updated successfully: id={}, name={} (messageId={})",
                    taskId, existingTask.getName(), messageId);
        }
    }

    /**
     * Merge task updates from incoming task into existing task
     * Only updates non-null fields
     */
    private void mergeTaskUpdates(Task existing, Task incoming) {
        if (incoming.getName() != null && !incoming.getName().isEmpty()) {
            existing.setName(incoming.getName());
        }

        if (incoming.getDescription() != null && !incoming.getDescription().isEmpty()) {
            existing.setDescription(incoming.getDescription());
        }

        if (incoming.getStatus() != null) {
            existing.setStatus(incoming.getStatus());
        }

        log.debug("Merged updates into task: id={}", existing.getId());
    }

    /**
     * Get message attributes (for debugging/logging)
     */
    private void logMessageAttributes(SQSEvent.SQSMessage message) {
        log.debug("Message Attributes:");
        log.debug("  Message ID: {}", message.getMessageId());
        log.debug("  Receipt Handle: {}", message.getReceiptHandle());
        log.debug("  Event Source ARN: {}", message.getEventSourceArn());

        if (message.getAttributes() != null && !message.getAttributes().isEmpty()) {
            message.getAttributes().forEach((key, value) ->
                    log.debug("  Attribute: {}={}", key, value)
            );
        }

        if (message.getMessageAttributes() != null && !message.getMessageAttributes().isEmpty()) {
            message.getMessageAttributes().forEach((key, value) ->
                    log.debug("  Message Attribute: {}={}", key, value.getStringValue())
            );
        }
    }
}

