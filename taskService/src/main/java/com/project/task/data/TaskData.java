package com.project.task.data;

import com.project.task.model.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * TaskData - In-memory task storage
 * Thread-safe storage using ConcurrentHashMap
 * Centralized data management for all task operations
 */
@Slf4j
public class TaskData {

    /**
     * Thread-safe in-memory storage for tasks
     * Key: Task ID
     * Value: Task object
     */
    private static final Map<String, Task> TASK_STORE = new ConcurrentHashMap<>();

    // Initialize with sample data on class load
    static {
        initializeSampleData();
    }

    /**
     * Initialize sample tasks for testing and demo purposes
     */
    private static void initializeSampleData() {
        Task task1 = Task.builder()
                .id("task-1")
                .name("Complete documentation")
                .description("Write comprehensive API documentation")
                .status(Task.TaskStatus.IN_PROGRESS)
                .build();

        Task task2 = Task.builder()
                .id("task-2")
                .name("Review code changes")
                .description("Review pull request #123")
                .status(Task.TaskStatus.TODO)
                .build();

        Task task3 = Task.builder()
                .id("task-3")
                .name("Deploy to production")
                .description("Deploy v1.0.0 to production environment")
                .status(Task.TaskStatus.COMPLETED)
                .build();

        TASK_STORE.put(task1.getId(), task1);
        TASK_STORE.put(task2.getId(), task2);
        TASK_STORE.put(task3.getId(), task3);

        log.info("Initialized task store with {} sample tasks", TASK_STORE.size());
    }

    /**
     * Get all tasks
     */
    public static List<Task> getAllTasks() {
        return TASK_STORE.values().stream()
                .collect(Collectors.toList());
    }

    /**
     * Get task by ID
     */
    public static Task getTaskById(String id) {
        return TASK_STORE.get(id);
    }

    /**
     * Check if task exists
     */
    public static boolean taskExists(String id) {
        return TASK_STORE.containsKey(id);
    }

    /**
     * Create or update task
     */
    public static Task saveTask(Task task) {
        TASK_STORE.put(task.getId(), task);
        log.debug("Saved task: id={}, name={}", task.getId(), task.getName());
        return task;
    }

    /**
     * Delete task by ID
     */
    public static Task deleteTask(String id) {
        Task removedTask = TASK_STORE.remove(id);
        if (removedTask != null) {
            log.debug("Deleted task: id={}, name={}", id, removedTask.getName());
        }
        return removedTask;
    }

    /**
     * Get task count
     */
    public static int getTaskCount() {
        return TASK_STORE.size();
    }

    /**
     * Clear all tasks (for testing)
     */
    public static void clearAll() {
        TASK_STORE.clear();
        log.warn("All tasks cleared from store");
    }

    /**
     * Reset to initial state (for testing)
     */
    public static void reset() {
        clearAll();
        initializeSampleData();
        log.info("Task store reset to initial state");
    }
}

