package com.dbsync.dbsync.progress;

public enum TableSyncStatus {
    PENDING,
    RUNNING, // Structure creation or data syncing
    COMPLETED, // Successfully synced
    FAILED   // Sync failed for this table
}
