package com.dbsync.dbsync.service;

import org.apache.ibatis.jdbc.SQL;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BatchInsertSqlProvider {

    public String generateBatchInsertSql(Map<String, Object> params) {
        String tableName = (String) params.get("tableName");
        List<Map<String, Object>> data = (List<Map<String, Object>>) params.get("data");

        if (data.isEmpty()) {
            return "";
        }

        SQL sql = new SQL() {{
            INSERT_INTO(tableName);
        }};

        // 获取列名并过滤掉分页相关的列
        Map<String, Object> firstRow = data.get(0);
        List<String> columns = firstRow.keySet().stream()
                .filter(column -> !column.equalsIgnoreCase("rnum") &&
                        !column.equalsIgnoreCase("rownum")) // 过滤掉分页相关的列
                .collect(Collectors.toList());

        // 添加列名
        sql.INTO_COLUMNS(columns.stream()
                .map(String::toLowerCase)
                .collect(Collectors.joining(", ")));

        // 添加值
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            sql.INTO_VALUES(columns.stream()
                    .map(column -> {
                        Object value = row.get(column);
                        if (value == null) {
                            return "NULL";
                        } else if (value instanceof String) {
                            return "'" + ((String) value).replace("'", "''") + "'";
                        } else if (value instanceof java.sql.Date) {
                            return "'" + new java.sql.Timestamp(((java.sql.Date) value).getTime()) + "'";
                        } else if (value instanceof java.sql.Timestamp) {
                            return "'" + value + "'";
                        } else if (value instanceof Boolean) {
                            return ((Boolean) value) ? "true" : "false";
                        } else {
                            return value.toString();
                        }
                    })
                    .collect(Collectors.joining(", ")));
        }

        return sql.toString();
    }
}
