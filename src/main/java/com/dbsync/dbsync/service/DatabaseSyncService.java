package com.dbsync.dbsync.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dbsync.dbsync.mapper.TableMapper;
import com.dbsync.dbsync.progress.ProgressManager;
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

import com.dbsync.dbsync.typemapping.TypeMappingRegistry;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.*;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
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
     *
     * @param taskId           The unique ID for this synchronization task.
     * @param tablesToSync     List of table names to synchronize.
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
                if (!tableExistsInTarget && this.targetDbType.equals("postgresql")) {
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

                // TODO:增加了两倍字段的长度
                Integer dataLengthInt = (length != null) ? length.intValue()*2 : null;
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

            logger.debug("生成的建表 SQL: {}", sql.toString());
            return sql.toString();

        } catch (Exception e) {
            logger.error("生成建表 SQL 失败：{}", e.getMessage());
            throw new RuntimeException("生成建表 SQL 失败", e);
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
                    // Pass the class field targetFactory to the refactored method
                    int rowsAffectedInBatch = executeAndReportBatchInsert(taskId, tableName, tableName.toLowerCase(), batchData, this.targetFactory);
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
                    if (processedCount < totalCount && page.getCurrent() * batchSize > totalCount) {
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

    private int executeAndReportBatchInsert(String taskId, String progressIdentifier,
                                            String targetTableName, List<Map<String, Object>> batchData,
                                            SqlSessionFactory currentTargetFactory) throws Exception {
        if (batchData == null || batchData.isEmpty()) {
            return 0;
        }
        DataSource targetDataSource = currentTargetFactory.getConfiguration().getEnvironment().getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(targetDataSource);

        Map<String, Object> firstRow = batchData.get(0);
        List<String> finalColumns = firstRow.keySet().stream()
                .filter(column -> !column.equalsIgnoreCase("rnum") && !column.equalsIgnoreCase("rownum"))
                .filter(column -> !column.equalsIgnoreCase("rnum_")) // 过滤掉 rnum_
                .filter(column -> !column.equalsIgnoreCase("rn")) // 过滤掉 rn
                .collect(Collectors.toList());

        if (finalColumns.isEmpty()) {
            logger.warn("Task [{}], ProgressID [{}]: No columns found for batch insert. Skipping batch.",
                    taskId, progressIdentifier);
            return 0;
        }
//        List<String> filteredColumns = columns.stream()
//                .filter(column -> !column.equalsIgnoreCase("rnum_")) // 过滤掉 rnum_
//                .filter(column -> !column.equalsIgnoreCase("rn")) // 过滤掉 rn
//                .collect(Collectors.toList());
//        columns = filteredColumns;
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(targetTableName).append(" (");
        sqlBuilder.append(String.join(", ", finalColumns.stream().map(String::toLowerCase).collect(Collectors.toList())));
        sqlBuilder.append(") VALUES (");
        sqlBuilder.append(finalColumns.stream().map(c -> "?").collect(Collectors.joining(",")));
        sqlBuilder.append(")");

        String sql = sqlBuilder.toString();

        try {

            int[][] rowsAffectedArray = jdbcTemplate.batchUpdate(sql, batchData, batchData.size(),
                    (ps, argument) -> {
                        for (int i = 0; i < finalColumns.size(); i++) {
                            ps.setObject(i + 1, argument.get(finalColumns.get(i)));
                        }
                    });

            int totalRowsAffected = 0;
            for (int[] batchResults : rowsAffectedArray) {
                for (int rows : batchResults) {
                    if (rows > 0) {
                        totalRowsAffected += rows;
                    } else if (rows == Statement.SUCCESS_NO_INFO) {
                        totalRowsAffected += 1; // Count SUCCESS_NO_INFO as one row affected for progress tracking
                    }
                }
            }

            if (totalRowsAffected > 0) {
                this.progressManager.updateTableProgress(taskId, progressIdentifier, totalRowsAffected);
            } else if (batchData.size() > 0 && totalRowsAffected == 0 &&
                    rowsAffectedArray.length > 0 && rowsAffectedArray[0][0] == Statement.SUCCESS_NO_INFO) {
                this.progressManager.updateTableProgress(taskId, progressIdentifier, batchData.size());
                totalRowsAffected = batchData.size();
            }

            logger.debug("Task [{}], ProgressID [{}], Table [{}]: Batch insert executed. SQL: [{}], Batch size: {}, Rows affected: {}",
                    taskId, progressIdentifier, targetTableName, sql, batchData.size(), totalRowsAffected);
            return totalRowsAffected;

        } catch (Exception e) {
            logger.error("Task [{}], ProgressID [{}], Table [{}]: Error during batch insert. SQL: [{}], Error: {}",
                    taskId, progressIdentifier, targetTableName, sql, e.getMessage(), e);
            // Removed completeTableSync, caller will handle it.
            throw e; // Rethrow the exception
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

    private SqlSessionFactory createSqlSessionFactory(Map<String, String> connectionDetails) throws Exception {
        String url = connectionDetails.get("url");
        String username = connectionDetails.get("username");
        String password = connectionDetails.get("password");
        String driverClassName = connectionDetails.get("driverClassName");

        if (driverClassName == null || driverClassName.isEmpty()) {
            throw new IllegalArgumentException("Driver class name is required.");
        }
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("Database URL is required.");
        }
        // Username and password can be optional depending on DB configuration

        Class.forName(driverClassName);
        DataSource dataSource = new UnpooledDataSource(
                driverClassName,
                url,
                username,
                password
        );

        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("customDbEnv-" + driverClassName, transactionFactory, dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(TableMapper.class); // Assuming TableMapper might be needed
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    public void executeCustomQueryAndSaveResults(
            String taskId,
            Map<String, String> sourceConnectionDetails,
            Map<String, String> targetConnectionDetails,
            String customSql,
            String targetTableName,
            String targetSchemaName
    ) throws Exception {
        // Initial Parameter Validations
        if (customSql == null || customSql.trim().isEmpty()) {
            logger.error("Task [{}]: Custom SQL query is missing or empty.", taskId);
            throw new IllegalArgumentException("Custom SQL query is missing or empty.");
        }
        if (targetTableName == null || targetTableName.trim().isEmpty()) {
            logger.error("Task [{}]: Target table name is missing or empty.", taskId);
            throw new IllegalArgumentException("Target table name is missing or empty.");
        }
        if (sourceConnectionDetails == null) {
            logger.error("Task [{}]: Source connection details are missing.", taskId);
            throw new IllegalArgumentException("Source connection details are missing.");
        }
        if (targetConnectionDetails == null) {
            logger.error("Task [{}]: Target connection details are missing.", taskId);
            throw new IllegalArgumentException("Target connection details are missing.");
        }
        // Essential keys (dbType already checked later, url/driverClassName checked in createSqlSessionFactory)
        // but good to have an early check for dbType as it's used before createSqlSessionFactory for source.
        if (sourceConnectionDetails.get("dbType") == null || sourceConnectionDetails.get("dbType").trim().isEmpty()){
            logger.error("Task [{}]: Source database type ('dbType') not provided in sourceConnectionDetails.", taskId);
            throw new IllegalArgumentException("Source database type ('dbType') is required in sourceConnectionDetails.");
        }
         if (targetConnectionDetails.get("dbType") == null || targetConnectionDetails.get("dbType").trim().isEmpty()){
            logger.error("Task [{}]: Target database type ('dbType') not provided in targetConnectionDetails.", taskId);
            throw new IllegalArgumentException("Target database type ('dbType') is required in targetConnectionDetails.");
        }


        this.progressManager.startTask(taskId, 1); // One overall operation: custom query execution and save

        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> columnDetailsList = new ArrayList<>();
        List<String> resultColumnNames = new ArrayList<>();
        SqlSessionFactory customSourceFactory = null;
        SqlSessionFactory customTargetFactory = null;

        try {
            logger.info("Task [{}]: Starting custom query execution against source.", taskId);
            customSourceFactory = createSqlSessionFactory(sourceConnectionDetails);

            try (SqlSession sqlSession = customSourceFactory.openSession();
                 Connection connection = sqlSession.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(customSql)) {

                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                logger.info("Task [{}]: Custom query returned {} columns.", taskId, columnCount);

                for (int i = 1; i <= columnCount; i++) {
                    String colName = metaData.getColumnName(i);
                    resultColumnNames.add(colName);

                    Map<String, Object> colDetail = new HashMap<>();
                    colDetail.put("COLUMN_NAME", colName);
                    colDetail.put("TYPE_NAME", metaData.getColumnTypeName(i));
                    colDetail.put("PRECISION", metaData.getPrecision(i));
                    colDetail.put("SCALE", metaData.getScale(i));
                    colDetail.put("IS_NULLABLE", metaData.isNullable(i) != ResultSetMetaData.columnNoNulls);
                    columnDetailsList.add(colDetail);
                }

                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (String columnName : resultColumnNames) {
                        row.put(columnName, resultSet.getObject(columnName));
                    }
                    results.add(row);
                }
                logger.info("Task [{}]: Fetched {} rows from custom query.", taskId, results.size());
                if (!results.isEmpty()) {
                    logger.debug("Task [{}]: First row of custom query result: {}", taskId, results.get(0));
                }
            } catch (SQLException e) {
                logger.error("Task [{}]: SQL error during custom query execution or data fetching: {}", taskId, e.getMessage(), e);
                throw new Exception("SQL error during custom query: " + e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Task [{}]: Error during source custom query execution: {}", taskId, e.getMessage(), e);
                throw e;
            }

            // --- Target Database Operations ---
            logger.info("Task [{}]: Starting target database operations for table '{}'.", taskId, targetTableName);
            String sourceDbTypeFromDetails = sourceConnectionDetails.get("dbType");
            if (sourceDbTypeFromDetails == null || sourceDbTypeFromDetails.isEmpty()) {
                logger.error("Task [{}]: Source database type ('dbType') not provided in sourceConnectionDetails.", taskId);
                throw new IllegalArgumentException("Source database type ('dbType') is required in sourceConnectionDetails.");
            }
            String targetDbTypeFromDetails = targetConnectionDetails.get("dbType");
            if (targetDbTypeFromDetails == null || targetDbTypeFromDetails.isEmpty()) {
                logger.error("Task [{}]: Target database type ('dbType') not provided in targetConnectionDetails.", taskId);
                throw new IllegalArgumentException("Target database type ('dbType') is required in targetConnectionDetails.");
            }

            customTargetFactory = createSqlSessionFactory(targetConnectionDetails);
            try (SqlSession targetSqlSession = customTargetFactory.openSession(false)) { // Auto-commit false
                TableMapper targetMapper = targetSqlSession.getMapper(TableMapper.class);
                boolean tableExistsInTarget;
            try {
                List<Map<String, Object>> targetStructure = targetMapper.getTableStructure(targetDbTypeFromDetails, targetTableName, targetSchemaName);
                tableExistsInTarget = (targetStructure != null && !targetStructure.isEmpty());
                logger.info("Task [{}]: Checked for table '{}' in schema '{}' in target DB ({}). Exists: {}", taskId, targetTableName, targetSchemaName, targetDbTypeFromDetails, tableExistsInTarget);
            } catch (Exception e) {
                    logger.warn("Task [{}], Table [{}]: Could not reliably check if target table exists. Error: {}", taskId, targetTableName, e.getMessage());
                    throw new Exception("Failed to check target table existence for " + targetTableName + ": " + e.getMessage(), e);
                }

                if (!tableExistsInTarget) {
                    logger.info("Task [{}]: Target table '{}' does not exist in schema '{}'. Proceeding with creation.", taskId, targetTableName, targetSchemaName);
                    String createTableSql = generateCreateTableSqlFromColumnDetails(
                            taskId,
                            targetTableName,
                            columnDetailsList,
                            sourceDbTypeFromDetails,
                            targetDbTypeFromDetails,
                            targetSchemaName,
                            this.typeMappingRegistry
                    );
                    logger.info("Task [{}]: Executing DDL for target table {}: {}", taskId, targetTableName, createTableSql);
                    targetMapper.executeDDL(createTableSql);
                    targetSqlSession.commit(); // Commit DDL
                    logger.info("Task [{}]: Table '{}' created successfully.", taskId, targetTableName);
                } else {
                    String errorMessage = String.format("Target table '%s' already exists in schema '%s'. Operation aborted as per policy.", targetTableName, (targetSchemaName == null ? "" : targetSchemaName));
                    logger.error("Task [{}]: {}", taskId, errorMessage);
                    throw new RuntimeException(errorMessage); // Or handle as per specific requirements for existing tables
                }

                // Data Insertion Stage
                String progressIdentifierForTableSync = targetTableName;
                long totalRecordCount = (results == null) ? 0 : results.size();
                this.progressManager.startTableSync(taskId, progressIdentifierForTableSync, totalRecordCount);
                boolean dataSyncSuccessful = false;
                String dataSyncFailureReason = null;

                try {
                    if (results != null && !results.isEmpty()) {
                        logger.info("Task [{}]: Attempting to insert {} rows into target table '{}'.", taskId, results.size(), targetTableName);
                        executeAndReportBatchInsert(taskId, progressIdentifierForTableSync, targetTableName.toLowerCase(), results, customTargetFactory);
                        targetSqlSession.commit(); // Commit data insertion
                        dataSyncSuccessful = true;
                        logger.info("Task [{}]: Data successfully inserted into table '{}' and transaction committed.", taskId, targetTableName);
                    } else {
                        logger.info("Task [{}]: No data fetched from source query to insert into target table '{}'.", taskId, targetTableName);
                        dataSyncSuccessful = true; // No data to insert is still a successful state for this part.
                    }
                } catch (Exception e) {
                    dataSyncFailureReason = e.getMessage();
                    logger.error("Task [{}]: Data insertion failed for table '{}'. Error: {}", taskId, targetTableName, e.getMessage(), e);
                    try {
                        targetSqlSession.rollback();
                        logger.info("Task [{}]: Transaction rolled back for table '{}' due to data insertion failure.", taskId, targetTableName);
                    } catch (Exception rbEx) {
                        logger.error("Task [{}]: Rollback failed for table '{}'. Error: {}", taskId, targetTableName, rbEx.getMessage(), rbEx);
                    }
                    throw e;
                } finally {
                    this.progressManager.completeTableSync(taskId, progressIdentifierForTableSync, dataSyncSuccessful, dataSyncFailureReason);
                }
            } // Target SqlSession try-with-resources ends here
        } catch (Exception e) {
            logger.error("Task [{}]: Overall failure in executeCustomQueryAndSaveResults. Error: {}", taskId, e.getMessage(), e);
            throw e;
        } finally {
            this.progressManager.completeTask(taskId);
        }
    }

    private String generateCreateTableSqlFromColumnDetails(
            String taskId, // For logging
            String tableName,
            List<Map<String, Object>> columnDetailsList,
            String sourceDbTypeForMapping,
            String targetDbTypeForMapping,
            String targetSchemaName, // Optional, for prefixing
            TypeMappingRegistry typeMappingRegistry
    ) throws SQLException {
        StringBuilder sql = new StringBuilder();
        String qualifiedTableName = (targetSchemaName != null && !targetSchemaName.trim().isEmpty())
                ? targetSchemaName.trim() + "." + tableName.toLowerCase()
                : tableName.toLowerCase();

        sql.append("CREATE TABLE ").append(qualifiedTableName).append(" (\n");

        for (int i = 0; i < columnDetailsList.size(); i++) {
            Map<String, Object> colDetail = columnDetailsList.get(i);
            String columnName = ((String) colDetail.get("COLUMN_NAME")).toLowerCase();
            String sourceDataTypeName = (String) colDetail.get("TYPE_NAME");
            Integer precision = (Integer) colDetail.get("PRECISION");
            Integer scale = (Integer) colDetail.get("SCALE");
            boolean isNullable = (Boolean) colDetail.get("IS_NULLABLE");

            String targetDataType = typeMappingRegistry.mapType(
                    sourceDataTypeName,
                    precision,
                    scale,
                    sourceDbTypeForMapping,
                    targetDbTypeForMapping
            );

            sql.append("    ").append(columnName).append(" ").append(targetDataType);
            if (!isNullable) {
                sql.append(" NOT NULL");
            }

            if (i < columnDetailsList.size() - 1) {
                sql.append(",");
            }
            sql.append("\n");
        }
        sql.append(")");
        logger.info("Task [{}]: Generated CREATE TABLE SQL for {}: {}", taskId, qualifiedTableName, sql.toString());
        return sql.toString();
    }
}