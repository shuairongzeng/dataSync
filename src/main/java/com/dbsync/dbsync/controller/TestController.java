package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.mapper.auth.UserMapper;
import com.dbsync.dbsync.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器 - 用于验证应用程序是否正常运行
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    @Qualifier("userMapper")
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/public")
    public ResponseEntity<?> publicEndpoint() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "This is a public endpoint");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "DbSync Authentication Service");
        response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin-info")
    public ResponseEntity<?> getAdminInfo() {
        try {
            User admin = userMapper.findByUsername("admin");
            if (admin == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Admin user not found");
                return ResponseEntity.status(404).body(error);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("username", admin.getUsername());
            response.put("email", admin.getEmail());
            response.put("role", admin.getRole());
            response.put("enabled", admin.getEnabled());
            response.put("passwordHash", admin.getPassword());
            response.put("passwordLength", admin.getPassword().length());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get admin info: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/verify-password")
    public ResponseEntity<?> verifyPassword(@RequestParam String username, @RequestParam String password) {
        try {
            User user = userMapper.findByUsername(username);
            if (user == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not found");
                return ResponseEntity.status(404).body(error);
            }

            boolean matches = passwordEncoder.matches(password, user.getPassword());

            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("passwordMatches", matches);
            response.put("storedHash", user.getPassword());
            response.put("inputPassword", password);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to verify password: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/update-admin-password")
    public ResponseEntity<?> updateAdminPassword(@RequestParam String newPassword) {
        try {
            User admin = userMapper.findByUsername("admin");
            if (admin == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Admin user not found");
                return ResponseEntity.status(404).body(error);
            }

            String newHash = passwordEncoder.encode(newPassword);
            admin.setPassword(newHash);

            int result = userMapper.updateUser(admin);

            Map<String, Object> response = new HashMap<>();
            response.put("success", result > 0);
            response.put("newPassword", newPassword);
            response.put("newHash", newHash);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update password: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
