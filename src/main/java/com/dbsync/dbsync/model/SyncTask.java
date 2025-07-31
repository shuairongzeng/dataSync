package com.dbsync.dbsync.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * 同步任务实体类
 */
@TableName("sync_tasks")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncTask {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private Long sourceConnectionId;
    
    private Long targetConnectionId;
    
    private String sourceSchemaName;
    
    private String targetSchemaName;
    
    private String tables; // JSON格式存储表名数组
    
    private Boolean truncateBeforeSync;
    
    private String status;
    
    private Integer progress;
    
    private Integer totalTables;
    
    private Integer completedTables;
    
    private String errorMessage;
    
    private String createdAt;
    
    private String updatedAt;
    
    private String lastRunAt;
    
    // 构造函数
    public SyncTask() {
        this.truncateBeforeSync = false;
        this.status = "PENDING";
        this.progress = 0;
        this.totalTables = 0;
        this.completedTables = 0;
        this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        this.updatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public SyncTask(String name, Long sourceConnectionId, Long targetConnectionId, 
                    List<String> tables, Boolean truncateBeforeSync) {
        this();
        this.name = name;
        this.sourceConnectionId = sourceConnectionId;
        this.targetConnectionId = targetConnectionId;
        this.truncateBeforeSync = truncateBeforeSync;
        this.setTables(tables);
        this.totalTables = tables != null ? tables.size() : 0;
    }
    
    // 辅助方法：获取表名列表
    public List<String> getTablesList() {
        if (tables == null || tables.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            // 使用简单的JSON解析
            String json = tables.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1);
                String[] tableArray = json.split(",");
                List<String> result = new java.util.ArrayList<>();
                for (String table : tableArray) {
                    table = table.trim().replaceAll("\"", "");
                    if (!table.isEmpty()) {
                        result.add(table);
                    }
                }
                return result;
            }
        } catch (Exception e) {
            // 解析失败返回空列表
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
    
    // 辅助方法：设置表名列表
    public void setTables(List<String> tables) {
        if (tables == null || tables.isEmpty()) {
            this.tables = "[]";
        } else {
            // 简单的JSON格式化
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < tables.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(tables.get(i)).append("\"");
            }
            json.append("]");
            this.tables = json.toString();
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Long getSourceConnectionId() {
        return sourceConnectionId;
    }
    
    public void setSourceConnectionId(Long sourceConnectionId) {
        this.sourceConnectionId = sourceConnectionId;
    }
    
    public Long getTargetConnectionId() {
        return targetConnectionId;
    }
    
    public void setTargetConnectionId(Long targetConnectionId) {
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
    
    public String getTables() {
        return tables;
    }
    
    public void setTables(String tables) {
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
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getLastRunAt() {
        return lastRunAt;
    }
    
    public void setLastRunAt(String lastRunAt) {
        this.lastRunAt = lastRunAt;
    }
    
    @Override
    public String toString() {
        return "SyncTask{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sourceConnectionId=" + sourceConnectionId +
                ", targetConnectionId=" + targetConnectionId +
                ", sourceSchemaName='" + sourceSchemaName + '\'' +
                ", targetSchemaName='" + targetSchemaName + '\'' +
                ", tables=" + tables +
                ", truncateBeforeSync=" + truncateBeforeSync +
                ", status='" + status + '\'' +
                ", progress=" + progress +
                ", totalTables=" + totalTables +
                ", completedTables=" + completedTables +
                ", errorMessage='" + errorMessage + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", lastRunAt='" + lastRunAt + '\'' +
                '}';
    }
}