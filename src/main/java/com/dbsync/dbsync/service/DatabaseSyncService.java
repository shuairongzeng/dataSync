package com.dbsync.dbsync.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dbsync.dbsync.mapper.TableMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatabaseSyncService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSyncService.class);
    private final SqlSessionFactory sourceFactory;
    private final SqlSessionFactory targetFactory;
    private final boolean truncateBeforeSync;

    public DatabaseSyncService(SqlSessionFactory sourceFactory, SqlSessionFactory targetFactory, boolean truncateBeforeSync) {
        this.sourceFactory = sourceFactory;
        this.targetFactory = targetFactory;
        this.truncateBeforeSync = truncateBeforeSync;
    }

    public void syncDatabase() {
        try (SqlSession sourceSession = sourceFactory.openSession();
             SqlSession targetSession = targetFactory.openSession()) {

            TableMapper sourceMapper = sourceSession.getMapper(TableMapper.class);
            TableMapper targetMapper = targetSession.getMapper(TableMapper.class);

            // 1. 获取所有需要同步的表
            List<Map<String, String>> tables = sourceMapper.getAllTableComments();

            for (Map<String, String> table : tables) {
                String tableName = table.get("TABLE_NAME");
                String tableComment = table.get("COMMENTS");
                if (!tableName.equalsIgnoreCase("H2000_ENTRY_WORKFLOW")) {
                    continue;
                }
                try {
                    syncTable(sourceSession, targetSession, tableName, tableComment);
                    targetSession.commit();
                    logger.info("表 {} 同步完成", tableName);
                } catch (Exception e) {
                    targetSession.rollback();
                    logger.error("表 {} 同步失败: {}", tableName, e.getMessage(), e);
                }
            }
        }
    }

    private void syncTable(SqlSession sourceSession, SqlSession targetSession,
                           String tableName, String tableComment) throws Exception {
        TableMapper sourceMapper = sourceSession.getMapper(TableMapper.class);
        TableMapper targetMapper = targetSession.getMapper(TableMapper.class);

        // 检查目标表是否存在
        boolean tableExists = targetMapper.checkPgTableExists(tableName.toLowerCase()) > 0;

        if (!tableExists) {
            // 表不存在，创建表
            logger.info("表 {} 不存在，开始创建表结构", tableName);
            List<Map<String, Object>> structure = sourceMapper.getTableStructure(tableName);
            List<Map<String, String>> columnComments = sourceMapper.getColumnComments(tableName);

            // 生成建表SQL
            String createTableSql = generateCreateTableSql(tableName, structure, tableComment, columnComments);
            logger.debug("建表SQL: {}", createTableSql);

            // 执行建表SQL
            targetMapper.executeDDL(createTableSql);

            // 添加注释（PostgreSQL需要单独执行注释语句）
            if (tableComment != null && !tableComment.isEmpty()) {
                String tableCommentSql = String.format(
                        "COMMENT ON TABLE %s IS '%s'",
                        tableName.toLowerCase(),
                        tableComment.replace("'", "''")
                );
                targetMapper.executeDDL(tableCommentSql);
            }

            // 添加列注释
            for (Map<String, String> comment : columnComments) {
                if (comment.get("COMMENTS") != null && !comment.get("COMMENTS").isEmpty()) {
                    String columnCommentSql = String.format(
                            "COMMENT ON COLUMN %s.%s IS '%s'",
                            tableName.toLowerCase(),
                            comment.get("COLUMN_NAME").toLowerCase(),
                            comment.get("COMMENTS").replace("'", "''")
                    );
                    targetMapper.executeDDL(columnCommentSql);
                }
            }

            logger.info("表 {} 创建完成", tableName);
        } else if (truncateBeforeSync) {
            // 表存在且配置了清空，则先清空表
            logger.info("表 {} 已存在，清空数据后同步", tableName);
            String truncateSql = String.format("TRUNCATE TABLE %s", tableName.toLowerCase());
            targetMapper.executeDDL(truncateSql);
        }

        // 同步数据
        syncTableData(sourceSession, targetSession, tableName);
    }

    private String getPostgreSQLDataType(String oracleType, Number length, Number precision, Number scale) {
        if (oracleType == null) {
            logger.warn("数据类型为空，使用text类型替代");
            return "text";
        }

        oracleType = oracleType.toUpperCase().trim();
        try {
            switch (oracleType) {
                case "VARCHAR2":
                case "NVARCHAR2":
                    return length != null ? "varchar2(" + (length.longValue() * 2) + ")" : "text";

                case "CHAR":
                case "NCHAR":
                    return length != null ? "char(" + length.longValue() * 2 + ")" : "char(1)";

                case "NUMBER":
                    if (precision == null && scale == null) {
                        return "numeric";
                    }
                    if (scale == null || scale.intValue() == 0) {
                        if (precision == null) {
                            return "numeric";
                        }
                        if (precision.intValue() <= 4) {
                            return "smallint";
                        } else if (precision.intValue() <= 9) {
                            return "integer";
                        } else if (precision.intValue() <= 18) {
                            return "bigint";
                        } else {
                            return "numeric(" + precision + ")";
                        }
                    }
                    return String.format("numeric(%d,%d)",
                            precision != null ? precision.intValue() : 38,
                            scale.intValue());

                case "DATE":
                    return "timestamp";

                case "TIMESTAMP":
                case "TIMESTAMP(6)":
                    return "timestamp";

                case "CLOB":
                case "NCLOB":
                case "LONG":
                case "LONG RAW":
                    return "text";

                case "BLOB":
                case "RAW":
                    return "bytea";

                case "FLOAT":
                    return "double precision";

                case "INTEGER":
                    return "integer";

                case "DECIMAL":
                    if (precision != null && scale != null) {
                        return String.format("numeric(%d,%d)", precision.intValue(), scale.intValue());
                    }
                    return "numeric";

                case "BOOLEAN":
                    return "boolean";

                default:
                    logger.warn("未知的数据类型: {}，使用text类型替代", oracleType);
                    return "text";
            }
        } catch (Exception e) {
            logger.error("数据类型转换失败: {} (length={}, precision={}, scale={})",
                    oracleType, length, precision, scale);
            return "text";
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

                // 处理数据类型转换
                String dataType = getPostgreSQLDataType(
                        (String) column.get("DATA_TYPE"),
                        (Number) column.get("DATA_LENGTH"),
                        (Number) column.get("DATA_PRECISION"),
                        (Number) column.get("DATA_SCALE")
                );

                sql.append("    ").append(columnName).append(" ").append(dataType);

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

    private void syncTableData(SqlSession sourceSession, SqlSession targetSession, String tableName) {
        try {
            TableMapper sourceMapper = sourceSession.getMapper(TableMapper.class);
            TableMapper targetMapper = targetSession.getMapper(TableMapper.class);

            // 获取源表总记录数
            long totalCount = sourceMapper.getTableCount(tableName);
            totalCount = totalCount > 10000 ? 10000 : totalCount;
            logger.info("表 {} 总记录数: {}", tableName, totalCount);

            // 分批处理数据
            long batchSize = 1000;
            long processedCount = 0;

            // 初始化分页对象
            Page<Map<String, Object>> page = new Page<>(1, batchSize);

            while (processedCount < totalCount) {
                long offset = (page.getCurrent() - 1) * batchSize;  // 计算偏移量
//                List<Map<String, Object>> data = sourceMapper.getTableData(tableName);
                // 分页查询数据
                List<Map<String, Object>> data = sourceMapper.getTableDataWithPagination(page.getCurrent(), batchSize, tableName);

                if (data.isEmpty()) {
                    break;
                }


                // 处理数据中的null值和特殊字符
                for (Map<String, Object> row : data) {
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        if (entry.getValue() instanceof String) {
                            // 替换特殊字符
                            String value = (String) entry.getValue();
                            entry.setValue(value.replace("'", "''"));
                        }
                    }
                }

                String insertSql = generateBatchInsertSql(tableName.toLowerCase(), data);

                try {
                    targetMapper.executeDML(insertSql);
                    processedCount += data.size();
                    logger.info("表 {} 同步进度: {}/{}", tableName, processedCount, totalCount);
                } catch (Exception e) {
                    logger.error("执行插入SQL失败: {}", e.getMessage());
                    throw e;
                }

                // 更新分页对象的当前页
                page.setCurrent(page.getCurrent() + 1);
            }

        } catch (Exception e) {
            logger.error("同步表数据失败: {}", e.getMessage());
            throw new RuntimeException("同步表数据失败", e);
        }
    }

    private String generateBatchInsertSql(String tableName, List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return "";
        }

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append(" (");

        // 获取列名并过滤掉分页相关的列
        Map<String, Object> firstRow = data.get(0);
        List<String> columns = firstRow.keySet().stream()
                .filter(column -> !column.equalsIgnoreCase("rnum") &&
                        !column.equalsIgnoreCase("rownum")) // 过滤掉分页相关的列
                .collect(Collectors.toList());

        // 添加列名
        sql.append(String.join(", ", columns.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList())));

        sql.append(") VALUES ");

        // 添加值
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            sql.append("(");

            for (int j = 0; j < columns.size(); j++) {
                String column = columns.get(j);
                Object value = row.get(column);

                // 处理不同类型的值
                if (value == null) {
                    sql.append("NULL");
                } else if (value.getClass().getName().equals("oracle.sql.CLOB")) {
                    try {
                        // 将Oracle CLOB转换为字符串
                        oracle.sql.CLOB clob = (oracle.sql.CLOB) value;
                        String clobValue = clob.stringValue();
                        // 转义单引号并处理特殊字符
                        clobValue = clobValue.replace("'", "''")
                                .replace("\r", "\\r")
                                .replace("\n", "\\n");
                        sql.append("'").append(clobValue).append("'");
                    } catch (SQLException e) {
                        // 如果CLOB读取失败，插入空字符串
                        sql.append("''");
                        logger.error("Failed to read CLOB value", e);
                    }
                } else if (value instanceof String) {
                    // 转义单引号，防止SQL注入
                    sql.append("'").append(((String) value).replace("'", "''")).append("'");
                } else if (value instanceof Date) {
                    sql.append("'").append(new Timestamp(((Date) value).getTime())).append("'");
                } else if (value instanceof Timestamp) {
                    sql.append("'").append(value).append("'");
                } else if (value instanceof Boolean) {
                    sql.append(((Boolean) value) ? "true" : "false");
                } else {
                    sql.append(value);
                }

                if (j < columns.size() - 1) {
                    sql.append(", ");
                }
            }

            sql.append(")");
            if (i < data.size() - 1) {
                sql.append(", ");
            }
        }

        return sql.toString();
    }


//    private String generateBatchInsertSql(String tableName, List<Map<String, Object>> data) {
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