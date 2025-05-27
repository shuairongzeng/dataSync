package com.dbsync.dbsync.config;

import java.util.List;

public class SyncTaskProperties {
    private String name;
    private String sourceConnectionId;
    private String targetConnectionId;
    private String sourceSchemaName; // Optional
    private String targetSchemaName; // Optional
    private List<String> tables;
    private boolean truncateBeforeSync;

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceConnectionId() {
        return sourceConnectionId;
    }

    public void setSourceConnectionId(String sourceConnectionId) {
        this.sourceConnectionId = sourceConnectionId;
    }

    public String getTargetConnectionId() {
        return targetConnectionId;
    }

    public void setTargetConnectionId(String targetConnectionId) {
        this.targetConnectionId = targetConnectionId;
    }

    public String getSourceSchemaName() {
        return sourceSchemaName;
    }

    public void setSourceSchemaName(String sourceSchemaName) {
        this.sourceSchemaName = sourceSchemaName;
    }

    public String getTargetSchemaName() {
        return targetSchemaName;
    }

    public void setTargetSchemaName(String targetSchemaName) {
        this.targetSchemaName = targetSchemaName;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables;
    }

    public boolean isTruncateBeforeSync() {
        return truncateBeforeSync;
    }

    public void setTruncateBeforeSync(boolean truncateBeforeSync) {
        this.truncateBeforeSync = truncateBeforeSync;
    }
}
