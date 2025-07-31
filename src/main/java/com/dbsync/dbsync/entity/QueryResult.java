package com.dbsync.dbsync.entity;

import java.util.List;

/**
 * 查询结果实体类
 */
public class QueryResult {
    
    private List<String> columns;
    
    private List<List<Object>> rows;
    
    private Integer totalRows;
    
    private Long executionTime;
    
    private String message;
    
    // 构造函数
    public QueryResult() {
    }
    
    public QueryResult(List<String> columns, List<List<Object>> rows, Integer totalRows, Long executionTime) {
        this.columns = columns;
        this.rows = rows;
        this.totalRows = totalRows;
        this.executionTime = executionTime;
    }
    
    public QueryResult(List<String> columns, List<List<Object>> rows, Integer totalRows, Long executionTime, String message) {
        this.columns = columns;
        this.rows = rows;
        this.totalRows = totalRows;
        this.executionTime = executionTime;
        this.message = message;
    }
    
    // Getters and Setters
    public List<String> getColumns() {
        return columns;
    }
    
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
    
    public List<List<Object>> getRows() {
        return rows;
    }
    
    public void setRows(List<List<Object>> rows) {
        this.rows = rows;
    }
    
    public Integer getTotalRows() {
        return totalRows;
    }
    
    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }
    
    public Long getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "QueryResult{" +
                "columns=" + columns +
                ", rows=" + rows +
                ", totalRows=" + totalRows +
                ", executionTime=" + executionTime +
                ", message='" + message + '\'' +
                '}';
    }
}
