package com.dbsync.dbsync.mapper;

import java.util.HashMap;
import java.util.Map;

public class OracleToPostgresTypeMapping {
    private static final Map<String, String> DATA_TYPE_MAPPING = new HashMap<>();

    static {
        // 数值类型映射
        DATA_TYPE_MAPPING.put("NUMBER", "NUMERIC");
        DATA_TYPE_MAPPING.put("NUMERIC", "NUMERIC");
        DATA_TYPE_MAPPING.put("DECIMAL", "NUMERIC");
        DATA_TYPE_MAPPING.put("INTEGER", "INTEGER");
        DATA_TYPE_MAPPING.put("INT", "INTEGER");
        DATA_TYPE_MAPPING.put("SMALLINT", "SMALLINT");
        DATA_TYPE_MAPPING.put("FLOAT", "DOUBLE PRECISION");
        DATA_TYPE_MAPPING.put("REAL", "REAL");
        DATA_TYPE_MAPPING.put("DOUBLE PRECISION", "DOUBLE PRECISION");

        // 字符类型映射
        DATA_TYPE_MAPPING.put("CHAR", "CHAR");
        DATA_TYPE_MAPPING.put("VARCHAR", "VARCHAR");
        DATA_TYPE_MAPPING.put("VARCHAR2", "VARCHAR");
        DATA_TYPE_MAPPING.put("NCHAR", "CHAR");
        DATA_TYPE_MAPPING.put("NVARCHAR2", "VARCHAR");
        DATA_TYPE_MAPPING.put("LONG", "TEXT");
        DATA_TYPE_MAPPING.put("CLOB", "TEXT");
        DATA_TYPE_MAPPING.put("NCLOB", "TEXT");

        // 日期时间类型映射
        DATA_TYPE_MAPPING.put("DATE", "TIMESTAMP");
        DATA_TYPE_MAPPING.put("TIMESTAMP", "TIMESTAMP");
        DATA_TYPE_MAPPING.put("TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH TIME ZONE");
        DATA_TYPE_MAPPING.put("TIMESTAMP WITH LOCAL TIME ZONE", "TIMESTAMP WITH TIME ZONE");

        // 二进制类型映射
        DATA_TYPE_MAPPING.put("RAW", "BYTEA");
        DATA_TYPE_MAPPING.put("LONG RAW", "BYTEA");
        DATA_TYPE_MAPPING.put("BLOB", "BYTEA");
        DATA_TYPE_MAPPING.put("BFILE", "BYTEA");

        // 其他类型映射
        DATA_TYPE_MAPPING.put("BOOLEAN", "BOOLEAN");
        DATA_TYPE_MAPPING.put("XMLTYPE", "XML");
    }

    public static Map<String, String> getDataTypeMapping() {
        return DATA_TYPE_MAPPING;
    }

    public static String getPostgresType(String oracleType) {
        return DATA_TYPE_MAPPING.getOrDefault(oracleType.toUpperCase(), oracleType);
    }
} 