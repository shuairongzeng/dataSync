package com.dbsync.dbsync.service;

import com.dbsync.dbsync.service.QueryService;
import com.dbsync.dbsync.util.SqlQueryAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 字段映射服务
 * 负责获取数据库字段的中文备注信息，支持缓存机制
 */
@Service
public class FieldMappingService {
    
    private static final Logger logger = LoggerFactory.getLogger(FieldMappingService.class);
    
    @Autowired
    private QueryService queryService;
    
    // Redis缓存（可选）
    private Object redisTemplate;
    
    // 本地缓存作为备选方案
    private final Map<String, CachedFieldMapping> localCache = new ConcurrentHashMap<>();
    
    @Autowired
    private SqlQueryAnalyzer sqlQueryAnalyzer;
    
    private static final String CACHE_PREFIX = "field_mapping:";
    private static final int CACHE_EXPIRE_HOURS = 24;
    private static final int MAX_TABLES_FOR_COMPLEX_QUERY = 20;
    
    // Redis缓存是否可用
    private boolean redisAvailable = false;
    
    /**
     * 缓存的字段映射数据
     */
    private static class CachedFieldMapping {
        private final Map<String, String> mappings;
        private final long timestamp;
        
        public CachedFieldMapping(Map<String, String> mappings) {
            this.mappings = new HashMap<>(mappings);
            this.timestamp = System.currentTimeMillis();
        }
        
        public Map<String, String> getMappings() {
            return new HashMap<>(mappings);
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_HOURS * 3600 * 1000L;
        }
    }
    
    @PostConstruct
    public void init() {
        try {
            // 尝试检测Redis是否可用
            Class.forName("org.springframework.data.redis.core.RedisTemplate");
            // 这里可以进一步检测Redis连接是否正常
            redisAvailable = false; // 暂时禁用Redis，使用本地缓存
            logger.info("字段映射服务初始化完成，使用本地缓存");
        } catch (ClassNotFoundException e) {
            redisAvailable = false;
            logger.info("Redis不可用，字段映射服务将使用本地缓存");
        }
    }
    
    /**
     * 获取字段中文名称映射
     */
    public Map<String, String> getFieldChineseNames(Long connectionId, 
                                                  Set<String> tableNames, 
                                                  String schema) {
        
        Map<String, String> allMappings = new HashMap<>();
        
        if (tableNames == null || tableNames.isEmpty()) {
            logger.debug("表名集合为空，返回空映射");
            return allMappings;
        }
        
        logger.debug("开始获取字段中文名称映射，连接ID: {}, Schema: {}, 表数量: {}", 
                    connectionId, schema, tableNames.size());
        
        for (String tableName : tableNames) {
            try {
                Map<String, String> tableMappings = getTableFieldMappings(connectionId, tableName, schema);
                allMappings.putAll(tableMappings);
                
                // 同时添加表名前缀的映射，用于处理多表查询
                for (Map.Entry<String, String> entry : tableMappings.entrySet()) {
                    String prefixedKey = tableName.toLowerCase() + "." + entry.getKey().toLowerCase();
                    allMappings.put(prefixedKey, entry.getValue());
                }
                
                logger.debug("表 {} 获取到 {} 个字段映射", tableName, tableMappings.size());
                
            } catch (Exception e) {
                logger.warn("获取表 {} 的字段映射失败: {}", tableName, e.getMessage());
            }
        }
        
        logger.debug("总共获取到 {} 个字段中文名称映射", allMappings.size());
        return allMappings;
    }
    
    /**
     * 获取单个表的字段映射
     */
    private Map<String, String> getTableFieldMappings(Long connectionId, String tableName, String schema) {
        String cacheKey = CACHE_PREFIX + connectionId + ":" + (schema != null ? schema : "default") + ":" + tableName.toLowerCase();
        
        // 先从缓存获取
        Map<String, String> cachedMappings = getCachedFieldMappings(cacheKey);
        if (cachedMappings != null) {
            logger.debug("从缓存获取表 {} 的字段映射，共 {} 个字段", tableName, cachedMappings.size());
            return cachedMappings;
        }
        
        // 从数据库获取字段信息
        Map<String, String> mappings = new HashMap<>();
        try {
            List<QueryService.ColumnInfo> columns = queryService.getTableColumns(connectionId, tableName, schema);
            
            for (QueryService.ColumnInfo column : columns) {
                String fieldName = column.getColumnName().toLowerCase();
                String remarks = column.getRemarks();
                
                if (remarks != null && !remarks.trim().isEmpty()) {
                    mappings.put(fieldName, remarks.trim());
                }
            }
            
            // 缓存结果
            cacheFieldMappings(cacheKey, mappings);
            
            logger.debug("从数据库获取表 {} 的字段映射，共 {} 个字段有中文备注", tableName, mappings.size());
            
        } catch (Exception e) {
            logger.error("获取表 {} 字段信息失败: {}", tableName, e.getMessage(), e);
        }
        
        return mappings;
    }
    
    /**
     * 批量获取查询涉及字段的中文名称
     */
    public Map<String, String> getQueryFieldDisplayNames(Long connectionId, 
                                                       String sql, 
                                                       String schema,
                                                       List<String> resultColumns) {
        
        if (resultColumns == null || resultColumns.isEmpty()) {
            logger.debug("结果列为空，返回空映射");
            return new HashMap<>();
        }
        
        Map<String, String> displayNames = new HashMap<>();
        
        try {
            logger.debug("开始分析SQL查询: {}", sql);
            
            // 1. 解析SQL获取表信息
            SqlQueryAnalyzer.QueryAnalysisResult analysisResult = sqlQueryAnalyzer.analyzeQuery(sql);
            
            logger.debug("SQL分析结果: 表数量={}, 字段数量={}, 复杂查询={}",
                        analysisResult.getTables().size(),
                        analysisResult.getFields().size(),
                        analysisResult.isComplexQuery());
            
            // 2. 如果是复杂查询，只能基于结果列名进行猜测
            if (analysisResult.isComplexQuery()) {
                return handleComplexQuery(connectionId, schema, resultColumns);
            }
            
            // 3. 获取字段映射
            Map<String, String> fieldMappings = getFieldChineseNames(
                connectionId, analysisResult.getTables(), schema);
            
            // 4. 处理每个结果列
            for (String column : resultColumns) {
                String displayName = findDisplayName(column, analysisResult, fieldMappings);
                if (displayName != null && !displayName.equals(column)) {
                    displayNames.put(column, displayName);
                    logger.debug("字段 {} -> {}", column, displayName);
                }
            }
            
            logger.info("为 {} 个字段设置了中文显示名称", displayNames.size());
            
        } catch (Exception e) {
            logger.error("获取查询字段显示名称失败: {}", e.getMessage(), e);
        }
        
        return displayNames;
    }
    
    /**
     * 处理复杂查询的字段映射
     */
    private Map<String, String> handleComplexQuery(Long connectionId, String schema, List<String> resultColumns) {
        Map<String, String> displayNames = new HashMap<>();
        
        logger.debug("处理复杂查询的字段映射，结果列数量: {}", resultColumns.size());
        
        try {
            // 获取当前schema下所有表的字段映射
            List<String> allTables = queryService.getTables(connectionId, schema);
            Set<String> limitedTables = allTables.stream()
                .limit(MAX_TABLES_FOR_COMPLEX_QUERY) // 限制表数量，避免性能问题
                .collect(Collectors.toSet());
            
            logger.debug("复杂查询模式下，将搜索 {} 个表的字段信息", limitedTables.size());
            
            Map<String, String> allFieldMappings = getFieldChineseNames(connectionId, limitedTables, schema);
            
            // 尝试匹配结果列名
            for (String column : resultColumns) {
                String chineseName = allFieldMappings.get(column.toLowerCase());
                if (chineseName != null) {
                    displayNames.put(column, chineseName);
                    logger.debug("复杂查询模式匹配: {} -> {}", column, chineseName);
                }
            }
            
            logger.debug("复杂查询模式下匹配到 {} 个字段的中文名称", displayNames.size());
            
        } catch (Exception e) {
            logger.warn("处理复杂查询字段映射失败: {}", e.getMessage(), e);
        }
        
        return displayNames;
    }
    
    /**
     * 查找字段的显示名称
     */
    private String findDisplayName(String columnName, 
                                 SqlQueryAnalyzer.QueryAnalysisResult analysisResult,
                                 Map<String, String> fieldMappings) {
        
        logger.debug("查找字段 {} 的显示名称", columnName);
        
        // 1. 直接查找
        String chineseName = fieldMappings.get(columnName.toLowerCase());
        if (chineseName != null) {
            logger.debug("直接匹配: {} -> {}", columnName, chineseName);
            return chineseName;
        }
        
        // 2. 通过别名查找原始字段名
        for (SqlQueryAnalyzer.FieldInfo field : analysisResult.getFields()) {
            if (columnName.equals(field.getAlias())) {
                String originalField = field.getFieldName().toLowerCase();
                chineseName = fieldMappings.get(originalField);
                if (chineseName != null) {
                    logger.debug("通过别名匹配: {} (别名: {}) -> {}", field.getFieldName(), columnName, chineseName);
                    return chineseName;
                }
                
                // 尝试加表名前缀
                if (field.getTableName() != null) {
                    String prefixedName = field.getTableName().toLowerCase() + "." + originalField;
                    chineseName = fieldMappings.get(prefixedName);
                    if (chineseName != null) {
                        logger.debug("通过表前缀匹配: {} -> {}", prefixedName, chineseName);
                        return chineseName;
                    }
                }
            }
        }
        
        // 3. 处理带表名前缀的情况
        if (columnName.contains(".")) {
            String[] parts = columnName.split("\\.");
            if (parts.length == 2) {
                String tableName = parts[0].toLowerCase();
                String fieldName = parts[1].toLowerCase();
                String prefixedName = tableName + "." + fieldName;
                chineseName = fieldMappings.get(prefixedName);
                if (chineseName != null) {
                    logger.debug("通过点号前缀匹配: {} -> {}", prefixedName, chineseName);
                    return chineseName;
                }
                
                // 也尝试不带前缀的查找
                chineseName = fieldMappings.get(fieldName);
                if (chineseName != null) {
                    logger.debug("去除前缀匹配: {} -> {}", fieldName, chineseName);
                    return chineseName;
                }
            }
        }
        
        // 4. 处理SELECT *的情况
        for (SqlQueryAnalyzer.FieldInfo field : analysisResult.getFields()) {
            if (field.isAllFields() && field.getTableName() != null) {
                String prefixedName = field.getTableName().toLowerCase() + "." + columnName.toLowerCase();
                chineseName = fieldMappings.get(prefixedName);
                if (chineseName != null) {
                    logger.debug("通过SELECT *匹配: {} -> {}", prefixedName, chineseName);
                    return chineseName;
                }
            }
        }
        
        logger.debug("未找到字段 {} 的中文显示名称", columnName);
        return null; // 没有找到对应的中文名
    }
    
    /**
     * 缓存字段映射
     */
    private void cacheFieldMappings(String cacheKey, Map<String, String> mappings) {
        try {
            if (redisAvailable) {
                // 使用Redis缓存（当前被禁用）
                logger.debug("Redis缓存已禁用，使用本地缓存");
            }
            
            // 使用本地缓存
            localCache.put(cacheKey, new CachedFieldMapping(mappings));
            logger.debug("成功缓存字段映射到本地缓存: {}, 数量: {}", cacheKey, mappings.size());
            
            // 清理过期的本地缓存条目
            cleanExpiredLocalCache();
            
        } catch (Exception e) {
            logger.warn("缓存字段映射失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从缓存获取字段映射
     */
    private Map<String, String> getCachedFieldMappings(String cacheKey) {
        try {
            if (redisAvailable) {
                // 使用Redis缓存（当前被禁用）
                logger.debug("Redis缓存已禁用，使用本地缓存");
            }
            
            // 使用本地缓存
            CachedFieldMapping cached = localCache.get(cacheKey);
            if (cached != null) {
                if (!cached.isExpired()) {
                    logger.debug("从本地缓存获取字段映射: {}", cacheKey);
                    return cached.getMappings();
                } else {
                    // 移除过期的缓存
                    localCache.remove(cacheKey);
                    logger.debug("本地缓存已过期，移除: {}", cacheKey);
                }
            }
            
        } catch (Exception e) {
            logger.warn("获取缓存字段映射失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 清除指定连接的所有字段映射缓存
     */
    public void clearConnectionCache(Long connectionId) {
        try {
            if (redisAvailable) {
                // 使用Redis缓存（当前被禁用）
                logger.debug("Redis缓存已禁用，清除本地缓存");
            }
            
            // 清除本地缓存
            String pattern = CACHE_PREFIX + connectionId + ":";
            Set<String> keysToRemove = new HashSet<>();
            
            for (String key : localCache.keySet()) {
                if (key.startsWith(pattern)) {
                    keysToRemove.add(key);
                }
            }
            
            for (String key : keysToRemove) {
                localCache.remove(key);
            }
            
            logger.info("清除连接 {} 的本地字段映射缓存，共 {} 个条目", connectionId, keysToRemove.size());
            
        } catch (Exception e) {
            logger.error("清除连接缓存失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 预热指定连接的字段映射缓存
     */
    public void warmupConnectionCache(Long connectionId, String schema) {
        try {
            logger.info("开始预热连接 {} 的字段映射缓存", connectionId);
            
            List<String> tables = queryService.getTables(connectionId, schema);
            Set<String> limitedTables = tables.stream()
                .limit(50) // 限制预热的表数量
                .collect(Collectors.toSet());
            
            logger.debug("将预热 {} 个表的字段映射", limitedTables.size());
            
            // 并发获取字段映射，触发缓存
            getFieldChineseNames(connectionId, limitedTables, schema);
            
            logger.info("完成连接 {} 的字段映射缓存预热", connectionId);
            
        } catch (Exception e) {
            logger.error("预热连接缓存失败: {}", e.getMessage(), e);
            throw new RuntimeException("预热缓存失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理过期的本地缓存条目
     */
    private void cleanExpiredLocalCache() {
        try {
            Set<String> keysToRemove = new HashSet<>();
            
            for (Map.Entry<String, CachedFieldMapping> entry : localCache.entrySet()) {
                if (entry.getValue().isExpired()) {
                    keysToRemove.add(entry.getKey());
                }
            }
            
            for (String key : keysToRemove) {
                localCache.remove(key);
            }
            
            if (!keysToRemove.isEmpty()) {
                logger.debug("清理过期的本地缓存条目，共 {} 个", keysToRemove.size());
            }
            
        } catch (Exception e) {
            logger.warn("清理过期本地缓存失败: {}", e.getMessage());
        }
    }
}