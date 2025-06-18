package com.dbsync.dbsync.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库测试控制器
 */
@RestController
@RequestMapping("/api/test/db")
public class DatabaseTestController {

    @Autowired
    @Qualifier("authDataSource")
    private DataSource authDataSource;

    @GetMapping("/auth-tables")
    public ResponseEntity<?> getAuthTables() {
        try {
            List<String> tables = new ArrayList<>();
            
            try (Connection connection = authDataSource.getConnection()) {
                DatabaseMetaData metaData = connection.getMetaData();
                try (ResultSet resultSet = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                    while (resultSet.next()) {
                        tables.add(resultSet.getString("TABLE_NAME"));
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("dataSource", "SQLite Auth Database");
            response.put("tables", tables);
            response.put("tableCount", tables.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get auth tables: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/auth-connection")
    public ResponseEntity<?> testAuthConnection() {
        try {
            try (Connection connection = authDataSource.getConnection()) {
                Map<String, Object> response = new HashMap<>();
                response.put("connected", true);
                response.put("url", connection.getMetaData().getURL());
                response.put("driverName", connection.getMetaData().getDriverName());
                response.put("driverVersion", connection.getMetaData().getDriverVersion());
                
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to connect to auth database: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
