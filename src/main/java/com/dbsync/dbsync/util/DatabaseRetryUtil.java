package com.dbsync.dbsync.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 数据库操作重试工具类
 * 专门处理锁等待超时等数据库并发问题
 */
public class DatabaseRetryUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseRetryUtil.class);
    
    // 默认重试配置
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_BASE_DELAY_MS = 1000; // 1秒
    private static final long DEFAULT_MAX_DELAY_MS = 10000; // 10秒
    private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
    
    /**
     * 重试配置类
     */
    public static class RetryConfig {
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private long baseDelayMs = DEFAULT_BASE_DELAY_MS;
        private long maxDelayMs = DEFAULT_MAX_DELAY_MS;
        private double backoffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;
        private boolean enableJitter = true;
        
        public RetryConfig maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public RetryConfig baseDelayMs(long baseDelayMs) {
            this.baseDelayMs = baseDelayMs;
            return this;
        }
        
        public RetryConfig maxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }
        
        public RetryConfig backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }
        
        public RetryConfig enableJitter(boolean enableJitter) {
            this.enableJitter = enableJitter;
            return this;
        }
        
        // Getters
        public int getMaxRetries() { return maxRetries; }
        public long getBaseDelayMs() { return baseDelayMs; }
        public long getMaxDelayMs() { return maxDelayMs; }
        public double getBackoffMultiplier() { return backoffMultiplier; }
        public boolean isEnableJitter() { return enableJitter; }
    }
    
    /**
     * 执行带重试的数据库操作
     */
    public static <T> T executeWithRetry(String taskId, String operation, 
                                        Supplier<T> supplier, RetryConfig config) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= config.getMaxRetries() + 1; attempt++) {
            try {
                if (attempt > 1) {
                    logger.info("Task [{}]: Retrying {} operation, attempt {}/{}", 
                              taskId, operation, attempt, config.getMaxRetries() + 1);
                }
                
                return supplier.get();
                
            } catch (Exception e) {
                lastException = e;
                
                // 检查是否是可重试的异常
                if (!isRetryableException(e)) {
                    logger.warn("Task [{}]: {} operation failed with non-retryable exception: {}", 
                              taskId, operation, e.getMessage());
                    throw e;
                }
                
                // 如果是最后一次尝试，直接抛出异常
                if (attempt > config.getMaxRetries()) {
                    logger.error("Task [{}]: {} operation failed after {} attempts. Last error: {}", 
                               taskId, operation, config.getMaxRetries() + 1, e.getMessage());
                    break;
                }
                
                // 计算延迟时间
                long delayMs = calculateDelay(attempt - 1, config);
                
                logger.warn("Task [{}]: {} operation failed (attempt {}/{}), retrying in {}ms. Error: {}", 
                          taskId, operation, attempt, config.getMaxRetries() + 1, delayMs, e.getMessage());
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new Exception("Retry interrupted", ie);
                }
            }
        }
        
        throw lastException;
    }
    
    /**
     * 使用默认配置执行重试
     */
    public static <T> T executeWithRetry(String taskId, String operation, Supplier<T> supplier) throws Exception {
        return executeWithRetry(taskId, operation, supplier, new RetryConfig());
    }
    
    /**
     * 检查异常是否可重试
     */
    private static boolean isRetryableException(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // 锁等待超时
        if (lowerMessage.contains("lock wait timeout") || 
            lowerMessage.contains("timeout waiting for lock")) {
            return true;
        }
        
        // 死锁
        if (lowerMessage.contains("deadlock") || 
            lowerMessage.contains("deadlock detected")) {
            return true;
        }
        
        // 连接超时
        if (lowerMessage.contains("connection timeout") || 
            lowerMessage.contains("connection timed out")) {
            return true;
        }
        
        // 临时网络问题
        if (lowerMessage.contains("connection reset") || 
            lowerMessage.contains("broken pipe") ||
            lowerMessage.contains("connection refused")) {
            return true;
        }
        
        // PostgreSQL特定错误
        if (lowerMessage.contains("could not serialize access") ||
            lowerMessage.contains("serialization failure")) {
            return true;
        }
        
        // SQL Server特定错误
        if (e instanceof SQLException) {
            SQLException sqlEx = (SQLException) e;
            int errorCode = sqlEx.getErrorCode();
            
            // SQL Server锁超时错误码
            if (errorCode == 1222) { // Lock request time out period exceeded
                return true;
            }
            
            // SQL Server死锁错误码
            if (errorCode == 1205) { // Deadlock victim
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 计算指数退避延迟时间
     */
    private static long calculateDelay(int retryCount, RetryConfig config) {
        long delay = (long) (config.getBaseDelayMs() * Math.pow(config.getBackoffMultiplier(), retryCount));
        
        // 限制最大延迟时间
        delay = Math.min(delay, config.getMaxDelayMs());
        
        // 添加随机抖动以避免惊群效应
        if (config.isEnableJitter()) {
            double jitterFactor = 0.1; // 10%的抖动
            long jitter = (long) (delay * jitterFactor * ThreadLocalRandom.current().nextDouble());
            delay += ThreadLocalRandom.current().nextBoolean() ? jitter : -jitter;
        }
        
        return Math.max(delay, 0);
    }
    
    /**
     * 创建针对锁冲突优化的重试配置
     */
    public static RetryConfig createLockConflictRetryConfig() {
        return new RetryConfig()
                .maxRetries(5)
                .baseDelayMs(500)
                .maxDelayMs(5000)
                .backoffMultiplier(1.5)
                .enableJitter(true);
    }
    
    /**
     * 创建针对网络问题优化的重试配置
     */
    public static RetryConfig createNetworkRetryConfig() {
        return new RetryConfig()
                .maxRetries(3)
                .baseDelayMs(1000)
                .maxDelayMs(10000)
                .backoffMultiplier(2.0)
                .enableJitter(true);
    }
}
