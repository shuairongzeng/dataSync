package com.dbsync.dbsync.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 查询历史实体类
 */
@TableName("query_history")
public class QueryHistory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String sql;
    
    private Long sourceConnectionId;
    
    private String sourceConnectionName;
    
    private Long targetConnectionId;
    
    private String targetConnectionName;
    
    private String targetTableName;
    
    private String targetSchemaName;
    
    private String executedAt;
    
    private Integer executionTime;
    
    private String status;
    
    private String errorMessage;
    
    private Integer resultRows;
    
    private String createdBy;
    
    // 构造函数
    public QueryHistory() {
    }
    
    public QueryHistory(String sql, Long sourceConnectionId, String sourceConnectionName, 
                       String executedAt, Integer executionTime, String status) {
        this.sql = sql;
        this.sourceConnectionId = sourceConnectionId;
        this.sourceConnectionName = sourceConnectionName;
        this.executedAt = executedAt;
        this.executionTime = executionTime;
        this.status = status;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSql() {
        return sql;
    }
    
    public void setSql(String sql) {
        this.sql = sql;
    }
    
    public Long getSourceConnectionId() {
        return sourceConnectionId;
    }
    
    public void setSourceConnectionId(Long sourceConnectionId) {
        this.sourceConnectionId = sourceConnectionId;
    }
    
    public String getSourceConnectionName() {
        return sourceConnectionName;
    }
    
    public void setSourceConnectionName(String sourceConnectionName) {
        this.sourceConnectionName = sourceConnectionName;
    }
    
    public Long getTargetConnectionId() {
        return targetConnectionId;
    }
    
    public void setTargetConnectionId(Long targetConnectionId) {
        this.targetConnectionId = targetConnectionId;
    }
    
    public String getTargetConnectionName() {
        return targetConnectionName;
    }
    
    public void setTargetConnectionName(String targetConnectionName) {
        this.targetConnectionName = targetConnectionName;
    }
    
    public String getTargetTableName() {
        return targetTableName;
    }
    
    public void setTargetTableName(String targetTableName) {
        this.targetTableName = targetTableName;
    }
    
    public String getTargetSchemaName() {
        return targetSchemaName;
    }
    
    public void setTargetSchemaName(String targetSchemaName) {
        this.targetSchemaName = targetSchemaName;
    }
    
    public String getExecutedAt() {
        return executedAt;
    }
    
    public void setExecutedAt(String executedAt) {
        this.executedAt = executedAt;
    }
    
    public Integer getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(Integer executionTime) {
        this.executionTime = executionTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Integer getResultRows() {
        return resultRows;
    }
    
    public void setResultRows(Integer resultRows) {
        this.resultRows = resultRows;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    @Override
    public String toString() {
        return "QueryHistory{" +
                "id=" + id +
                ", sql='" + sql + '\'' +
                ", sourceConnectionId=" + sourceConnectionId +
                ", sourceConnectionName='" + sourceConnectionName + '\'' +
                ", targetConnectionId=" + targetConnectionId +
                ", targetConnectionName='" + targetConnectionName + '\'' +
                ", targetTableName='" + targetTableName + '\'' +
                ", targetSchemaName='" + targetSchemaName + '\'' +
                ", executedAt='" + executedAt + '\'' +
                ", executionTime=" + executionTime +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", resultRows=" + resultRows +
                ", createdBy='" + createdBy + '\'' +
                '}';
    }
}
