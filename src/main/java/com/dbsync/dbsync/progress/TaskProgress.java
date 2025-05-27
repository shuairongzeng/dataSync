package com.dbsync.dbsync.progress;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TaskProgress {
    private String taskId;
    private TaskStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int totalTables;
    private AtomicInteger tablesCompleted = new AtomicInteger(0);
    private AtomicInteger tablesFailed = new AtomicInteger(0);
    private AtomicLong totalRecordsToSync = new AtomicLong(0); // Sum of sourceRecordCount for all tables
    private AtomicLong totalRecordsSynced = new AtomicLong(0); // Sum of recordsProcessed for all tables
    private Map<String, TableSyncProgress> tableProgressMap; // Key: tableName

    public TaskProgress(String taskId, int totalTables) {
        this.taskId = taskId;
        this.totalTables = totalTables;
        this.status = TaskStatus.PENDING;
        this.tableProgressMap = new ConcurrentHashMap<>();
    }

    // Getters
    public String getTaskId() {
        return taskId;
    }

    public synchronized TaskStatus getStatus() {
        return status;
    }

    public synchronized LocalDateTime getStartTime() {
        return startTime;
    }

    public synchronized LocalDateTime getEndTime() {
        return endTime;
    }

    public int getTotalTables() {
        return totalTables;
    }

    public int getTablesCompleted() {
        return tablesCompleted.get();
    }

    public int getTablesFailed() {
        return tablesFailed.get();
    }
    
    public long getTotalRecordsToSync() {
        return totalRecordsToSync.get();
    }

    public long getTotalRecordsSynced() {
        return totalRecordsSynced.get();
    }

    public Map<String, TableSyncProgress> getTableProgressMap() {
        return tableProgressMap;
    }

    // Synchronized methods to update progress
    public synchronized void setStatus(TaskStatus status) {
        this.status = status;
    }

    public synchronized void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public synchronized void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void incrementTablesCompleted() {
        this.tablesCompleted.incrementAndGet();
    }

    public void incrementTablesFailed() {
        this.tablesFailed.incrementAndGet();
    }
    
    public void addTotalRecordsToSync(long count) {
        this.totalRecordsToSync.addAndGet(count);
    }

    public void addTotalRecordsSynced(long count) {
        this.totalRecordsSynced.addAndGet(count);
    }

    public TableSyncProgress getTableProgress(String tableName) {
        return tableProgressMap.computeIfAbsent(tableName, TableSyncProgress::new);
    }
    
    public void addTableProgress(String tableName, TableSyncProgress progress) {
        this.tableProgressMap.put(tableName, progress);
    }
}
