package com.dbsync.dbsync;

import com.dbsync.dbsync.config.DatabaseConfig;
import com.dbsync.dbsync.config.DataSourceProperties;
import com.dbsync.dbsync.config.DbsyncProperties;
import com.dbsync.dbsync.config.SyncTaskProperties;
import com.dbsync.dbsync.service.DatabaseSyncService;
import com.dbsync.dbsync.typemapping.TypeMappingRegistry;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DbsyncApplication {

    private static final Logger logger = LoggerFactory.getLogger(DbsyncApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DbsyncApplication.class, args);
    }

import com.dbsync.dbsync.progress.ProgressManager; // Added import

@SpringBootApplication
public class DbsyncApplication {

    private static final Logger logger = LoggerFactory.getLogger(DbsyncApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DbsyncApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(DbsyncProperties dbsyncProperties,
                                               DatabaseConfig databaseConfig,
                                               TypeMappingRegistry typeMappingRegistry,
                                               ProgressManager progressManager) { // Added ProgressManager
        return args -> {
            if (dbsyncProperties.getTasks() == null || dbsyncProperties.getTasks().isEmpty()) {
                logger.info("No synchronization tasks defined in configuration. Exiting.");
                return;
            }

            logger.info("Found {} synchronization task(s). Starting execution...", dbsyncProperties.getTasks().size());

            for (SyncTaskProperties task : dbsyncProperties.getTasks()) {
                logger.info("Starting task: {}", task.getName());
                try {
                    DataSourceProperties sourceConnProps = databaseConfig.getConnections().get(task.getSourceConnectionId());
                    DataSourceProperties targetConnProps = databaseConfig.getConnections().get(task.getTargetConnectionId());

                    if (sourceConnProps == null) {
                        logger.error("Task '{}': Source connection ID '{}' not found in db.connections. Skipping task.", task.getName(), task.getSourceConnectionId());
                        continue;
                    }
                    if (targetConnProps == null) {
                        logger.error("Task '{}': Target connection ID '{}' not found in db.connections. Skipping task.", task.getName(), task.getTargetConnectionId());
                        continue;
                    }

                    String sourceDbType = sourceConnProps.getDbType();
                    String targetDbType = targetConnProps.getDbType();

                    if (sourceDbType == null || sourceDbType.isEmpty()) {
                        logger.error("Task '{}': dbType not configured for source connection ID '{}'. Skipping task.", task.getName(), task.getSourceConnectionId());
                        continue;
                    }
                    if (targetDbType == null || targetDbType.isEmpty()) {
                        logger.error("Task '{}': dbType not configured for target connection ID '{}'. Skipping task.", task.getName(), task.getTargetConnectionId());
                        continue;
                    }

                    SqlSessionFactory sourceFactory = databaseConfig.getSessionFactory(task.getSourceConnectionId());
                    SqlSessionFactory targetFactory = databaseConfig.getSessionFactory(task.getTargetConnectionId());

                    logger.info("Task '{}': Source DB Type: {}, Target DB Type: {}", task.getName(), sourceDbType, targetDbType);
                    logger.info("Task '{}': Source Schema: {}, Target Schema: {}", task.getName(), task.getSourceSchemaName(), task.getTargetSchemaName());
                    logger.info("Task '{}': Tables to sync: {}", task.getName(), task.getTables());

                    DatabaseSyncService syncService = new DatabaseSyncService(
                            sourceFactory,
                            targetFactory,
                            task.isTruncateBeforeSync(),
                            typeMappingRegistry,
                            sourceDbType,
                            targetDbType,
                            task.getTargetSchemaName(),
                            progressManager // Pass ProgressManager
                    );

                    // Call the modified syncDatabase method, passing task name as taskId
                    syncService.syncDatabase(task.getName(), task.getTables(), task.getSourceSchemaName());
                    
                    // Check task status for test reporting
                    if (task.getName().startsWith("Test")) {
                        TaskProgress finalProgress = progressManager.getTaskProgress(task.getName());
                        if (finalProgress != null && 
                            (finalProgress.getStatus() == TaskStatus.COMPLETED_SUCCESS || 
                             (finalProgress.getStatus() == TaskStatus.COMPLETED_WITH_ERRORS && finalProgress.getTablesFailed() == 0) )) { // Consider success if only some tables had no data but no actual errors
                            logger.info("--- Test Task [{}] PASSED ---", task.getName());
                        } else {
                            logger.error("--- Test Task [{}] FAILED. Final status: {} ---", task.getName(), finalProgress != null ? finalProgress.getStatus() : "UNKNOWN");
                        }
                    } else {
                         logger.info("Task '{}' completed processing.", task.getName());
                    }

                } catch (Exception e) {
                    logger.error("Error executing task '{}': {}", task.getName(), e.getMessage(), e);
                    if (task.getName().startsWith("Test")) {
                         logger.error("--- Test Task [{}] FAILED due to exception. ---", task.getName());
                    }
                    // Decide if one task failure should stop all, or continue with next.
                    // Current loop structure will continue with the next task.
                }
                logger.info("---------------------------------------------------------");
            }
            logger.info("All synchronization tasks processed.");
        };
    }
}
