package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.progress.ProgressManager;
import com.dbsync.dbsync.service.DatabaseSyncService;
import com.dbsync.dbsync.typemapping.TypeMappingRegistry;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dbsync.dbsync.controller.dto.SyncRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/sync")
public class SyncController {
    private static final Logger logger = LoggerFactory.getLogger(SyncController.class);

    private final SqlSessionFactory oracleSqlSessionFactory;
    private final SqlSessionFactory postgresSqlSessionFactory;
    private final TypeMappingRegistry typeMappingRegistry;
    private final ProgressManager progressManager;

    @Autowired
    public SyncController(@Qualifier("oracleSqlSessionFactory") SqlSessionFactory oracleSqlSessionFactory,
                          @Qualifier("postgresSqlSessionFactory") SqlSessionFactory postgresSqlSessionFactory,
                          TypeMappingRegistry typeMappingRegistry,
                          ProgressManager progressManager) {
        this.oracleSqlSessionFactory = oracleSqlSessionFactory;
        this.postgresSqlSessionFactory = postgresSqlSessionFactory;
        this.typeMappingRegistry = typeMappingRegistry;
        this.progressManager = progressManager;
    }

    @PostMapping("/oracle-to-postgres")
    public ResponseEntity<String> startSync(@RequestBody SyncRequest syncRequest) { // Changed return type
        String taskId = "api-oracle-to-postgres-sync-" + System.currentTimeMillis(); // Define taskId outside try for catch block access if needed, or include in success response
        try {
            String sourceDbType = "oracle";
            String targetDbType = "postgresql";
            List<String> tablesToSync = syncRequest.getTablesToSync();
            String sourceSchemaName = syncRequest.getSourceSchemaName() != null ? syncRequest.getSourceSchemaName() : ""; // Default to empty if null
            String targetSchemaName = syncRequest.getTargetSchemaName() != null ? syncRequest.getTargetSchemaName() : ""; // Default to empty if null
            boolean truncateBeforeSync = syncRequest.isTruncateBeforeSync();

            // Create a new DatabaseSyncService instance for this specific request
            // This mirrors the behavior in CommandLineRunner for setting specific sync properties like truncateBeforeSync and targetSchemaName
            DatabaseSyncService requestSpecificSyncService = new DatabaseSyncService(
                    oracleSqlSessionFactory,      // Injected field
                    postgresSqlSessionFactory,  // Injected field
                    truncateBeforeSync,
                    typeMappingRegistry,        // Injected field
                    sourceDbType,
                    targetDbType,
                    targetSchemaName,
                    progressManager             // Injected field
            );

            logger.info("Starting API synchronization task: {}", taskId);
            logger.info("Source Schema: {}, Target Schema: {}", sourceSchemaName, targetSchemaName);
            logger.info("Tables to sync: {}", tablesToSync);
            logger.info("Truncate before sync: {}", truncateBeforeSync);

            requestSpecificSyncService.syncDatabase(taskId, tablesToSync, sourceSchemaName);

            logger.info("API Synchronization task enqueued/completed: {}", taskId);
            return ResponseEntity.ok("Synchronization task " + taskId + " started successfully.");

        } catch (Exception e) {
            logger.error("Error during API synchronization task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during synchronization task " + taskId + ": " + e.getMessage());
        }
    }
}
