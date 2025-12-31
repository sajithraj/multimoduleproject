package com.project.task.service;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.task.data.TaskData;
import com.project.task.mapper.TaskMapper;
import com.project.task.model.Task;
import com.project.task.model.dto.TaskRequestDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class SQSTaskService {

    private static final Logger log = LogManager.getLogger(SQSTaskService.class);

    private final ObjectMapper objectMapper;
    private final TaskMapper taskMapper;

    @Inject
    public SQSTaskService(ObjectMapper objectMapper, TaskMapper taskMapper) {
        this.objectMapper = objectMapper;
        this.taskMapper = taskMapper;
    }

    public SQSBatchResponse processSQSMessages(SQSEvent event, Context context) {
        List<SQSEvent.SQSMessage> records = event.getRecords();
        int totalMessages = records.size();

        log.info("Processing SQS batch of {} messages", totalMessages);

        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (SQSEvent.SQSMessage message : records) {
            String messageId = message.getMessageId();

            try {
                log.debug("Processing message: messageId={}", messageId);
                processMessage(message);
                successCount++;
                log.info("Message processed successfully: messageId={}", messageId);
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to process message: messageId={}, error={}", messageId, e.getMessage(), e);
                SQSBatchResponse.BatchItemFailure failure = new SQSBatchResponse.BatchItemFailure(messageId);
                failures.add(failure);
            }
        }

        log.info("SQS batch processing complete: total={}, success={}, failures={}", totalMessages, successCount, failureCount);

        return new SQSBatchResponse(failures);
    }

    private void processMessage(SQSEvent.SQSMessage message) {
        String messageId = message.getMessageId();
        String messageBody = message.getBody();

        log.debug("Processing message body for messageId={}", messageId);

        if (messageBody == null || messageBody.trim().isEmpty()) {
            throw new IllegalArgumentException("Message body is empty");
        }

        TaskRequestDTO taskRequest;
        try {
            taskRequest = objectMapper.readValue(messageBody, TaskRequestDTO.class);
            log.debug("Parsed TaskRequestDTO: name={}", taskRequest.getName());
        } catch (Exception e) {
            log.error("Failed to parse TaskRequestDTO for messageId={}: {}", messageId, e.getMessage());
            throw new IllegalArgumentException("Invalid JSON format in message body: " + e.getMessage(), e);
        }

        if (taskRequest.getName() == null || taskRequest.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Task name is required");
        }

        Task task = taskMapper.toEntity(taskRequest);
        processCreateTask(task, messageId);
    }

    private void processCreateTask(Task task, String messageId) {
        log.info("Creating new task from SQS message: id={}, name={} (messageId={})", task.getId(), task.getName(), messageId);
        TaskData.saveTask(task);
        log.info("Task created successfully: id={}, name={}", task.getId(), task.getName());
    }

}
