package com.dbsync.dbsync;

import com.dbsync.dbsync.config.DatabaseConfig;
import com.dbsync.dbsync.progress.ProgressManager;
import com.dbsync.dbsync.service.DatabaseSyncService;
import com.dbsync.dbsync.typemapping.TypeMappingRegistry;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@Import(DatabaseConfig.class)
@MapperScan("com.dbsync.dbsync.mapper") // 添加这一行
public class OracleToPostgresSyncRunner {
    private static final Logger logger = LoggerFactory.getLogger(OracleToPostgresSyncRunner.class);

    public static void main(String[] args) {
        SpringApplication.run(OracleToPostgresSyncRunner.class, args);
    }

    @Bean
    @Profile("cli")
    public CommandLineRunner commandLineRunner(
            @Qualifier("oracleSqlSessionFactory") SqlSessionFactory sourceFactory,
            @Qualifier("postgresSqlSessionFactory") SqlSessionFactory targetFactory,
            TypeMappingRegistry typeMappingRegistry,
            ProgressManager progressManager) {
        return args -> {
            try {
                // 配置源数据库和目标数据库
                String sourceDbType = "oracle";
                String targetDbType = "postgresql";
                String sourceSchemaName = "";  // Oracle 源 schema
                String targetSchemaName = "";  // PostgreSQL 目标 schema
                boolean truncateBeforeSync = true;  // 在同步前清空目标表

                // 创建同步服务
                DatabaseSyncService syncService = new DatabaseSyncService(
                        sourceFactory,
                        targetFactory,
                        truncateBeforeSync,
                        typeMappingRegistry,
                        sourceDbType,
                        targetDbType,
                        targetSchemaName,
                        progressManager
                );

                // 要同步的表列表
                List<String> tablesToSync = Arrays.asList(
                    "OMS_ORDER"
                );

                // 执行同步
                String taskId = "oracle-to-postgres-sync-" + System.currentTimeMillis();
                logger.info("Starting synchronization task: {}", taskId);
                logger.info("Source Schema: {}, Target Schema: {}", sourceSchemaName, targetSchemaName);
                logger.info("Tables to sync: {}", tablesToSync);

                syncService.syncDatabase(taskId, tablesToSync, sourceSchemaName);

                logger.info("Synchronization task completed: {}", taskId);
            } catch (Exception e) {
                logger.error("Error during synchronization: {}", e.getMessage(), e);
                throw e;
            }
        };
    }
} 