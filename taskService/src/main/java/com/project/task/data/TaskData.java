package com.project.task.data;

import com.project.task.model.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskData {

    private static final Logger log = LogManager.getLogger(TaskData.class);

    private static final Map<String, Task> TASK_STORE = new ConcurrentHashMap<>();

    static {
        initializeSampleData();
    }

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

    public static List<Task> getAllTasks() {
        return new ArrayList<>(TASK_STORE.values());
    }

    public static Task getTaskById(String id) {
        return TASK_STORE.get(id);
    }

    public static boolean taskExists(String id) {
        return TASK_STORE.containsKey(id);
    }

    public static void saveTask(Task task) {
        TASK_STORE.put(task.getId(), task);
        log.debug("Saved task: id={}, name={}", task.getId(), task.getName());
    }

    public static Task deleteTask(String id) {
        Task removedTask = TASK_STORE.remove(id);
        if (removedTask != null) {
            log.debug("Deleted task: id={}, name={}", id, removedTask.getName());
        }
        return removedTask;
    }

    public static int getTaskCount() {
        return TASK_STORE.size();
    }

    public static void clearAll() {
        TASK_STORE.clear();
        log.warn("All tasks cleared from store");
    }

    public static void reset() {
        clearAll();
        initializeSampleData();
        log.info("Task store reset to initial state");
    }
}

