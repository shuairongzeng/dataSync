package com.dbsync.dbsync.progress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProgressManager {
    private static final Logger logger = LoggerFactory.getLogger(ProgressManager.class);
    private final Map<String, TaskProgress> tasks = new ConcurrentHashMap<>();

    public void startTask(String taskId, int totalTables) {
        TaskProgress taskProgress = tasks.computeIfAbsent(taskId, id -> new TaskProgress(id, totalTables));
        taskProgress.setStartTime(LocalDateTime.now());
        taskProgress.setStatus(TaskStatus.RUNNING);
        logger.info("Task [{}] started. Total tables to sync: {}.", taskId, totalTables);
    }

    public void startTableSync(String taskId, String tableName, long sourceRecordCount) {
        TaskProgress taskProgress = tasks.get(taskId);
        if (taskProgress == null) {
            logger.warn("Cannot start table sync. Task [{}] not found.", taskId);
            return;
        }
        TableSyncProgress tableProgress = taskProgress.getTableProgress(tableName); // Ensures creation if not exists
        tableProgress.setSourceRecordCount(sourceRecordCount);
        tableProgress.setStartTime(LocalDateTime.now());
        tableProgress.setStatus(TableSyncStatus.RUNNING);
        
        // Add this table's record count to the task's total records to sync
        taskProgress.addTotalRecordsToSync(sourceRecordCount);

        logger.info("Task [{}], Table [{}]: Sync started. Total records: {}.", taskId, tableName, sourceRecordCount);
    }

    public void updateTableProgress(String taskId, String tableName, long newlyProcessedRecords) {
        TaskProgress taskProgress = tasks.get(taskId);
        if (taskProgress == null) {
            logger.warn("Cannot update table progress. Task [{}] not found.", taskId);
            return;
        }
        TableSyncProgress tableProgress = taskProgress.getTableProgress(tableName);
        if (tableProgress.getStatus() != TableSyncStatus.RUNNING) {
            // Avoid updates if not running, e.g. already failed or completed
            logger.warn("Task [{}], Table [{}]: Attempted to update progress but status is {}.", taskId, tableName, tableProgress.getStatus());
            return;
        }
        tableProgress.addRecordsProcessed(newlyProcessedRecords);
        taskProgress.addTotalRecordsSynced(newlyProcessedRecords); // Update overall task synced records

        logger.info("Task [{}], Table [{}]: Progress: {}/{} records.",
                taskId, tableName, tableProgress.getRecordsProcessed(), tableProgress.getSourceRecordCount());
    }

    public void completeTableSync(String taskId, String tableName, boolean success, String errorMessage) {
        TaskProgress taskProgress = tasks.get(taskId);
        if (taskProgress == null) {
            logger.warn("Cannot complete table sync. Task [{}] not found.", taskId);
            return;
        }
        TableSyncProgress tableProgress = taskProgress.getTableProgress(tableName);
        tableProgress.setEndTime(LocalDateTime.now());
        if (success) {
            tableProgress.setStatus(TableSyncStatus.COMPLETED);
            taskProgress.incrementTablesCompleted();
            logger.info("Task [{}], Table [{}]: Sync completed successfully. Processed {} records.",
                    taskId, tableName, tableProgress.getRecordsProcessed());
        } else {
            tableProgress.setStatus(TableSyncStatus.FAILED);
            tableProgress.addErrorMessage(errorMessage != null ? errorMessage : "Unknown error");
            taskProgress.incrementTablesFailed();
            logger.error("Task [{}], Table [{}]: Sync FAILED. Error: {}", taskId, tableName, errorMessage);
        }
    }

    public void completeTask(String taskId) {
        TaskProgress taskProgress = tasks.get(taskId);
        if (taskProgress == null) {
            logger.warn("Cannot complete task. Task [{}] not found.", taskId);
            return;
        }
        taskProgress.setEndTime(LocalDateTime.now());
        if (taskProgress.getTablesFailed() == 0 && taskProgress.getTablesCompleted() == taskProgress.getTotalTables()) {
            taskProgress.setStatus(TaskStatus.COMPLETED_SUCCESS);
        } else if (taskProgress.getTablesFailed() > 0) {
            taskProgress.setStatus(TaskStatus.COMPLETED_WITH_ERRORS);
        } else {
            // Not all tables completed, but no failures reported (e.g. task interrupted)
            // Or if some tables were skipped and not marked failed/completed.
            // This logic might need refinement based on how skipped tables are handled.
            taskProgress.setStatus(TaskStatus.FAILED); // Default to FAILED if not clearly successful or with specific errors.
             logger.warn("Task [{}] completed with an indeterminate state ({} completed, {} failed out of {} total). Marking as FAILED or COMPLETED_WITH_ERRORS if any failed.",
                taskId, taskProgress.getTablesCompleted(), taskProgress.getTablesFailed(), taskProgress.getTotalTables());
            if (taskProgress.getTablesFailed() > 0) {
                 taskProgress.setStatus(TaskStatus.COMPLETED_WITH_ERRORS);
            } else if (taskProgress.getTablesCompleted() < taskProgress.getTotalTables()) {
                taskProgress.setStatus(TaskStatus.FAILED); // If some tables were not processed at all
            }

        }
        logger.info("Task [{}] completed. Status: {}. Tables successful: {}, Tables failed: {}. Total records synced: {} out of {}.",
                taskId, taskProgress.getStatus(), taskProgress.getTablesCompleted(), taskProgress.getTablesFailed(),
                taskProgress.getTotalRecordsSynced(), taskProgress.getTotalRecordsToSync());
    }

    public TaskProgress getTaskProgress(String taskId) {
        return tasks.get(taskId);
    }

    public Collection<TaskProgress> getAllTaskProgress() {
        return tasks.values();
    }
}
