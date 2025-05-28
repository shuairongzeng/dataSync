package com.dbsync.dbsync;

import com.dbsync.dbsync.mapper.OracleToPostgresTypeMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class OracleToPostgresSyncWithSchema {
    private static final Logger logger = LoggerFactory.getLogger(OracleToPostgresSyncWithSchema.class);
    private static final int BATCH_SIZE = 1000;
    private static final int MAX_RETRY = 2;
    private static final int THREAD_POOL_SIZE = 4;

    // Oracle 数据库连接信息
    private static final String ORACLE_URL = "jdbc:oracle:thin:@192.168.107.101:1525/orcl";
    private static final String ORACLE_USER = "PT1_ECI_CQDM";
    private static final String ORACLE_PASSWORD = "ecidh.com2024";
    // 支持多表同步
    private static final List<String> ORACLE_TABLES = Arrays.asList("OMS_ORDER");

    // PostgreSQL 数据库连接信息
    private static final String POSTGRES_URL = "jdbc:postgresql://192.168.106.103:5432/pt1_eci_cqdm";
    private static final String POSTGRES_USER = "cqdm_basic";
    private static final String POSTGRES_PASSWORD = "cqdm_basic_1qaz";
    // 目标表名与源表名一致（如需不同可用 Map 配置）
    private static final Map<String, String> TABLE_NAME_MAPPING = new HashMap<>();

    static {
        for (String t : ORACLE_TABLES) {
            TABLE_NAME_MAPPING.put(t, t);
        }
     //  TABLE_NAME_MAPPING.put("OMS_ORDER", "OMS_ORDER_1");
    }

    private static final Map<String, String> DATA_TYPE_MAPPING = OracleToPostgresTypeMapping.getDataTypeMapping();

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Class.forName("org.postgresql.Driver");

            Properties oracleProps = new Properties();
            oracleProps.setProperty("user", ORACLE_USER);
            oracleProps.setProperty("password", ORACLE_PASSWORD);

            Properties postgresProps = new Properties();
            postgresProps.setProperty("user", POSTGRES_USER);
            postgresProps.setProperty("password", POSTGRES_PASSWORD);

            try (Connection oracleConn = DriverManager.getConnection(ORACLE_URL, oracleProps);
                 Connection postgresConn = DriverManager.getConnection(POSTGRES_URL, postgresProps)) {

                // 创建线程池
                ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
                List<Future<Boolean>> futures = new ArrayList<>();

                // 提交同步任务
                for (String srcTable : ORACLE_TABLES) {
                    String destTable = TABLE_NAME_MAPPING.getOrDefault(srcTable, srcTable);
                    futures.add(executor.submit(() -> syncTable(oracleConn, postgresConn, srcTable, destTable)));
                }

                // 等待所有任务完成
                executor.shutdown();
                if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                    logger.error("同步任务超时");
                    executor.shutdownNow();
                }

                // 检查结果
                int successCount = 0;
                for (Future<Boolean> future : futures) {
                    if (future.get()) {
                        successCount++;
                    }
                }
                logger.info("同步完成，成功同步 {} 个表，失败 {} 个表",
                        successCount, ORACLE_TABLES.size() - successCount);

            } catch (Exception e) {
                logger.error("同步过程发生异常", e);
            }
        } catch (ClassNotFoundException e) {
            logger.error("未找到 JDBC 驱动", e);
        }
    }

    private static boolean syncTable(Connection oracleConn, Connection postgresConn,
                                     String srcTable, String destTable) {
        logger.info("开始同步表：{} -> {}", srcTable, destTable);

        // 同步表结构
        boolean schemaOk = false;
        for (int retry = 0; retry < MAX_RETRY && !schemaOk; retry++) {
            try {
                schemaOk = syncTableSchema(oracleConn, postgresConn, srcTable, destTable);
                if (schemaOk) {
                    logger.info("表结构同步成功：{}", srcTable);
                }
            } catch (Exception e) {
                logger.error("表结构同步失败：{}", srcTable, e);
                if (retry + 1 < MAX_RETRY) {
                    logger.info("重试表结构同步：{}", srcTable);
                }
            }
        }

        if (!schemaOk) {
            logger.error("表结构同步失败，跳过数据同步：{}", srcTable);
            return false;
        }

        // 同步数据
        boolean dataOk = false;
        for (int retry = 0; retry < MAX_RETRY && !dataOk; retry++) {
            try {
                syncTableData(oracleConn, postgresConn, srcTable, destTable);
                dataOk = true;
                logger.info("数据同步成功：{}", srcTable);
            } catch (Exception e) {
                logger.error("数据同步失败：{}", srcTable, e);
                if (retry + 1 < MAX_RETRY) {
                    logger.info("重试数据同步：{}", srcTable);
                }
            }
        }

        return dataOk;
    }

    private static boolean syncTableSchema(Connection oracleConn, Connection postgresConn,
                                           String oracleTableName, String postgresTableName) throws SQLException {
        StringBuilder createTableSql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(postgresTableName)
                .append(" (");

        List<String> columnDefinitions = new ArrayList<>();
        Set<String> processedColumns = new HashSet<>(); // 用于检查重复列

        // 使用更精确的查询获取列信息
        String columnQuery = "SELECT column_name, data_type, data_length, data_precision, data_scale, nullable " +
                "FROM user_tab_columns " +
                "WHERE table_name = ? " +
                "ORDER BY column_id";

        try (PreparedStatement pstmt = oracleConn.prepareStatement(columnQuery)) {
            pstmt.setString(1, oracleTableName.toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name");

                    // 检查是否已处理过该列
                    if (processedColumns.contains(columnName)) {
                        logger.warn("跳过重复列：{}", columnName);
                        continue;
                    }
                    processedColumns.add(columnName);

                    String oracleDataTypeName = rs.getString("data_type");
                    int columnSize = rs.getInt("data_length") * 2;
                    int dataPrecision = rs.getInt("data_precision");
                    int dataScale = rs.getInt("data_scale");
                    String isNullable = rs.getString("nullable");

                    String postgresDataType = DATA_TYPE_MAPPING.getOrDefault(oracleDataTypeName.toUpperCase(),
                            oracleDataTypeName.toUpperCase());

                    StringBuilder columnDef = new StringBuilder(columnName).append(" ").append(postgresDataType);

                    // 处理数据类型长度和精度
                    if ("VARCHAR".equalsIgnoreCase(postgresDataType) || "VARCHAR2".equalsIgnoreCase(postgresDataType)) {
                        columnDef.append("(").append(columnSize).append(")");
                    } else if ("NUMERIC".equalsIgnoreCase(postgresDataType) || "NUMBER".equalsIgnoreCase(postgresDataType)) {
                        if (dataPrecision > 0) {
                            if (dataScale > 0) {
                                columnDef.append("(").append(dataPrecision).append(",").append(dataScale).append(")");
                            } else {
                                columnDef.append("(").append(dataPrecision).append(")");
                            }
                        }
                    }

                    if ("N".equalsIgnoreCase(isNullable)) {
                        columnDef.append(" NOT NULL");
                    }

                    columnDefinitions.add(columnDef.toString());
                }
            }
        }

        if (columnDefinitions.isEmpty()) {
            logger.error("在 Oracle 表中未找到任何列信息：{}", oracleTableName);
            return false;
        }

        createTableSql.append(String.join(", ", columnDefinitions)).append(")");

        try (Statement stmt = postgresConn.createStatement()) {
            stmt.executeUpdate(createTableSql.toString());
            logger.info("成功创建表：{}", postgresTableName);
            return true;
        } catch (SQLException e) {
            logger.error("创建 PostgreSQL 表失败：{}", postgresTableName, e);
            logger.error("SQL: {}", createTableSql.toString());
            return false;
        }
    }

    private static void syncTableData(Connection oracleConn, Connection postgresConn,
                                      String oracleTableName, String postgresTableName) throws SQLException {
        postgresConn.setAutoCommit(false);

        // 清除 PostgreSQL 目标表中的现有数据
        try (Statement truncateStmt = postgresConn.createStatement()) {
            try {
                truncateStmt.executeUpdate("TRUNCATE TABLE " + postgresTableName);
                logger.info("PostgreSQL 表 {} 中的现有数据已清除 (使用 TRUNCATE)", postgresTableName);
            } catch (SQLException e) {
                logger.warn("无法使用 TRUNCATE TABLE (可能存在外键约束): {}", e.getMessage());
                logger.info("尝试使用 DELETE FROM 清除数据");
                try (Statement deleteStmt = postgresConn.createStatement()) {
                    deleteStmt.executeUpdate("DELETE FROM " + postgresTableName);
                    logger.info("PostgreSQL 表 {} 中的现有数据已清除 (使用 DELETE FROM)", postgresTableName);
                } catch (SQLException ex) {
                    logger.error("清除 PostgreSQL 表数据失败 (DELETE FROM): {}", ex.getMessage());
                    throw ex;
                }
            }
        }

        List<String> oracleColumns = getTableColumns(oracleConn, oracleTableName);
        String insertSql = buildInsertStatement(postgresTableName, oracleColumns);

        try (PreparedStatement postgresStmt = postgresConn.prepareStatement(insertSql);
             Statement oracleStmt = oracleConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

            oracleStmt.setFetchSize(BATCH_SIZE);
            String selectSql = "SELECT " + String.join(", ", oracleColumns) + " FROM " + oracleTableName;

            try (ResultSet oracleRs = oracleStmt.executeQuery(selectSql)) {
                int rowCount = 0;
                while (oracleRs.next()) {
                    for (int i = 0; i < oracleColumns.size(); i++) {
                        postgresStmt.setObject(i + 1, oracleRs.getObject(i + 1));
                    }
                    postgresStmt.addBatch();
                    rowCount++;

                    if (rowCount % BATCH_SIZE == 0) {
                        postgresStmt.executeBatch();
                        postgresConn.commit();
                        logger.info("已同步 {} 条记录", rowCount);
                    }
                }

                if (rowCount % BATCH_SIZE != 0) {
                    postgresStmt.executeBatch();
                    postgresConn.commit();
                    logger.info("已同步剩余 {} 条记录", rowCount % BATCH_SIZE);
                }

                logger.info("数据同步完成，总共同步 {} 条记录", rowCount);

            } catch (SQLException e) {
                postgresConn.rollback();
                throw e;
            } finally {
                postgresConn.setAutoCommit(true);
            }
        }
    }

    private static List<String> getTableColumns(Connection connection, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT column_name FROM user_tab_columns WHERE table_name = '" + tableName.toUpperCase() + "' ")) {
            while (rs.next()) {
                columns.add(rs.getString("column_name"));
            }
        }
        return columns;
    }

    private static String buildInsertStatement(String tableName, List<String> columns) {
        StringBuilder sb = new StringBuilder("INSERT INTO ")
                .append(tableName)
                .append(" (")
                .append(String.join(", ", columns))
                .append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            sb.append("?");
            if (i < columns.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}