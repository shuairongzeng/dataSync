package com.dbsync.cache;

/**
 * Cache statistics entity
 */
public class CacheStats {
    private Long totalEntries;
    private Long totalHits;
    private Long totalSize;
    private Double averageHits;
    private Double hitRate;
    
    // Constructors
    public CacheStats() {}
    
    public CacheStats(Long totalEntries, Long totalHits, Long totalSize, Double averageHits) {
        this.totalEntries = totalEntries;
        this.totalHits = totalHits;
        this.totalSize = totalSize;
        this.averageHits = averageHits;
        this.hitRate = calculateHitRate();
    }
    
    // Calculate hit rate
    private Double calculateHitRate() {
        if (totalEntries == null || totalEntries == 0) {
            return 0.0;
        }
        return totalHits != null ? (double) totalHits / totalEntries : 0.0;
    }
    
    // Getters and Setters
    public Long getTotalEntries() {
        return totalEntries;
    }
    
    public void setTotalEntries(Long totalEntries) {
        this.totalEntries = totalEntries;
        this.hitRate = calculateHitRate();
    }
    
    public Long getTotalHits() {
        return totalHits;
    }
    
    public void setTotalHits(Long totalHits) {
        this.totalHits = totalHits;
        this.hitRate = calculateHitRate();
    }
    
    public Long getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }
    
    public Double getAverageHits() {
        return averageHits;
    }
    
    public void setAverageHits(Double averageHits) {
        this.averageHits = averageHits;
    }
    
    public Double getHitRate() {
        return hitRate;
    }
    
    public void setHitRate(Double hitRate) {
        this.hitRate = hitRate;
    }
    
    // Utility methods
    public String getFormattedSize() {
        if (totalSize == null) return "0 B";
        
        long size = totalSize;
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
    
    public String getFormattedHitRate() {
        return hitRate != null ? String.format("%.2f%%", hitRate * 100) : "0.00%";
    }
}