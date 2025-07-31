package com.dbsync.dbsync.dto;

import java.util.List;

/**
 * 表分页查询响应结果
 */
public class TablePageResponse<T> {
    
    /**
     * 当前页数据
     */
    private List<T> data;
    
    /**
     * 当前页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 是否有下一页
     */
    private Boolean hasNext;
    
    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;
    
    /**
     * 搜索关键词
     */
    private String search;
    
    // 构造函数
    public TablePageResponse() {}
    
    public TablePageResponse(List<T> data, Integer page, Integer size, Long total) {
        this.data = data;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = (int) Math.ceil((double) total / size);
        this.hasNext = page < totalPages;
        this.hasPrevious = page > 1;
    }
    
    // 静态工厂方法
    public static <T> TablePageResponse<T> of(List<T> data, Integer page, Integer size, Long total) {
        return new TablePageResponse<>(data, page, size, total);
    }
    
    public static <T> TablePageResponse<T> of(List<T> data, Integer page, Integer size, Long total, String search) {
        TablePageResponse<T> response = new TablePageResponse<>(data, page, size, total);
        response.setSearch(search);
        return response;
    }
    
    // Getters and Setters
    public List<T> getData() {
        return data;
    }
    
    public void setData(List<T> data) {
        this.data = data;
    }
    
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
    
    public Long getTotal() {
        return total;
    }
    
    public void setTotal(Long total) {
        this.total = total;
    }
    
    public Integer getTotalPages() {
        return totalPages;
    }
    
    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
    
    public Boolean getHasNext() {
        return hasNext;
    }
    
    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }
    
    public Boolean getHasPrevious() {
        return hasPrevious;
    }
    
    public void setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }
    
    public String getSearch() {
        return search;
    }
    
    public void setSearch(String search) {
        this.search = search;
    }
    
    @Override
    public String toString() {
        return "TablePageResponse{" +
                "dataSize=" + (data != null ? data.size() : 0) +
                ", page=" + page +
                ", size=" + size +
                ", total=" + total +
                ", totalPages=" + totalPages +
                ", hasNext=" + hasNext +
                ", hasPrevious=" + hasPrevious +
                ", search='" + search + '\'' +
                '}';
    }
}
