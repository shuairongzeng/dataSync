package com.dbsync.dbsync.controller.dto;

import java.util.List;

public class SyncRequest {
    private List<String> tablesToSync;
    private String sourceSchemaName;
    private String targetSchemaName;
    private boolean truncateBeforeSync = true; // Default value

    public List<String> getTablesToSync() {
        return tablesToSync;
    }

    public void setTablesToSync(List<String> tablesToSync) {
        this.tablesToSync = tablesToSync;
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

    public boolean isTruncateBeforeSync() {
        return truncateBeforeSync;
    }

    public void setTruncateBeforeSync(boolean truncateBeforeSync) {
        this.truncateBeforeSync = truncateBeforeSync;
    }
}
