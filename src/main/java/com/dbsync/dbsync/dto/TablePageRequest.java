package com.dbsync.dbsync.dto;

/**
 * 表分页查询请求参数
 */
public class TablePageRequest {
    
    /**
     * 页码，从1开始
     */
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    private Integer size = 50;
    
    /**
     * 搜索关键词（表名模糊匹配）
     */
    private String search;
    
    /**
     * 排序字段（name, created_time等）
     */
    private String sortBy = "name";
    
    /**
     * 排序方向（asc, desc）
     */
    private String sortOrder = "asc";
    
    /**
     * 数据库schema名称
     */
    private String schema;
    
    // 构造函数
    public TablePageRequest() {}
    
    public TablePageRequest(Integer page, Integer size) {
        this.page = page;
        this.size = size;
    }
    
    // Getters and Setters
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page != null && page > 0 ? page : 1;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size != null && size > 0 && size <= 200 ? size : 50;
    }
    
    public String getSearch() {
        return search;
    }
    
    public void setSearch(String search) {
        this.search = search != null && !search.trim().isEmpty() ? search.trim() : null;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy != null && !sortBy.trim().isEmpty() ? sortBy.trim() : "name";
    }
    
    public String getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(String sortOrder) {
        this.sortOrder = "desc".equalsIgnoreCase(sortOrder) ? "desc" : "asc";
    }
    
    public String getSchema() {
        return schema;
    }
    
    public void setSchema(String schema) {
        this.schema = schema != null && !schema.trim().isEmpty() ? schema.trim() : null;
    }
    
    /**
     * 计算偏移量
     */
    public int getOffset() {
        return (page - 1) * size;
    }
    
    /**
     * 获取限制数量
     */
    public int getLimit() {
        return size;
    }
    
    @Override
    public String toString() {
        return "TablePageRequest{" +
                "page=" + page +
                ", size=" + size +
                ", search='" + search + '\'' +
                ", sortBy='" + sortBy + '\'' +
                ", sortOrder='" + sortOrder + '\'' +
                ", schema='" + schema + '\'' +
                '}';
    }
}
