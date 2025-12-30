package com.project.task.integration;

import com.amazonaws.services.lambda.runtime.Context;
import com.project.task.data.TaskData;
import com.project.task.handler.UnifiedTaskHandler;
import com.project.task.model.Task;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class EventBridgeIntegrationTest extends BaseIntegrationTest {

    private UnifiedTaskHandler handler;

    @Mock
    private Context mockContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UnifiedTaskHandler();

        when(mockContext.getAwsRequestId()).thenReturn("eventbridge-test-id");
        when(mockContext.getFunctionName()).thenReturn("task-service-eventbridge-integration");
        when(mockContext.getRemainingTimeInMillis()).thenReturn(30000);

        // Clear TaskData to ensure clean state for each test
        TaskData.getAllTasks().clear();
    }

    @Test
    public void testEventBridge_ScheduledTask() {
        System.out.println("\n=== Test: EventBridge Scheduled Task ===");

        // Get initial task count (should be 0 after setUp clears it)
        int initialCount = TaskData.getAllTasks().size();

        // Create event as Map (mimicking AWS Lambda input)
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("id", "scheduled-123");
        eventMap.put("source", "aws.events");
        eventMap.put("detail-type", "Scheduled Event");  // Note: hyphenated
        eventMap.put("time", DateTime.now().toString());
        eventMap.put("region", "us-east-1");
        eventMap.put("account", "123456789012");
        eventMap.put("resources", Arrays.asList());

        Map<String, Object> detail = new HashMap<>();
        detail.put("taskType", "daily-cleanup");
        eventMap.put("detail", detail);

        Object response = handler.handleRequest(eventMap, mockContext);

        // Verify response
        assertNotNull("Response should not be null", response);
        assertEquals("Response should be OK", "OK", response);

        // Verify task created
        assertEquals("One new task should be created", initialCount + 1, TaskData.getAllTasks().size());

        // Find the created task
        Task createdTask = TaskData.getAllTasks().stream()
                .filter(t -> t.getName().startsWith("scheduled event "))
                .filter(t -> t.getName().contains("scheduled-123"))
                .findFirst()
                .orElse(null);

        assertNotNull("Scheduled task should be created", createdTask);
        assertTrue("Task name should start with 'scheduled event '",
                createdTask.getName().startsWith("scheduled event "));
        assertTrue("Task name should contain event ID",
                createdTask.getName().contains("scheduled-123"));
        assertEquals("Task status should be TODO", Task.TaskStatus.TODO, createdTask.getStatus());

        System.out.println("✓ Scheduled task created: " + createdTask.getName());
    }

    @Test
    public void testEventBridge_CustomEvent() {
        System.out.println("\n=== Test: EventBridge Custom Event ===");

        // Get initial task count
        int initialCount = TaskData.getAllTasks().size();

        // Record task IDs before test
        java.util.Set<String> existingTaskIds = TaskData.getAllTasks().stream()
                .map(Task::getId)
                .collect(java.util.stream.Collectors.toSet());

        // Create event as Map (mimicking AWS Lambda input)
        Map<String, Object> eventMap = new HashMap<>();
        eventMap.put("id", "custom-event-123");
        eventMap.put("source", "com.custom.orders");  // Note: com.custom prefix
        eventMap.put("detail-type", "custom-event-OrderCompleted");  // Note: custom-event prefix
        eventMap.put("time", DateTime.now().toString());
        eventMap.put("region", "us-east-1");
        eventMap.put("account", "123456789012");
        eventMap.put("resources", Arrays.asList());

        // Detail contains TaskRequestDTO structure
        Map<String, Object> detail = new HashMap<>();
        detail.put("name", "Process Order");
        detail.put("description", "Process completed order");
        detail.put("status", "TODO");
        eventMap.put("detail", detail);

        Object response = handler.handleRequest(eventMap, mockContext);

        // Verify response
        assertNotNull("Response should not be null", response);
        assertEquals("Response should be OK", "OK", response);

        // Verify task created from detail
        assertEquals("One new task should be created", initialCount + 1, TaskData.getAllTasks().size());

        // Find the NEW task (not pre-existing ones)
        Task createdTask = TaskData.getAllTasks().stream()
                .filter(t -> !existingTaskIds.contains(t.getId()))  // Only new tasks
                .filter(t -> "Process Order".equals(t.getName()))
                .findFirst()
                .orElse(null);

        assertNotNull("Custom event task should be created", createdTask);
        assertEquals("Task name from detail", "Process Order", createdTask.getName());
        assertEquals("Task description from detail", "Process completed order", createdTask.getDescription());
        assertEquals("Task status from detail", Task.TaskStatus.TODO, createdTask.getStatus());

        System.out.println("✓ Custom event task created: " + createdTask.getName());
    }

}
