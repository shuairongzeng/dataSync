package com.dbsync.dbsync.service;

import com.dbsync.dbsync.mapper.auth.UserMapper;
import com.dbsync.dbsync.model.User;
import com.dbsync.dbsync.model.dto.RegisterRequest;
import com.dbsync.dbsync.model.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务类
 */
@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    @Qualifier("userMapper")
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 用户注册
     */
    @Transactional
    public User registerUser(RegisterRequest registerRequest) {
        logger.info("Registering new user: {}", registerRequest.getUsername());
        
        // 检查用户名是否已存在
        if (userMapper.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("用户名已存在: " + registerRequest.getUsername());
        }
        
        // 检查邮箱是否已存在
        if (userMapper.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("邮箱已存在: " + registerRequest.getEmail());
        }
        
        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setRole(registerRequest.getRole());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        user.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 保存用户
        int result = userMapper.insertUser(user);
        if (result > 0) {
            logger.info("User registered successfully: {}", user.getUsername());
            return user;
        } else {
            throw new RuntimeException("用户注册失败");
        }
    }
    
    /**
     * 根据用户名查找用户
     */
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    /**
     * 根据邮箱查找用户
     */
    public User findByEmail(String email) {
        return userMapper.findByEmail(email);
    }
    
    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return userMapper.existsByUsername(username);
    }
    
    /**
     * 检查邮箱是否存在
     */
    public boolean existsByEmail(String email) {
        return userMapper.existsByEmail(email);
    }
    
    /**
     * 获取所有用户
     */
    public List<UserResponse> getAllUsers() {
        List<User> users = userMapper.findAllUsers();
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据角色查找用户
     */
    public List<UserResponse> getUsersByRole(String role) {
        List<User> users = userMapper.findByRole(role);
        return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 启用/禁用用户
     */
    @Transactional
    public boolean updateUserStatus(Long id, Boolean enabled) {
        int result = userMapper.updateUserStatus(id, enabled);
        return result > 0;
    }
    
    /**
     * 删除用户
     */
    @Transactional
    public boolean deleteUser(Long id) {
        int result = userMapper.deleteById(id);
        return result > 0;
    }
    
    /**
     * 转换User为UserResponse
     */
    private UserResponse convertToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
    
    /**
     * 验证密码
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
