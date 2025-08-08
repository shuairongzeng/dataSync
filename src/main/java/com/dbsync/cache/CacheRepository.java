package com.dbsync.cache;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Cache repository for SQLite operations
 */
@Repository
@Mapper
public interface CacheRepository {
    
    @Select("SELECT * FROM cache_data WHERE cache_key = #{cacheKey}")
    @Results({
        @Result(property = "cacheKey", column = "cache_key"),
        @Result(property = "dataContent", column = "data_content"),
        @Result(property = "compressionType", column = "compression_type"),
        @Result(property = "createdAt", column = "created_at")
    })
    Optional<CacheData> findByCacheKey(@Param("cacheKey") String cacheKey);
    
    @Insert("INSERT OR REPLACE INTO cache_metadata " +
            "(cache_key, cache_type, data_source, created_at, updated_at, expires_at, data_size, checksum) " +
            "VALUES (#{cacheKey}, #{cacheType}, #{dataSource}, datetime('now'), datetime('now'), #{expiresAt}, #{dataSize}, #{checksum})")
    void saveCacheMetadata(@Param("cacheKey") String cacheKey,
                          @Param("cacheType") String cacheType,
                          @Param("dataSource") String dataSource,
                          @Param("expiresAt") LocalDateTime expiresAt,
                          @Param("dataSize") int dataSize,
                          @Param("checksum") String checksum);
    
    @Insert("INSERT OR REPLACE INTO cache_data " +
            "(cache_key, data_content, compression_type, created_at) " +
            "VALUES (#{cacheKey}, #{dataContent}, 'none', datetime('now'))")
    void saveCacheData(@Param("cacheKey") String cacheKey,
                      @Param("dataContent") String dataContent);
    
    default void saveCache(String cacheKey, String cacheType, String dataSource, 
                          String dataContent, int dataSize, String checksum, LocalDateTime expiresAt) {
        saveCacheMetadata(cacheKey, cacheType, dataSource, expiresAt, dataSize, checksum);
        saveCacheData(cacheKey, dataContent);
    }
    
    @Update("UPDATE cache_metadata SET hit_count = hit_count + 1 WHERE cache_key = #{cacheKey}")
    void incrementHitCount(@Param("cacheKey") String cacheKey);
    
    @Delete("DELETE FROM cache_metadata WHERE cache_key = #{cacheKey}")
    void deleteMetadataByKey(@Param("cacheKey") String cacheKey);
    
    @Delete("DELETE FROM cache_data WHERE cache_key = #{cacheKey}")
    void deleteCacheDataByKey(@Param("cacheKey") String cacheKey);
    
    default void deleteByKey(String cacheKey) {
        deleteMetadataByKey(cacheKey);
        deleteCacheDataByKey(cacheKey);
    }
    
    @Delete("DELETE FROM cache_metadata WHERE cache_type = #{cacheType}")
    void deleteMetadataByType(@Param("cacheType") String cacheType);
    
    @Delete("DELETE FROM cache_data WHERE cache_key IN " +
            "(SELECT cache_key FROM cache_metadata WHERE cache_type = #{cacheType})")
    void deleteCacheDataByType(@Param("cacheType") String cacheType);
    
    default void deleteByType(String cacheType) {
        deleteCacheDataByType(cacheType);
        deleteMetadataByType(cacheType);
    }
    
    @Delete("DELETE FROM cache_metadata WHERE data_source = #{dataSource}")
    void deleteMetadataByDataSource(@Param("dataSource") String dataSource);
    
    @Delete("DELETE FROM cache_data WHERE cache_key IN " +
            "(SELECT cache_key FROM cache_metadata WHERE data_source = #{dataSource})")
    void deleteCacheDataByDataSource(@Param("dataSource") String dataSource);
    
    default void deleteByDataSource(String dataSource) {
        deleteCacheDataByDataSource(dataSource);
        deleteMetadataByDataSource(dataSource);
    }
    
    @Delete("DELETE FROM cache_metadata WHERE expires_at IS NOT NULL AND expires_at < datetime('now')")
    int deleteExpiredMetadata();
    
    @Delete("DELETE FROM cache_data WHERE cache_key NOT IN (SELECT cache_key FROM cache_metadata)")
    int deleteOrphanedCacheData();
    
    default int deleteExpired() {
        int deletedMetadata = deleteExpiredMetadata();
        deleteOrphanedCacheData();
        return deletedMetadata;
    }
    
    @Select("SELECT " +
            "COUNT(*) as totalEntries, " +
            "SUM(hit_count) as totalHits, " +
            "SUM(data_size) as totalSize, " +
            "AVG(hit_count) as averageHits " +
            "FROM cache_metadata")
    @Results({
        @Result(property = "totalEntries", column = "totalEntries"),
        @Result(property = "totalHits", column = "totalHits"),
        @Result(property = "totalSize", column = "totalSize"),
        @Result(property = "averageHits", column = "averageHits")
    })
    CacheStats getCacheStats();
    
    @Select("SELECT cache_key, cache_type, data_source, created_at, updated_at, expires_at, hit_count, data_size " +
            "FROM cache_metadata ORDER BY updated_at DESC")
    @Results({
        @Result(property = "cacheKey", column = "cache_key"),
        @Result(property = "cacheType", column = "cache_type"),
        @Result(property = "dataSource", column = "data_source"),
        @Result(property = "createdAt", column = "created_at"),
        @Result(property = "updatedAt", column = "updated_at"),
        @Result(property = "expiresAt", column = "expires_at"),
        @Result(property = "hitCount", column = "hit_count"),
        @Result(property = "dataSize", column = "data_size")
    })
    List<CacheMetadata> getAllCacheMetadata();
}