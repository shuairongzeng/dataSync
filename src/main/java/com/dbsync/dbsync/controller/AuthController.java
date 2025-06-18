package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.model.User;
import com.dbsync.dbsync.model.dto.JwtResponse;
import com.dbsync.dbsync.model.dto.LoginRequest;
import com.dbsync.dbsync.model.dto.RegisterRequest;
import com.dbsync.dbsync.model.dto.UserResponse;
import com.dbsync.dbsync.service.UserDetailsImpl;
import com.dbsync.dbsync.service.UserService;
import com.dbsync.dbsync.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    /**
     * 用户登录
     */
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")));
        } catch (Exception e) {
            logger.error("Authentication failed for user: {}", loginRequest.getUsername(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "用户名或密码错误");
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        try {
            User user = userService.registerUser(signUpRequest);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "用户注册成功");
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Registration failed for user: {}", signUpRequest.getUsername(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "未认证");
            return ResponseEntity.badRequest().body(error);
        }
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userService.findByUsername(userDetails.getUsername());
        
        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "用户不存在");
            return ResponseEntity.badRequest().body(error);
        }
        
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
        
        return ResponseEntity.ok(userResponse);
    }
    
    /**
     * 获取所有用户（仅管理员）
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // 检查是否为管理员
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "权限不足");
            return ResponseEntity.status(403).body(error);
        }
        
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    /**
     * 启用/禁用用户（仅管理员）
     */
    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(@PathVariable Long id, 
                                            @RequestParam Boolean enabled,
                                            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // 检查是否为管理员
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        
        if (!isAdmin) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "权限不足");
            return ResponseEntity.status(403).body(error);
        }
        
        boolean success = userService.updateUserStatus(id, enabled);
        if (success) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "用户状态更新成功");
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> error = new HashMap<>();
            error.put("error", "用户状态更新失败");
            return ResponseEntity.badRequest().body(error);
        }
    }
}
