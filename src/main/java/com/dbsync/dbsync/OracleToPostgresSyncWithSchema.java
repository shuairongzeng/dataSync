package com.dbsync.dbsync;

import com.dbsync.dbsync.mapper.OracleToPostgresTypeMapping;

import java.sql.*;
import java.util.*;

public class OracleToPostgresSyncWithSchema {

    private static final int BATCH_SIZE = 1000;

    // Oracle 数据库连接信息
    private static final String ORACLE_URL = "jdbc:oracle:thin:@192.168.107.101:1525/orcl";
    private static final String ORACLE_USER = "PT1_ECI_CQDM";
    private static final String ORACLE_PASSWORD = "ecidh.com2024";
    private static final String ORACLE_TABLE_NAME = "SYS_DATA_HELP";

    // PostgreSQL 数据库连接信息
    private static final String POSTGRES_URL = "jdbc:postgresql://192.168.106.103:5432/pt1_eci_cqdm";
    private static final String POSTGRES_USER = "cqdm_basic";
    private static final String POSTGRES_PASSWORD = "cqdm_basic_1qaz";
    private static final String POSTGRES_TABLE_NAME = "SYS_DATA_HELP1";

    // Oracle 数据类型到 PostgreSQL 数据类型的映射 (需要根据实际情况完善)
    private static final Map<String, String> DATA_TYPE_MAPPING =OracleToPostgresTypeMapping.getDataTypeMapping();

    // Oracle 数据类型到 PostgreSQL 数据类型的映射 (需要根据实际情况完善)
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

                // 同步表结构
                if (syncTableSchema(oracleConn, postgresConn, ORACLE_TABLE_NAME, POSTGRES_TABLE_NAME)) {
                    System.out.println("表结构同步成功！");

                    // 同步数据
                    syncTableData(oracleConn, postgresConn, ORACLE_TABLE_NAME, POSTGRES_TABLE_NAME);
                } else {
                    System.err.println("表结构同步失败，无法进行数据同步。");
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            System.err.println("未找到 JDBC 驱动: " + e.getMessage());
        }
    }

    private static boolean syncTableSchema(Connection oracleConn, Connection postgresConn, String oracleTableName, String postgresTableName) throws SQLException {
        StringBuilder createTableSql = new StringBuilder("CREATE TABLE IF NOT EXISTS ")
                .append(postgresTableName)
                .append(" (");

        List<String> columnDefinitions = new ArrayList<>();
        DatabaseMetaData metaData = oracleConn.getMetaData();
        ResultSet columnsRs = metaData.getColumns(null, null, oracleTableName.toUpperCase(), null);
{
            while (columnsRs.next()) {
                String columnName = columnsRs.getString("COLUMN_NAME");
                String oracleDataTypeName = columnsRs.getString("TYPE_NAME");
                int columnSize = columnsRs.getInt("COLUMN_SIZE");
                int decimalDigits = columnsRs.getInt("DECIMAL_DIGITS");
                String isNullable = columnsRs.getString("IS_NULLABLE");

                String postgresDataType = DATA_TYPE_MAPPING.getOrDefault(oracleDataTypeName.toUpperCase(), oracleDataTypeName.toUpperCase()); // 默认使用 Oracle 的类型名

                StringBuilder columnDef = new StringBuilder(columnName).append(" ").append(postgresDataType);

                if ("VARCHAR".equalsIgnoreCase(postgresDataType)) {
                    columnDef.append("(").append(columnSize).append(")");
                } else if ("NUMERIC".equalsIgnoreCase(postgresDataType) && decimalDigits > 0) {
                    columnDef.append("(").append(columnSize).append(", ").append(decimalDigits).append(")");
                } else if ("NUMERIC".equalsIgnoreCase(postgresDataType) && columnSize > 0 && decimalDigits == 0) {
                    columnDef.append("(").append(columnSize).append(")");
                }

                if ("NO".equalsIgnoreCase(isNullable)) {
                    columnDef.append(" NOT NULL");
                }

                columnDefinitions.add(columnDef.toString());
            }
        }

        if (columnDefinitions.isEmpty()) {
            System.err.println("在 Oracle 表中未找到任何列信息。");
            return false;
        }

        createTableSql.append(String.join(", ", columnDefinitions)).append(")");

        try (Statement stmt = postgresConn.createStatement()) {
            stmt.executeUpdate(createTableSql.toString());
            return true;
        } catch (SQLException e) {
            System.err.println("创建 PostgreSQL 表失败: " + e.getMessage());
            System.err.println("SQL: " + createTableSql.toString());
            return false;
        }
    }

    private static void syncTableData(Connection oracleConn, Connection postgresConn, String oracleTableName, String postgresTableName) throws SQLException {
        postgresConn.setAutoCommit(false);

        // 清除 PostgreSQL 目标表中的现有数据
        try (Statement truncateStmt = postgresConn.createStatement()) {
            try {
                // 尝试使用 TRUNCATE TABLE (更快，但可能受外键约束限制)
                truncateStmt.executeUpdate("TRUNCATE TABLE " + postgresTableName); // RESTART IDENTITY 可选，用于重置序列
                System.out.println("PostgreSQL 表 " + postgresTableName + " 中的现有数据已清除 (使用 TRUNCATE)。");
            } catch (SQLException e) {
                System.out.println("无法使用 TRUNCATE TABLE (可能存在外键约束): " + e.getMessage());
                System.out.println("尝试使用 DELETE FROM 清除数据。");
                try (Statement deleteStmt = postgresConn.createStatement()) {
                    deleteStmt.executeUpdate("DELETE FROM " + postgresTableName);
                    System.out.println("PostgreSQL 表 " + postgresTableName + " 中的现有数据已清除 (使用 DELETE FROM)。");
                } catch (SQLException ex) {
                    System.err.println("清除 PostgreSQL 表数据失败 (DELETE FROM): " + ex.getMessage());
                    throw ex; // 重新抛出异常，因为无法清除数据
                }
            }
        } catch (SQLException e) {
            System.err.println("清除 PostgreSQL 表数据失败: " + e.getMessage());
            throw e; // 重新抛出异常，因为无法清除数据
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
                        System.out.println("已同步 " + rowCount + " 条记录");
                    }
                }

                if (rowCount % BATCH_SIZE != 0) {
                    postgresStmt.executeBatch();
                    postgresConn.commit();
                    System.out.println("已同步剩余 " + (rowCount % BATCH_SIZE) + " 条记录");
                }

                System.out.println("数据同步完成，总共同步 " + rowCount + " 条记录");

            } catch (SQLException e) {
                postgresConn.rollback();
                e.printStackTrace();
            } finally {
                postgresConn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
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