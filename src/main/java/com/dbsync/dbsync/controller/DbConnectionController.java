package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.service.DbConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库连接管理控制器 - 简化版本用于测试
 */
@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = "*")
public class DbConnectionController {

    @Autowired
    private DbConnectionService dbConnectionService;

    /**
     * 获取所有数据库连接
     */
    @GetMapping("/connections")
    public List<DbConnection> getAllConnections() {
        return dbConnectionService.getAllConnections();
    }

    /**
     * 创建数据库连接
     */
    @PostMapping("/connections")
    public Object createConnection(@RequestBody DbConnection connection) {
        try {
            return dbConnectionService.createConnection(connection);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * 更新数据库连接
     */
    @PutMapping("/connections/{id}")
    public Object updateConnection(@PathVariable Long id, @RequestBody DbConnection connection) {
        try {
            return dbConnectionService.updateConnection(id, connection);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * 删除数据库连接
     */
    @DeleteMapping("/connections/{id}")
    public Object deleteConnection(@PathVariable Long id) {
        try {
            boolean success = dbConnectionService.deleteConnection(id);
            if (success) {
                Map<String, String> result = new HashMap<>();
                result.put("message", "删除成功");
                return result;
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "删除失败");
                return error;
            }
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * 测试数据库连接
     */
    @PostMapping("/test-connection")
    public Object testConnection(@RequestBody DbConnection connection) {
        try {
            DbConnectionService.DbTestResult result = dbConnectionService.testConnection(connection);
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("connectionTime", result.getConnectionTime());
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "测试失败: " + e.getMessage());
            response.put("connectionTime", 0);
            return response;
        }
    }

    /**
     * 根据ID获取数据库连接
     */
    @GetMapping("/connections/{id}")
    public Object getConnectionById(@PathVariable Long id) {
        try {
            DbConnection connection = dbConnectionService.getConnectionById(id);
            if (connection != null) {
                return connection;
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "数据库连接不存在");
                return error;
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "查询失败");
            return error;
        }
    }
}