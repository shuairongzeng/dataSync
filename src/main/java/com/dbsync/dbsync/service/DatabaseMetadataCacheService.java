package com.dbsync.dbsync.service;

import com.dbsync.dbsync.config.CacheConfig;
import com.dbsync.dbsync.entity.ColumnInfo;
import com.dbsync.dbsync.entity.TableInfo;
import com.dbsync.dbsync.entity.BasicTableInfo;
import com.dbsync.dbsync.dto.TablePageRequest;
import com.dbsync.dbsync.dto.TablePageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据库元数据缓存服务
 * 提供数据库表、列等元数据的缓存管理功能
 */
@Service
public class DatabaseMetadataCacheService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMetadataCacheService.class);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private DbConnectionService dbConnectionService;

    /**
     * 获取数据库表列表（带缓存）
     */
    @Cacheable(value = CacheConfig.DB_TABLES_CACHE, key = "#connectionId + '_' + (#schema != null ? #schema : 'default')")
    public List<String> getTables(Long connectionId, String schema) {
        logger.info("从数据库获取表列表，连接ID: {}, Schema: {}", connectionId, schema);
        return dbConnectionService.getTables(connectionId, schema);
    }

    /**
     * 获取数据库表列表（分页，带缓存）
     */
    @Cacheable(value = CacheConfig.TABLE_METADATA_CACHE, 
               key = "#connectionId + '_' + (#request.schema != null ? #request.schema : 'default') + '_page_' + #request.page + '_' + #request.size + '_' + (#request.search != null ? #request.search : '') + '_' + #request.sortBy + '_' + #request.sortOrder")
    public TablePageResponse<TableInfo> getTablesWithPagination(Long connectionId, TablePageRequest request) {
        logger.info("从数据库获取分页表列表，连接ID: {}, 页码: {}, 大小: {}", connectionId, request.getPage(), request.getSize());
        return dbConnectionService.getTablesWithPagination(connectionId, request);
    }

    /**
     * 获取表的列信息（带缓存）
     */
    @Cacheable(value = CacheConfig.TABLE_COLUMNS_CACHE, 
               key = "#connectionId + '_' + #tableName + '_' + (#schemaName != null ? #schemaName : 'default')")
    public List<ColumnInfo> getTableColumns(Long connectionId, String tableName, String schemaName) {
        logger.info("从数据库获取表列信息，连接ID: {}, 表名: {}, Schema: {}", connectionId, tableName, schemaName);
        return dbConnectionService.getTableColumns(connectionId, tableName, schemaName);
    }

    /**
     * 获取数据库Schema列表（带缓存）
     */
    @Cacheable(value = CacheConfig.DB_SCHEMAS_CACHE, key = "#connectionId")
    public List<String> getSchemas(Long connectionId) {
        logger.info("从数据库获取Schema列表，连接ID: {}", connectionId);
        return dbConnectionService.getSchemas(connectionId);
    }

    // ================ 基础表信息缓存方法 ================

    /**
     * 获取基础表信息列表（带缓存）- 快速加载
     */
    @Cacheable(value = CacheConfig.BASIC_TABLES_CACHE, key = "#connectionId + '_' + (#schema != null ? #schema : 'default')")
    public List<BasicTableInfo> getBasicTablesInfo(Long connectionId, String schema) {
        logger.info("从数据库获取基础表信息列表，连接ID: {}, Schema: {}", connectionId, schema);
        return dbConnectionService.getBasicTablesInfo(connectionId, schema);
    }

    /**
     * 获取基础表信息列表（分页，带缓存）- 快速加载
     */
    @Cacheable(value = CacheConfig.BASIC_TABLE_METADATA_CACHE, 
               key = "#connectionId + '_' + (#request.schema != null ? #request.schema : 'default') + '_page_' + #request.page + '_' + #request.size + '_' + (#request.search != null ? #request.search : '') + '_' + #request.sortBy + '_' + #request.sortOrder")
    public TablePageResponse<BasicTableInfo> getBasicTablesWithPagination(Long connectionId, TablePageRequest request) {
        logger.info("从数据库获取基础表信息分页列表，连接ID: {}, 页码: {}, 大小: {}", connectionId, request.getPage(), request.getSize());
        return dbConnectionService.getBasicTablesWithPagination(connectionId, request);
    }

    /**
     * 清除指定连接的所有缓存
     */
    @CacheEvict(value = {CacheConfig.DB_TABLES_CACHE, CacheConfig.TABLE_COLUMNS_CACHE, 
                         CacheConfig.DB_SCHEMAS_CACHE, CacheConfig.TABLE_METADATA_CACHE,
                         CacheConfig.BASIC_TABLES_CACHE, CacheConfig.BASIC_TABLE_METADATA_CACHE}, 
                key = "#connectionId + '*'", allEntries = false)
    public void evictConnectionCache(Long connectionId) {
        logger.info("清除连接缓存，连接ID: {}", connectionId);
        
        // 手动清除所有相关的缓存条目
        evictCacheByPattern(CacheConfig.DB_TABLES_CACHE, connectionId.toString());
        evictCacheByPattern(CacheConfig.TABLE_COLUMNS_CACHE, connectionId.toString());
        evictCacheByPattern(CacheConfig.DB_SCHEMAS_CACHE, connectionId.toString());
        evictCacheByPattern(CacheConfig.TABLE_METADATA_CACHE, connectionId.toString());
        evictCacheByPattern(CacheConfig.BASIC_TABLES_CACHE, connectionId.toString());
        evictCacheByPattern(CacheConfig.BASIC_TABLE_METADATA_CACHE, connectionId.toString());
    }

    /**
     * 清除指定表的缓存
     */
    public void evictTableCache(Long connectionId, String tableName, String schemaName) {
        logger.info("清除表缓存，连接ID: {}, 表名: {}, Schema: {}", connectionId, tableName, schemaName);
        
        String cacheKey = connectionId + "_" + tableName + "_" + (schemaName != null ? schemaName : "default");
        Cache cache = cacheManager.getCache(CacheConfig.TABLE_COLUMNS_CACHE);
        if (cache != null) {
            cache.evict(cacheKey);
        }
    }

    /**
     * 清除所有缓存
     */
    @CacheEvict(value = {CacheConfig.DB_TABLES_CACHE, CacheConfig.TABLE_COLUMNS_CACHE, 
                         CacheConfig.DB_SCHEMAS_CACHE, CacheConfig.TABLE_METADATA_CACHE,
                         CacheConfig.BASIC_TABLES_CACHE, CacheConfig.BASIC_TABLE_METADATA_CACHE}, 
                allEntries = true)
    public void evictAllCache() {
        logger.info("清除所有数据库元数据缓存");
    }

    /**
     * 根据模式清除缓存
     */
    private void evictCacheByPattern(String cacheName, String pattern) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache = 
                (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();
            
            // 遍历所有key，清除匹配的条目
            caffeineCache.asMap().keySet().removeIf(key -> 
                key != null && key.toString().startsWith(pattern));
        }
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        CacheStats stats = new CacheStats();
        
        for (String cacheName : java.util.Arrays.asList(
            CacheConfig.DB_TABLES_CACHE,
            CacheConfig.TABLE_COLUMNS_CACHE,
            CacheConfig.DB_SCHEMAS_CACHE,
            CacheConfig.TABLE_METADATA_CACHE,
            CacheConfig.BASIC_TABLES_CACHE,
            CacheConfig.BASIC_TABLE_METADATA_CACHE
        )) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
                com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache = 
                    (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();
                
                com.github.benmanes.caffeine.cache.stats.CacheStats cacheStats = caffeineCache.stats();
                stats.addCacheInfo(cacheName, cacheStats);
            }
        }
        
        return stats;
    }

    /**
     * 缓存统计信息类
     */
    public static class CacheStats {
        private java.util.Map<String, Object> cacheInfos = new java.util.HashMap<>();
        
        public void addCacheInfo(String cacheName, com.github.benmanes.caffeine.cache.stats.CacheStats stats) {
            java.util.Map<String, Object> info = new java.util.HashMap<>();
            info.put("hitCount", stats.hitCount());
            info.put("missCount", stats.missCount());
            info.put("hitRate", stats.hitRate());
            info.put("evictionCount", stats.evictionCount());
            info.put("requestCount", stats.requestCount());
            
            cacheInfos.put(cacheName, info);
        }
        
        public java.util.Map<String, Object> getCacheInfos() {
            return cacheInfos;
        }
    }
}
