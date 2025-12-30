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

@Slf4j
public class SQSTaskService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TaskMapper TASK_MAPPER = TaskMapper.INSTANCE;

    public SQSBatchResponse processSQSMessages(SQSEvent event, Context context) {
        List<SQSEvent.SQSMessage> records = event.getRecords();
        int totalMessages = records.size();

        log.info("Processing SQS batch: {} messages", totalMessages);

        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (SQSEvent.SQSMessage message : records) {
            String messageId = message.getMessageId();

            try {
                log.debug("Processing SQS message: messageId={}", messageId);

                processMessage(message, context);

                successCount++;
                log.info("Message processed successfully: messageId={}", messageId);

            } catch (Exception e) {
                failureCount++;
                log.error("Failed to process message: messageId={}, error={}",
                        messageId, e.getMessage(), e);

                SQSBatchResponse.BatchItemFailure failure =
                        new SQSBatchResponse.BatchItemFailure(messageId);
                failures.add(failure);
            }
        }

        log.info("SQS batch processing complete: total={}, success={}, failures={}",
                totalMessages, successCount, failureCount);

        return new SQSBatchResponse(failures);
    }

    private void processMessage(SQSEvent.SQSMessage message, Context context) throws Exception {
        String messageId = message.getMessageId();
        String messageBody = message.getBody();

        log.info("Processing message: messageId={}", messageId);
        log.debug("Message body: {}", messageBody);

        if (messageBody == null || messageBody.trim().isEmpty()) {
            throw new IllegalArgumentException("Message body is empty");
        }

        TaskRequestDTO taskRequest;
        try {
            taskRequest = OBJECT_MAPPER.readValue(messageBody, TaskRequestDTO.class);
            log.debug("Parsed TaskRequestDTO: name={}", taskRequest.getName());
        } catch (Exception e) {
            log.error("Failed to parse TaskRequestDTO: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JSON format in message body: " + e.getMessage(), e);
        }

        if (taskRequest.getName() == null || taskRequest.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name is required");
        }

        Task task = TASK_MAPPER.toEntity(taskRequest);
        processCreateTask(task, messageId);
    }

    private void processCreateTask(Task task, String messageId) {

        log.info("Creating new task: id={}, name={}", task.getId(), task.getName());
        TaskData.saveTask(task);

        log.info("Task created successfully: id={}, name={} (messageId={})", task.getId(), task.getName(), messageId);
    }

}
