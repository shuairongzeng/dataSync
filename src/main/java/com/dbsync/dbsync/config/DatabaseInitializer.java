package com.dbsync.dbsync.config;

import com.dbsync.dbsync.mapper.auth.UserMapper;
import com.dbsync.dbsync.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 数据库初始化组件
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    @Qualifier("userMapper")
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing database...");
        
        try {
            // 检查是否已存在admin用户
            if (!userMapper.existsByUsername("admin")) {
                logger.info("Creating default admin user...");
                
                User adminUser = new User();
                adminUser.setUsername("admin");
                adminUser.setPassword(passwordEncoder.encode("admin123"));
                adminUser.setEmail("admin@dbsync.com");
                adminUser.setRole("ADMIN");
                adminUser.setEnabled(true);
                adminUser.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                adminUser.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                int result = userMapper.insertUser(adminUser);
                if (result > 0) {
                    logger.info("Default admin user created successfully");
                } else {
                    logger.error("Failed to create default admin user");
                }
            } else {
                logger.info("Admin user already exists");
            }
            
            // 验证用户是否存在
            User existingAdmin = userMapper.findByUsername("admin");
            if (existingAdmin != null) {
                logger.info("Admin user verification successful: {}", existingAdmin.getUsername());
            } else {
                logger.error("Admin user verification failed");
            }
            
        } catch (Exception e) {
            logger.error("Error during database initialization: {}", e.getMessage(), e);
        }
    }
}
