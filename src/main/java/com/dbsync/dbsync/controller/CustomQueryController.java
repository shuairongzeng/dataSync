package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.model.CustomQueryRequest;
import com.dbsync.dbsync.model.DbConfig;
import com.dbsync.dbsync.service.DatabaseSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/custom-query")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class CustomQueryController {

    private static final Logger logger = LoggerFactory.getLogger(CustomQueryController.class);
    private final DatabaseSyncService databaseSyncService;

    @Autowired
    public CustomQueryController(DatabaseSyncService databaseSyncService) {
        this.databaseSyncService = databaseSyncService;
    }

    @PostMapping("/execute-and-save")
    public ResponseEntity<?> executeQueryAndSave(@RequestBody CustomQueryRequest request) {
        String taskId = UUID.randomUUID().toString();
        logger.info("Task [{}]: Received custom query request to execute and save. Target table: {}", taskId, request.getTargetTableName());

        try {
            if (request.getSourceDbConfig() == null) {
                throw new IllegalArgumentException("Source DB config is missing.");
            }
            if (request.getTargetDbConfig() == null) {
                throw new IllegalArgumentException("Target DB config is missing.");
            }
            if (request.getCustomSql() == null || request.getCustomSql().trim().isEmpty()) {
                throw new IllegalArgumentException("Custom SQL query is missing or empty.");
            }
            if (request.getTargetTableName() == null || request.getTargetTableName().trim().isEmpty()) {
                throw new IllegalArgumentException("Target table name is missing or empty.");
            }
            // targetSchemaName can be optional, so no strict check here unless required by service

            validateDbConfig(request.getSourceDbConfig(), "Source");
            validateDbConfig(request.getTargetDbConfig(), "Target");

            Map<String, String> sourceConnectionDetails = convertDbConfigToMap(request.getSourceDbConfig());
            Map<String, String> targetConnectionDetails = convertDbConfigToMap(request.getTargetDbConfig());

            // Asynchronously execute the service call if it's long-running.
            // For now, synchronous execution for simplicity.
            databaseSyncService.executeCustomQueryAndSaveResults(
                    taskId,
                    sourceConnectionDetails,
                    targetConnectionDetails,
                    request.getCustomSql(),
                    request.getTargetTableName(),
                    request.getTargetSchemaName() // Can be null
            );

            Map<String, String> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("message", "Custom query task started successfully and is processing.");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Task [{}]: Invalid arguments for custom query task. Error: {}", taskId, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("taskId", taskId);
            errorResponse.put("error", "Invalid arguments: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (RuntimeException e) { // Catching RuntimeException specifically if thrown by service for known issues
            logger.error("Task [{}]: Business logic error processing custom query task. Error: {}", taskId, e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("taskId", taskId);
            errorResponse.put("error", e.getMessage()); // Use the exception message directly for client
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(errorResponse); // 417 or similar
        } catch (Exception e) {
            logger.error("Task [{}]: Failed to start or process custom query task. Error: {}", taskId, e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("taskId", taskId);
            errorResponse.put("error", "Failed to process custom query task: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private Map<String, String> convertDbConfigToMap(DbConfig dbConfig) {
        // This method now assumes dbConfig is not null, as validateDbConfig would have been called.
        Map<String, String> map = new HashMap<>();
        map.put("url", dbConfig.getUrl());
        map.put("username", dbConfig.getUsername());
        map.put("password", dbConfig.getPassword());
        map.put("driverClassName", dbConfig.getDriverClassName());
        map.put("dbType", dbConfig.getDbType());
        return map;
    }

    private void validateDbConfig(DbConfig dbConfig, String configType) {
        if (dbConfig.getUrl() == null || dbConfig.getUrl().trim().isEmpty()) {
            throw new IllegalArgumentException(configType + " DB URL is missing or empty.");
        }
        if (dbConfig.getDriverClassName() == null || dbConfig.getDriverClassName().trim().isEmpty()) {
            throw new IllegalArgumentException(configType + " DB driver class name is missing or empty.");
        }
        if (dbConfig.getDbType() == null || dbConfig.getDbType().trim().isEmpty()) {
            throw new IllegalArgumentException(configType + " DB type is missing or empty.");
        }
        // Username and password can be optional
    }
}
