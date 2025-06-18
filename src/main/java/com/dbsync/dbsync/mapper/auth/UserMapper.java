package com.dbsync.dbsync.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbsync.dbsync.model.User;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserMapper extends BaseMapper<User> {
    
    /**
     * 根据用户名查找用户
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查找用户
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    User findByEmail(@Param("email") String email);
    
    /**
     * 检查用户名是否存在
     */
    @Select("SELECT COUNT(*) FROM users WHERE username = #{username}")
    boolean existsByUsername(@Param("username") String username);
    
    /**
     * 检查邮箱是否存在
     */
    @Select("SELECT COUNT(*) FROM users WHERE email = #{email}")
    boolean existsByEmail(@Param("email") String email);
    
    /**
     * 插入新用户
     */
    @Insert("INSERT INTO users (username, password, email, role, enabled, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{email}, #{role}, #{enabled}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);
    
    /**
     * 更新用户信息
     */
    @Update("UPDATE users SET password = #{password}, email = #{email}, role = #{role}, " +
            "enabled = #{enabled}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updateUser(User user);
    
    /**
     * 根据ID删除用户
     */
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    /**
     * 获取所有用户
     */
    @Select("SELECT * FROM users ORDER BY created_at DESC")
    List<User> findAllUsers();
    
    /**
     * 根据角色查找用户
     */
    @Select("SELECT * FROM users WHERE role = #{role} ORDER BY created_at DESC")
    List<User> findByRole(@Param("role") String role);
    
    /**
     * 启用/禁用用户
     */
    @Update("UPDATE users SET enabled = #{enabled}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateUserStatus(@Param("id") Long id, @Param("enabled") Boolean enabled);
}
