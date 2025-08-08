package com.dbsync.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;

/**
 * Cache configuration
 */
@Configuration("sqliteCacheConfig")
@EnableScheduling
@ConfigurationProperties(prefix = "dbsync.cache")
public class CacheConfig {
    
    private boolean enabled = true;
    private String sqliteDbPath = "cache/dbsync-cache.db";
    private int defaultTtlMinutes = 60;
    private int maxCacheSize = 1000;
    private int cleanupIntervalMinutes = 30;
    private boolean enableCompression = false;
    
    private final ApplicationContext applicationContext;
    
    public CacheConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    /**
     * Cache SQLite DataSource
     */
    @Bean(name = "cacheDataSource")
    public DataSource cacheDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.sqlite.JDBC")
                .url("jdbc:sqlite:" + sqliteDbPath)
                .build();
    }
    
    /**
     * Scheduled cache cleanup task
     */
    @Scheduled(fixedRateString = "#{${dbsync.cache.cleanup-interval-minutes:30} * 60 * 1000}")
    public void cleanupExpiredCache() {
        if (enabled) {
            try {
                CacheService cacheService = applicationContext.getBean(CacheService.class);
                int deletedCount = cacheService.cleanupExpired();
                if (deletedCount > 0) {
                    System.out.println("Cache cleanup: removed " + deletedCount + " expired entries");
                }
            } catch (Exception e) {
                System.err.println("Error during cache cleanup: " + e.getMessage());
            }
        }
    }
    
    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public String getSqliteDbPath() {
        return sqliteDbPath;
    }
    
    public void setSqliteDbPath(String sqliteDbPath) {
        this.sqliteDbPath = sqliteDbPath;
    }
    
    public int getDefaultTtlMinutes() {
        return defaultTtlMinutes;
    }
    
    public void setDefaultTtlMinutes(int defaultTtlMinutes) {
        this.defaultTtlMinutes = defaultTtlMinutes;
    }
    
    public int getMaxCacheSize() {
        return maxCacheSize;
    }
    
    public void setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }
    
    public int getCleanupIntervalMinutes() {
        return cleanupIntervalMinutes;
    }
    
    public void setCleanupIntervalMinutes(int cleanupIntervalMinutes) {
        this.cleanupIntervalMinutes = cleanupIntervalMinutes;
    }
    
    public boolean isEnableCompression() {
        return enableCompression;
    }
    
    public void setEnableCompression(boolean enableCompression) {
        this.enableCompression = enableCompression;
    }
}