package com.dbsync.dbsync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 同步任务请求数据传输对象
 * 用于处理前端发送的API请求，其中tables字段是数组格式
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncTaskRequest {
    
    private String name;
    
    private String sourceConnectionId;
    
    private String targetConnectionId;
    
    private String sourceSchemaName;
    
    private String targetSchemaName;
    
    private String[] tables; // 前端发送的数组格式
    
    private Boolean truncateBeforeSync;
    
    private String status;
    
    private Integer progress;
    
    private Integer totalTables;
    
    private Integer completedTables;
    
    private String errorMessage;
    
    // 构造函数
    public SyncTaskRequest() {
        this.truncateBeforeSync = false;
        this.status = "PENDING";
        this.progress = 0;
        this.totalTables = 0;
        this.completedTables = 0;
    }
    
    // 转换为SyncTask实体类
    public SyncTask toSyncTask() {
        SyncTask task = new SyncTask();
        task.setName(this.name);
        task.setSourceConnectionId(Long.valueOf(this.sourceConnectionId));
        task.setTargetConnectionId(Long.valueOf(this.targetConnectionId));
        task.setSourceSchemaName(this.sourceSchemaName);
        task.setTargetSchemaName(this.targetSchemaName);
        
        // 将数组转换为JSON字符串
        if (this.tables != null) {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < this.tables.length; i++) {
                if (i > 0) {
                    json.append(",");
                }
                json.append("\"").append(this.tables[i]).append("\"");
            }
            json.append("]");
            task.setTables(json.toString());
        } else {
            task.setTables("[]");
        }
        
        task.setTruncateBeforeSync(this.truncateBeforeSync);
        task.setStatus(this.status);
        task.setProgress(this.progress);
        task.setTotalTables(this.tables != null ? this.tables.length : 0);
        task.setCompletedTables(this.completedTables);
        task.setErrorMessage(this.errorMessage);
        
        return task;
    }
    
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
    
    public String[] getTables() {
        return tables;
    }
    
    public void setTables(String[] tables) {
        this.tables = tables;
    }
    
    public Boolean getTruncateBeforeSync() {
        return truncateBeforeSync;
    }
    
    public void setTruncateBeforeSync(Boolean truncateBeforeSync) {
        this.truncateBeforeSync = truncateBeforeSync;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Integer getProgress() {
        return progress;
    }
    
    public void setProgress(Integer progress) {
        this.progress = progress;
    }
    
    public Integer getTotalTables() {
        return totalTables;
    }
    
    public void setTotalTables(Integer totalTables) {
        this.totalTables = totalTables;
    }
    
    public Integer getCompletedTables() {
        return completedTables;
    }
    
    public void setCompletedTables(Integer completedTables) {
        this.completedTables = completedTables;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}