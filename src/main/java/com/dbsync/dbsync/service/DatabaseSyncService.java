package com.dbsync.dbsync.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dbsync.dbsync.mapper.TableMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseSyncService {
import com.dbsync.dbsync.typemapping.TypeMappingRegistry;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseSyncService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSyncService.class);
    private final SqlSessionFactory sourceFactory;
    private final SqlSessionFactory targetFactory;
    private final boolean truncateBeforeSync;
    private final TypeMappingRegistry typeMappingRegistry;
import com.dbsync.dbsync.progress.ProgressManager; // Added import
import javax.sql.DataSource; // Added import

public class DatabaseSyncService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSyncService.class);
    private final SqlSessionFactory sourceFactory;
    private final SqlSessionFactory targetFactory;
    private final boolean truncateBeforeSync;
    private final TypeMappingRegistry typeMappingRegistry;
    private final String sourceDbType;
    private final String targetDbType;
    private final String targetSchemaName;
    private final ProgressManager progressManager; // Added ProgressManager


    public DatabaseSyncService(SqlSessionFactory sourceFactory, SqlSessionFactory targetFactory,
                               boolean truncateBeforeSync, TypeMappingRegistry typeMappingRegistry,
                               String sourceDbType, String targetDbType, String targetSchemaName,
                               ProgressManager progressManager) { // Added ProgressManager
        this.sourceFactory = sourceFactory;
        this.targetFactory = targetFactory;
        this.truncateBeforeSync = truncateBeforeSync;
        this.typeMappingRegistry = typeMappingRegistry;
        this.sourceDbType = sourceDbType;
        this.targetDbType = targetDbType;
        this.targetSchemaName = targetSchemaName;
        this.progressManager = progressManager; // Store ProgressManager
    }

    /**
     * Synchronizes a list of specified tables for a given task.
     * @param taskId The unique ID for this synchronization task.
     * @param tablesToSync List of table names to synchronize.
     * @param sourceSchemaName The schema name for the source database.
     */
    public void syncDatabase(String taskId, List<String> tablesToSync, String sourceSchemaName) {
        if (tablesToSync == null || tablesToSync.isEmpty()) {
            logger.info("Task [{}]: No tables specified for synchronization. Skipping.", taskId);
            this.progressManager.startTask(taskId, 0); // Start task even if no tables, to mark it
            this.progressManager.completeTask(taskId); // Immediately complete
            return;
        }

        this.progressManager.startTask(taskId, tablesToSync.size());
        boolean allTablesSuccess = true;

        try (SqlSession sourceSession = sourceFactory.openSession();
             SqlSession targetSession = targetFactory.openSession()) {

            logger.info("Task [{}]: Starting synchronization for {} tables from source schema '{}'", taskId, tablesToSync.size(), sourceSchemaName);

            for (String tableName : tablesToSync) {
                String tableComment = null; // Placeholder
                // Potentially fetch table comment here if needed for DDL
                // ... 

                try {
                    syncTable(taskId, sourceSession, targetSession, tableName, sourceSchemaName, tableComment);
                    targetSession.commit(); // Commit after each table successfully synced
                    // progressManager.completeTableSync already logs success
                } catch (Exception e) {
                    targetSession.rollback(); // Rollback for the current table
                    allTablesSuccess = false; 
                    // progressManager.completeTableSync (with failure) is called in syncTable's finally block
                    logger.error("Task [{}]: Failed to synchronize table {}. Error: {}", taskId, tableName, e.getMessage());
                    // Continue with the next table
                }
            }
        } catch (Exception e) {
            logger.error("Task [{}]: Error during database synchronization session: {}", taskId, e.getMessage(), e);
            allTablesSuccess = false; // Mark task as failed if session setup fails
        } finally {
            this.progressManager.completeTask(taskId); // Final task status determined by table statuses
        }
    }

    private void syncTable(String taskId, SqlSession sourceSession, SqlSession targetSession,
                           String tableName, String sourceSchemaName, String tableComment) throws Exception {
        TableMapper sourceMapper = sourceSession.getMapper(TableMapper.class);
        TableMapper targetMapper = targetSession.getMapper(TableMapper.class);
        
        long sourceRecordCount = 0;
        boolean tableStructureCreatedOrExisted = false;
        String failureReason = null;

        try {
            sourceRecordCount = sourceMapper.getTableCount(this.sourceDbType, tableName, sourceSchemaName);
            this.progressManager.startTableSync(taskId, tableName, sourceRecordCount);

            String targetTableNameForCheck = tableName.toLowerCase();
            boolean tableExistsInTarget;
            try {
                List<Map<String, Object>> targetStructure = targetMapper.getTableStructure(this.targetDbType, tableName, this.targetSchemaName);
                tableExistsInTarget = (targetStructure != null && !targetStructure.isEmpty());
                 if(!tableExistsInTarget && this.targetDbType.equals("postgresql")){
                     tableExistsInTarget = targetMapper.checkPgTableExists(targetTableNameForCheck) > 0;
                 }
            } catch (Exception e) {
                logger.warn("Task [{}], Table [{}]: Could not reliably check if target table exists, assuming it does not. Error: {}", taskId, tableName, e.getMessage());
                tableExistsInTarget = false;
            }

            if (!tableExistsInTarget) {
                logger.info("Task [{}], Table [{}]: Does not exist in target, creating structure (source schema: {}).", taskId, tableName, sourceSchemaName);
                List<Map<String, Object>> sourceStructure = sourceMapper.getTableStructure(this.sourceDbType, tableName, sourceSchemaName);
                if (sourceStructure == null || sourceStructure.isEmpty()) {
                    throw new Exception("No structure found for source table " + sourceSchemaName + "." + tableName + ". Cannot create target table.");
                }
                List<Map<String, String>> sourceColumnComments = sourceMapper.getColumnComments(this.sourceDbType, tableName, sourceSchemaName);
                
                // Fetch table comment if not provided (and if needed for DDL)
                // ... (logic for tableComment fetching if necessary) ...

                String createTableSql = generateCreateTableSql(tableName, sourceStructure, tableComment, sourceColumnComments);
                logger.debug("Task [{}], Table [{}]: Create table SQL: {}", taskId, tableName, createTableSql);
                targetMapper.executeDDL(createTableSql);

                // Add comments if applicable (PostgreSQL example)
                if (this.targetDbType.equals("postgresql")) { // Example, make this more generic
                    if (tableComment != null && !tableComment.isEmpty()) {
                        targetMapper.executeDDL(String.format("COMMENT ON TABLE %s IS '%s'", targetTableNameForCheck, tableComment.replace("'", "''")));
                    }
                    for (Map<String, String> comment : sourceColumnComments) {
                        if (comment.get("COMMENTS") != null && !comment.get("COMMENTS").isEmpty()) {
                            targetMapper.executeDDL(String.format("COMMENT ON COLUMN %s.%s IS '%s'", targetTableNameForCheck, comment.get("COLUMN_NAME").toLowerCase(), comment.get("COMMENTS").replace("'", "''")));
                        }
                    }
                }
                logger.info("Task [{}], Table [{}]: Structure created.", taskId, tableName);
            } else if (truncateBeforeSync) {
                logger.info("Task [{}], Table [{}]: Exists in target, truncating data before sync.", taskId, tableName);
                targetMapper.truncateTable(targetTableNameForCheck); // Assuming TRUNCATE is generally cross-DB or mapper handles it
            }
            tableStructureCreatedOrExisted = true;

            // Sync data
            if (sourceRecordCount > 0) {
                 syncTableData(taskId, sourceSession, targetSession, tableName, sourceSchemaName);
            } else {
                logger.info("Task [{}], Table [{}]: No records to sync from source.", taskId, tableName);
            }
            
        } catch (Exception e) {
            failureReason = e.getMessage();
            logger.error("Task [{}], Table [{}]: Error during table synchronization process: {}", taskId, tableName, failureReason, e);
            throw e; // Rethrow to be caught by syncDatabase loop
        } finally {
            // If startTableSync was called (i.e., sourceRecordCount was fetched), then complete it.
            if (progressManager.getTaskProgress(taskId) != null && 
                progressManager.getTaskProgress(taskId).getTableProgress(tableName) != null &&
                progressManager.getTaskProgress(taskId).getTableProgress(tableName).getStartTime() != null) {
                
                boolean successStatus = (failureReason == null) && tableStructureCreatedOrExisted;
                 // If data sync started, its success is determined by exceptions during syncTableData.
                 // If structure creation failed before data sync, it's a failure.
                 // If sourceRecordCount was 0, it's success if structure part was okay.
                if (sourceRecordCount == 0 && !tableStructureCreatedOrExisted && failureReason == null) {
                    // Case where table structure couldn't be fetched from source (already logged and thrown by getTableStructure)
                    // This might result in failureReason being set if getTableStructure throws
                }

                this.progressManager.completeTableSync(taskId, tableName, successStatus, failureReason);
            }
        }
    }

    private String generateCreateTableSql(String tableName, List<Map<String, Object>> structure,
                                          String tableComment, List<Map<String, String>> columnComments) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(tableName.toLowerCase()).append(" (\n");

        try {
            for (int i = 0; i < structure.size(); i++) {
                Map<String, Object> column = structure.get(i);
                String columnName = ((String) column.get("COLUMN_NAME")).toLowerCase();
                String sourceDataType = (String) column.get("DATA_TYPE");

                Number length = (Number) column.get("DATA_LENGTH");
                Number precision = (Number) column.get("DATA_PRECISION");
                Number scale = (Number) column.get("DATA_SCALE");

                Integer dataLengthInt = (length != null) ? length.intValue() : null;
                Integer dataPrecisionInt = (precision != null) ? precision.intValue() : null;
                Integer dataScaleInt = (scale != null) ? scale.intValue() : null;
                
                Integer columnSizeForMapper = dataLengthInt;
                Integer decimalDigitsForMapper = dataScaleInt;

                // Heuristic: For numeric types, "size" is often precision, not length.
                // Individual mappers might have more specific logic for their source DB.
                if (sourceDataType != null) {
                    String upperSourceDataType = sourceDataType.toUpperCase();
                    if (upperSourceDataType.contains("NUMBER") || 
                        upperSourceDataType.contains("DECIMAL") || 
                        upperSourceDataType.contains("NUMERIC") ||
                        upperSourceDataType.contains("FLOAT") || // Oracle FLOAT(binary_precision) uses precision for size
                        upperSourceDataType.contains("DOUBLE") ||
                        upperSourceDataType.contains("MONEY")) { // SQL Server money types
                        columnSizeForMapper = dataPrecisionInt;
                    }
                     // For types like VARCHAR(n), CHAR(n), DATA_LENGTH is usually the correct size.
                     // For types like TIME(p), TIMESTAMP(p), DATA_SCALE or a specific attribute might hold 'p'.
                     // The current structure map (DATA_LENGTH, DATA_PRECISION, DATA_SCALE) is generic.
                     // Mappers should be robust enough or this part might need DB-specific pre-processing.
                     // For example, SQL Server's TIME(p) might put 'p' in DATA_SCALE.
                    if (upperSourceDataType.startsWith("TIME") && dataScaleInt != null) { // e.g. SQL Server TIME(p)
                        // Here, decimalDigitsForMapper is already dataScaleInt.
                        // columnSizeForMapper for TIME(p) is not typically its length, but its precision.
                        // The TypeMapper interface uses columnSize and decimalDigits.
                        // For TIME(p), we might pass 'p' as decimalDigits or columnSize depending on mapper convention.
                        // Let's assume for now that mappers expecting precision for TIME/TIMESTAMP will check decimalDigits.
                    }
                }


                String targetDataType = this.typeMappingRegistry.mapType(
                        sourceDataType,
                        columnSizeForMapper,
                        decimalDigitsForMapper,
                        this.sourceDbType,
                        this.targetDbType
                );

                sql.append("    ").append(columnName).append(" ").append(targetDataType);

                // 处理非空约束
                if ("N".equals(column.get("NULLABLE"))) {
                    sql.append(" NOT NULL");
                }

                // 如果不是最后一个字段，添加逗号
                if (i < structure.size() - 1) {
                    sql.append(",");
                }
                sql.append("\n");
            }

            sql.append(")");

            logger.debug("生成的建表SQL: {}", sql.toString());
            return sql.toString();

        } catch (Exception e) {
            logger.error("生成建表SQL失败: {}", e.getMessage());
            throw new RuntimeException("生成建表SQL失败", e);
        }
    }

    private void syncTableData(String taskId, SqlSession sourceSession, SqlSession targetSession, String tableName, String sourceSchemaName) {
        try {
            TableMapper sourceMapper = sourceSession.getMapper(TableMapper.class);
            // TableMapper targetMapper = targetSession.getMapper(TableMapper.class); // Not directly used for inserts via JdbcTemplate

            long totalCount = sourceMapper.getTableCount(this.sourceDbType, tableName, sourceSchemaName);
            logger.info("Task [{}], Table [{}]: Total records to sync from source: {}", taskId, tableName, totalCount);

            // Inform ProgressManager about the total records for this table
            // This was moved to syncTable's startTableSync call.
            // this.progressManager.startTableSync(taskId, tableName, totalCount); // Already called in syncTable

            long batchSize = 1000; // Configurable batch size
            long processedCount = 0;
            boolean tableSyncSuccess = true;

            Page<Map<String, Object>> page = new Page<>(1, batchSize);

            while (processedCount < totalCount) {
                Map<String, Object> paginationParams = new HashMap<>();
                paginationParams.put("dbType", this.sourceDbType);
                paginationParams.put("tableName", tableName);
                paginationParams.put("schemaName", sourceSchemaName);
                paginationParams.put("current", page.getCurrent());
                paginationParams.put("size", batchSize);
                // TODO: Handle orderByColumn for SQL Server if necessary for stable pagination
                // String orderByColumn = determineOrderByColumnForTable(tableName); // pseudo-code
                // paginationParams.put("orderByColumn", orderByColumn);


                List<Map<String, Object>> batchData = sourceMapper.getTableDataWithPagination(paginationParams);

                if (batchData.isEmpty()) {
                    if (processedCount < totalCount) {
                         logger.warn("Task [{}], Table [{}]: Expected more data (processed {} of {}), but batch was empty. Pagination might be inconsistent or source data changed.",
                                taskId, tableName, processedCount, totalCount);
                    }
                    break; // No more data
                }

                try {
                    int rowsAffectedInBatch = executeAndReportBatchInsert(taskId, tableName, tableName.toLowerCase(), batchData);
                    // executeAndReportBatchInsert already calls progressManager.updateTableProgress
                    // processedCount is now tracked by progressManager via updateTableProgress calls
                    processedCount += rowsAffectedInBatch; // Keep a local count for loop termination, or rely on progressManager's value
                } catch (Exception e) {
                    // executeAndReportBatchInsert already logs and calls completeTableSync with failure
                    // So, we just rethrow or mark this table sync as failed and break.
                    tableSyncSuccess = false; // Mark as failed, completeTableSync will be called in syncTable's finally
                    logger.error("Task [{}], Table [{}]: Data batch processing failed. Error: {}", taskId, tableName, e.getMessage());
                    throw e; // Rethrow to be caught by syncTable's catch block
                }

                if (batchData.size() < batchSize) {
                    // Last page was smaller than batchSize, means we are done.
                    if(processedCount < totalCount && page.getCurrent()*batchSize > totalCount) {
                        //This is expected if the last page is not full
                    } else if (processedCount < totalCount) {
                         logger.warn("Task [{}], Table [{}]: Processed count {} is less than total count {} after processing a partial batch. Data might have changed during sync.",
                                taskId, tableName, processedCount, totalCount);
                    }
                    break; 
                }
                page.setCurrent(page.getCurrent() + 1);
            }
            
            // Final check on processed count if relying on local processedCount
            // TableSyncProgress currentTableProgress = this.progressManager.getTaskProgress(taskId).getTableProgress(tableName);
            // if (currentTableProgress.getRecordsProcessed() < totalCount && tableSyncSuccess) {
            //     logger.warn("Task [{}], Table [{}]: Final processed count {} is less than total source count {}. Potential data inconsistency or issue with pagination/source data.",
            //             taskId, tableName, currentTableProgress.getRecordsProcessed(), totalCount);
            // }


        } catch (Exception e) {
            logger.error("Task [{}], Table [{}]: Error during data synchronization. Error: {}", taskId, tableName, e.getMessage(), e);
            // Ensure progressManager is updated about failure if not already handled by executeAndReportBatchInsert
            // This is now handled by syncTable's finally block.
            throw new RuntimeException("Data synchronization failed for table " + tableName, e);
        }
    }

    private int executeAndReportBatchInsert(String taskId, String sourceTableNameForProgress, // Typically same as targetTableName
                                             String targetTableName, List<Map<String, Object>> batchData) {
        if (batchData.isEmpty()) {
            return 0;
        }
        // Use the targetFactory's DataSource for the JdbcTemplate
        DataSource targetDataSource = targetFactory.getConfiguration().getEnvironment().getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(targetDataSource);

        // Prepare SQL and column list from the first data row
        Map<String, Object> firstRow = batchData.get(0);
        List<String> columns = firstRow.keySet().stream()
                .filter(column -> !column.equalsIgnoreCase("rnum") && !column.equalsIgnoreCase("rownum"))
                .collect(Collectors.toList());

        if (columns.isEmpty()) {
            logger.warn("Task [{}], Table [{}]: No columns found for batch insert. Skipping batch.", taskId, sourceTableNameForProgress);
            return 0;
        }
        
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(targetTableName).append(" (");
        sqlBuilder.append(String.join(", ", columns.stream().map(String::toLowerCase).collect(Collectors.toList())));
        sqlBuilder.append(") VALUES (");
        sqlBuilder.append(columns.stream().map(c -> "?").collect(Collectors.joining(",")));
        sqlBuilder.append(")");
        
        String sql = sqlBuilder.toString();

        try {
            int[] rowsAffectedArray = jdbcTemplate.batchUpdate(sql, batchData, batchData.size(),
                (ps, argument) -> {
                    for (int i = 0; i < columns.size(); i++) {
                        ps.setObject(i + 1, argument.get(columns.get(i)));
                    }
                });

            int totalRowsAffected = 0;
            for (int rows : rowsAffectedArray) {
                // For batchUpdate, JDBC drivers might return Statement.SUCCESS_NO_INFO (-2)
                // or Statement.EXECUTE_FAILED (-3). We count actual affected rows.
                if (rows > 0) {
                    totalRowsAffected += rows;
                } else if (rows == Statement.SUCCESS_NO_INFO) {
                    // If driver returns SUCCESS_NO_INFO, we can assume the command was successful for one row.
                    // This is an approximation if precise count isn't available.
                    // Or, for more accuracy, one might need to verify this behavior with the specific JDBC driver.
                    // For simplicity here, let's assume each command in the batch affected one row if SUCCESS_NO_INFO.
                    // A more conservative approach would be to not count it or count as 1.
                    // Let's count it as 1 for progress reporting, assuming success.
                    totalRowsAffected += 1; 
                }
            }
            
            if (totalRowsAffected > 0) {
                 this.progressManager.updateTableProgress(taskId, sourceTableNameForProgress, totalRowsAffected);
            } else if (batchData.size() > 0 && totalRowsAffected == 0 && 
                       rowsAffectedArray.length > 0 && rowsAffectedArray[0] == Statement.SUCCESS_NO_INFO) {
                // If all results are SUCCESS_NO_INFO, it means commands were sent.
                // Let's assume this means all rows in the batch were processed.
                this.progressManager.updateTableProgress(taskId, sourceTableNameForProgress, batchData.size());
                totalRowsAffected = batchData.size(); // For return value consistency
            }


            logger.debug("Task [{}], Table [{}->{}]: Batch insert executed. SQL: [{}], Batch size: {}, Rows affected reported by driver: {}",
                         taskId, sourceTableNameForProgress, targetTableName, sql, batchData.size(), totalRowsAffected);
            return totalRowsAffected;

        } catch (Exception e) {
            logger.error("Task [{}], Table [{}->{}]: Error during batch insert. SQL: [{}], Error: {}",
                         taskId, sourceTableNameForProgress, targetTableName, sql, e.getMessage(), e);
            // Optionally rethrow or handle more gracefully, perhaps marking table as failed
            this.progressManager.completeTableSync(taskId, sourceTableNameForProgress, false, "Batch insert failed: " + e.getMessage());
            throw new RuntimeException("Batch insert failed for table " + sourceTableNameForProgress, e);
        }
    }

    // The method generateBatchInsertSql_OLD was a remnant and is now removed.
//        if (data.isEmpty()) {
//            return "";
//        }
//
//        StringBuilder sql = new StringBuilder();
//        sql.append("INSERT INTO ").append(tableName).append(" (");
//
//        // 获取列名
//        Map<String, Object> firstRow = data.get(0);
//        List<String> columns = new ArrayList<>(firstRow.keySet());
//
//        // 添加列名
//        sql.append(String.join(", ", columns.stream()
//                .map(String::toLowerCase)
//                .collect(Collectors.toList())));
//
//        sql.append(") VALUES ");
//
//        // 添加值
//        for (int i = 0; i < data.size(); i++) {
//            Map<String, Object> row = data.get(i);
//            sql.append("(");
//
//            for (int j = 0; j < columns.size(); j++) {
//                Object value = row.get(columns.get(j));
//                if (value == null) {
//                    sql.append("NULL");
//                } else if (value instanceof String) {
//                    sql.append("'").append(value).append("'");
//                } else if (value instanceof Date) {
//                    sql.append("'").append(new Timestamp(((Date) value).getTime())).append("'");
//                } else if (value instanceof Timestamp) {
//                    sql.append("'").append(value).append("'");
//                } else if (value instanceof Boolean) {
//                    sql.append(((Boolean) value) ? "true" : "false");
//                } else {
//                    sql.append(value);
//                }
//
//                if (j < columns.size() - 1) {
//                    sql.append(", ");
//                }
//            }
//
//            sql.append(")");
//            if (i < data.size() - 1) {
//                sql.append(", ");
//            }
//        }
//
//        return sql.toString();
//    }
}