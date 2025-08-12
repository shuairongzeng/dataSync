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
    
    // 分页相关字段
    private Integer currentPage;
    private Integer pageSize;
    private Integer totalPages;
    private Boolean hasMore;
    private Boolean fromCache;
    
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
    
    // 分页构造函数
    public QueryResult(List<String> columns, List<List<Object>> rows, Integer totalRows, Long executionTime, 
                      Integer currentPage, Integer pageSize, Integer totalPages, Boolean hasMore) {
        this.columns = columns;
        this.rows = rows;
        this.totalRows = totalRows;
        this.executionTime = executionTime;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
        this.hasMore = hasMore;
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
    
    public Integer getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }
    
    public Integer getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
    
    public Integer getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
    
    public Boolean getHasMore() {
        return hasMore;
    }
    
    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }
    
    public Boolean getFromCache() {
        return fromCache;
    }
    
    public void setFromCache(Boolean fromCache) {
        this.fromCache = fromCache;
    }
    
    @Override
    public String toString() {
        return "QueryResult{" +
                "columns=" + columns +
                ", rows=" + rows +
                ", totalRows=" + totalRows +
                ", executionTime=" + executionTime +
                ", message='" + message + '\'' +
                ", currentPage=" + currentPage +
                ", pageSize=" + pageSize +
                ", totalPages=" + totalPages +
                ", hasMore=" + hasMore +
                ", fromCache=" + fromCache +
                '}';
    }
}