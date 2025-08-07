package com.dbsync.dbsync.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 使用Caffeine作为缓存实现，提供高性能的本地缓存
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存名称常量
     */
    public static final String DB_TABLES_CACHE = "dbTables";
    public static final String TABLE_COLUMNS_CACHE = "tableColumns";
    public static final String DB_SCHEMAS_CACHE = "dbSchemas";
    public static final String TABLE_METADATA_CACHE = "tableMetadata";

    /**
     * 配置Caffeine缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // 设置缓存名称
        cacheManager.setCacheNames(java.util.Arrays.asList(
            DB_TABLES_CACHE,
            TABLE_COLUMNS_CACHE,
            DB_SCHEMAS_CACHE,
            TABLE_METADATA_CACHE
        ));
        
        // 配置Caffeine缓存属性
        cacheManager.setCaffeine(caffeineCacheBuilder());
        
        return cacheManager;
    }

    /**
     * 配置Caffeine缓存构建器
     */
    @Bean
    public Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                // 设置最大缓存条目数
                .maximumSize(1000)
                // 设置写入后过期时间（30分钟）
                .expireAfterWrite(30, TimeUnit.MINUTES)
                // 设置访问后过期时间（10分钟）
                .expireAfterAccess(10, TimeUnit.MINUTES)
                // 启用缓存统计
                .recordStats()
                // 设置初始容量
                .initialCapacity(50);
    }
}
