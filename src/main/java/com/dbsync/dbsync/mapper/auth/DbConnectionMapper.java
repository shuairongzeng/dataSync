package com.dbsync.dbsync.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbsync.dbsync.model.DbConnection;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 数据库连接数据访问接口
 */
@Repository
public interface DbConnectionMapper extends BaseMapper<DbConnection> {
    
    /**
     * 根据名称查找数据库连接
     */
    @Select("SELECT * FROM db_connections WHERE name = #{name}")
    DbConnection findByName(@Param("name") String name);
    
    /**
     * 根据数据库类型查找连接
     */
    @Select("SELECT * FROM db_connections WHERE db_type = #{dbType} AND enabled = 1 ORDER BY created_at DESC")
    List<DbConnection> findByDbType(@Param("dbType") String dbType);
    
    /**
     * 查找所有启用的数据库连接
     */
    @Select("SELECT * FROM db_connections WHERE enabled = 1 ORDER BY created_at DESC")
    List<DbConnection> findAllEnabled();
    
    /**
     * 检查连接名称是否存在
     */
    @Select("SELECT COUNT(*) FROM db_connections WHERE name = #{name}")
    boolean existsByName(@Param("name") String name);
    
    /**
     * 检查连接名称是否存在（排除指定ID）
     */
    @Select("SELECT COUNT(*) FROM db_connections WHERE name = #{name} AND id != #{id}")
    boolean existsByNameExcludingId(@Param("name") String name, @Param("id") Long id);
    
    /**
     * 插入新的数据库连接
     */
    @Insert("INSERT INTO db_connections (name, db_type, host, port, database, username, password, schema, description, enabled, created_at, updated_at) " +
            "VALUES (#{name}, #{dbType}, #{host}, #{port}, #{database}, #{username}, #{password}, #{schema}, #{description}, #{enabled}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertConnection(DbConnection connection);
    
    /**
     * 更新数据库连接
     */
    @Update("UPDATE db_connections SET name = #{name}, db_type = #{dbType}, host = #{host}, port = #{port}, " +
            "database = #{database}, username = #{username}, password = #{password}, schema = #{schema}, " +
            "description = #{description}, enabled = #{enabled}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateConnection(DbConnection connection);
    
    /**
     * 根据ID删除数据库连接
     */
    @Delete("DELETE FROM db_connections WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    /**
     * 启用/禁用数据库连接
     */
    @Update("UPDATE db_connections SET enabled = #{enabled}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateConnectionStatus(@Param("id") Long id, @Param("enabled") Boolean enabled);
    
    /**
     * 根据ID查找数据库连接
     */
    @Select("SELECT * FROM db_connections WHERE id = #{id}")
    DbConnection findById(@Param("id") Long id);
    
    /**
     * 获取所有数据库连接
     */
    @Select("SELECT * FROM db_connections ORDER BY created_at DESC")
    List<DbConnection> findAllConnections();
}