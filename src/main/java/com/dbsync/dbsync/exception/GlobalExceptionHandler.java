package com.dbsync.dbsync.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * 处理SQL异常
     */
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<Map<String, Object>> handleSQLException(SQLException e) {
        logger.error("SQL异常: {}", e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "数据库操作失败");
        response.put("message", formatSQLErrorMessage(e.getMessage()));
        response.put("code", "SQL_ERROR");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        logger.error("运行时异常: {}", e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "操作失败");
        response.put("message", e.getMessage());
        response.put("code", "RUNTIME_ERROR");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        logger.warn("参数验证失败: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        response.put("error", "参数验证失败");
        response.put("message", "请检查输入参数");
        response.put("details", errors);
        response.put("code", "VALIDATION_ERROR");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException e) {
        logger.warn("参数绑定失败: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        response.put("error", "参数绑定失败");
        response.put("message", "请检查输入参数格式");
        response.put("details", errors);
        response.put("code", "BIND_ERROR");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException e) {
        logger.warn("访问被拒绝: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "访问被拒绝");
        response.put("message", "您没有权限执行此操作");
        response.put("code", "ACCESS_DENIED");
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    /**
     * 处理认证失败异常
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException e) {
        logger.warn("认证失败: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "认证失败");
        response.put("message", "用户名或密码错误");
        response.put("code", "BAD_CREDENTIALS");
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("非法参数: {}", e.getMessage());
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "参数错误");
        response.put("message", e.getMessage());
        response.put("code", "ILLEGAL_ARGUMENT");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<Map<String, Object>> handleNullPointerException(NullPointerException e) {
        logger.error("空指针异常: {}", e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "系统内部错误");
        response.put("message", "系统处理异常，请稍后重试");
        response.put("code", "NULL_POINTER");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        logger.error("未处理的异常: {}", e.getMessage(), e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "系统错误");
        response.put("message", "系统处理异常，请联系管理员");
        response.put("code", "SYSTEM_ERROR");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 格式化SQL错误消息
     */
    private String formatSQLErrorMessage(String originalMessage) {
        if (originalMessage == null) {
            return "数据库操作失败";
        }
        
        // 常见SQL错误的友好提示
        if (originalMessage.contains("Connection refused")) {
            return "无法连接到数据库，请检查数据库服务是否正常运行";
        } else if (originalMessage.contains("Access denied")) {
            return "数据库访问被拒绝，请检查用户名和密码";
        } else if (originalMessage.contains("Unknown database")) {
            return "指定的数据库不存在";
        } else if (originalMessage.contains("Table") && originalMessage.contains("doesn't exist")) {
            return "指定的表不存在";
        } else if (originalMessage.contains("Syntax error")) {
            return "SQL语法错误，请检查SQL语句";
        } else if (originalMessage.contains("Duplicate entry")) {
            return "数据重复，违反唯一性约束";
        } else if (originalMessage.contains("Data too long")) {
            return "数据长度超出字段限制";
        } else if (originalMessage.contains("Cannot add or update a child row")) {
            return "外键约束违反，相关记录不存在";
        } else if (originalMessage.contains("Cannot delete or update a parent row")) {
            return "外键约束违反，存在相关子记录";
        } else if (originalMessage.contains("Timeout")) {
            return "数据库操作超时，请稍后重试";
        } else {
            // 返回简化的错误信息，避免暴露敏感信息
            return "数据库操作失败: " + originalMessage.substring(0, Math.min(originalMessage.length(), 100));
        }
    }
}
