package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.progress.ProgressManager;
import com.dbsync.dbsync.service.DatabaseSyncService;
import com.dbsync.dbsync.typemapping.TypeMappingRegistry;
import org.apache.ibatis.session.SqlSessionFactory;
import com.dbsync.dbsync.progress.TaskProgress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dbsync.dbsync.controller.dto.SyncRequest;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final ExecutorService executorService;

    @Autowired
    public SyncController(@Qualifier("oracleSqlSessionFactory") SqlSessionFactory oracleSqlSessionFactory,
                          @Qualifier("postgresSqlSessionFactory") SqlSessionFactory postgresSqlSessionFactory,
                          TypeMappingRegistry typeMappingRegistry,
                          ProgressManager progressManager) {
        this.oracleSqlSessionFactory = oracleSqlSessionFactory;
        this.postgresSqlSessionFactory = postgresSqlSessionFactory;
        this.typeMappingRegistry = typeMappingRegistry;
        this.progressManager = progressManager;
        this.executorService = Executors.newCachedThreadPool();
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

            // Submit the synchronization task to the executor service
            executorService.submit(() -> {
                try {
                    logger.info("Starting background synchronization task: {}", taskId);
                    requestSpecificSyncService.syncDatabase(taskId, tablesToSync, sourceSchemaName);
                    logger.info("Background synchronization task {} completed.", taskId);
                } catch (Exception e) {
                    logger.error("Error during background synchronization task {}: {}", taskId, e.getMessage(), e);
                    // Optional: Update task progress to FAILED here if not handled by syncDatabase itself
                    // progressManager.completeTaskWithError(taskId, e.getMessage());
                }
            });

            logger.info("API Synchronization task {} enqueued.", taskId); // Changed log message
            return ResponseEntity.ok("Synchronization task " + taskId + " started successfully and is running in the background."); // Adjusted response message

        } catch (Exception e) {
            // This catch block now primarily handles errors related to task submission itself
            logger.error("Error submitting API synchronization task {}: {}", taskId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error starting synchronization task " + taskId + ": " + e.getMessage());
        }
    }

    @GetMapping("/status/{taskId}")
    public ResponseEntity<?> getSyncStatus(@PathVariable("taskId") String taskId) {
        TaskProgress taskProgress = this.progressManager.getTaskProgress(taskId);
        if (taskProgress != null) {
            return ResponseEntity.ok(taskProgress);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Task with ID '" + taskId + "' not found.");
        }
    }
}
