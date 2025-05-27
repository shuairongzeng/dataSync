package com.dbsync.dbsync.progress;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TableSyncProgress {
    private String tableName;
    private TableSyncStatus status;
    private long sourceRecordCount;
    private long recordsProcessed;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> errorMessages;

    public TableSyncProgress(String tableName) {
        this.tableName = tableName;
        this.status = TableSyncStatus.PENDING;
        this.errorMessages = new ArrayList<>();
    }

    // Getters and synchronized setters/updaters
    public String getTableName() {
        return tableName;
    }

    public synchronized TableSyncStatus getStatus() {
        return status;
    }

    public synchronized void setStatus(TableSyncStatus status) {
        this.status = status;
    }

    public synchronized long getSourceRecordCount() {
        return sourceRecordCount;
    }

    public synchronized void setSourceRecordCount(long sourceRecordCount) {
        this.sourceRecordCount = sourceRecordCount;
    }

    public synchronized long getRecordsProcessed() {
        return recordsProcessed;
    }

    public synchronized void setRecordsProcessed(long recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }
    
    public synchronized void addRecordsProcessed(long count) {
        this.recordsProcessed += count;
    }

    public synchronized LocalDateTime getStartTime() {
        return startTime;
    }

    public synchronized void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public synchronized LocalDateTime getEndTime() {
        return endTime;
    }

    public synchronized void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public synchronized List<String> getErrorMessages() {
        return errorMessages;
    }

    public synchronized void addErrorMessage(String errorMessage) {
        this.errorMessages.add(errorMessage);
    }
}
