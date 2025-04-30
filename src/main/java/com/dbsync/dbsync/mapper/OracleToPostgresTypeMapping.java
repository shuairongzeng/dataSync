package com.dbsync.dbsync.mapper;

import java.util.HashMap;
import java.util.Map;

public class OracleToPostgresTypeMapping {

    public static Map<String, String> getDataTypeMapping() {
        Map<String, String> mapping = new HashMap<>();

        // 字符类型
        mapping.put("VARCHAR2", "VARCHAR");
        mapping.put("VARCHAR2", "VARCHAR"); // 不指定长度时也映射到 VARCHAR
        mapping.put("NVARCHAR2", "VARCHAR"); // PostgreSQL 支持 Unicode
        mapping.put("CHAR", "CHAR");
        mapping.put("NCHAR", "CHAR");
        mapping.put("CLOB", "TEXT");
        mapping.put("LONG", "TEXT"); // 注意：Oracle 的 LONG 类型有诸多限制，建议迁移到 CLOB

        // 数值类型
        mapping.put("NUMBER", "NUMERIC"); // 默认映射到 NUMERIC，可以处理任意精度和标度
        mapping.put("NUMBER(*,0)", "BIGINT"); // 整数，无小数
        mapping.put("NUMBER(38)", "BIGINT"); // Oracle 中常用于表示大整数
        mapping.put("INTEGER", "INTEGER");
        mapping.put("INT", "INTEGER");
        mapping.put("SMALLINT", "SMALLINT");
        mapping.put("DECIMAL", "NUMERIC");
        mapping.put("DEC", "NUMERIC");
        mapping.put("NUMERIC", "NUMERIC");
        mapping.put("FLOAT", "REAL"); // 注意精度差异
        mapping.put("BINARY_FLOAT", "REAL");
        mapping.put("DOUBLE PRECISION", "DOUBLE PRECISION");
        mapping.put("BINARY_DOUBLE", "DOUBLE PRECISION");

        // 日期和时间类型
        mapping.put("DATE", "DATE");
        mapping.put("TIMESTAMP", "TIMESTAMP WITHOUT TIME ZONE");
        mapping.put("TIMESTAMP WITHOUT TIME ZONE", "TIMESTAMP WITHOUT TIME ZONE");
        mapping.put("TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH TIME ZONE");
        mapping.put("TIMESTAMP WITH LOCAL TIME ZONE", "TIMESTAMP WITH TIME ZONE"); // PostgreSQL 没有 LOCAL TIME ZONE 的概念

        // 二进制类型
        mapping.put("BLOB", "BYTEA");
        mapping.put("RAW", "BYTEA");
        mapping.put("LONG RAW", "BYTEA"); // 注意：Oracle 的 LONG RAW 类型有诸多限制，建议迁移到 BLOB

        // 布尔类型 (Oracle 没有标准的布尔类型，通常用 NUMBER(1) 或 CHAR(1) 表示)
        // 如果你的 Oracle 表中使用了 NUMBER(1) 或 CHAR(1) 来表示布尔值，
        // 你可能需要在代码中进行额外的逻辑判断和转换。
        // 例如：
        // if ("NUMBER".equalsIgnoreCase(oracleDataTypeName) && columnSize == 1) {
        //     postgresDataType = "BOOLEAN";
        // } else if ("CHAR".equalsIgnoreCase(oracleDataTypeName) && columnSize == 1) {
        //     postgresDataType = "BOOLEAN";
        // }

        // 其他类型 (可能需要特殊处理)
        // mapping.put("ROWID", "VARCHAR(18)"); // Oracle 特有的行标识符，可以映射为 VARCHAR
        // mapping.put("UROWID", "VARCHAR(32)"); // Oracle 特有的通用行标识符，可以映射为 VARCHAR
        // mapping.put("XMLTYPE", "XML"); // PostgreSQL 支持 XML 类型
        // mapping.put("JSON", "JSONB"); // PostgreSQL 支持 JSON 和 JSONB 类型 (推荐使用 JSONB)

        return mapping;
    }

    public static String getMappedType(String oracleType) {
        return getDataTypeMapping().getOrDefault(oracleType.toUpperCase(), oracleType.toUpperCase());
    }

    public static void main(String[] args) {
        Map<String, String> mapping = getDataTypeMapping();
        System.out.println("Oracle to PostgreSQL Data Type Mapping:");
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }

        // 示例使用
        String oracleVarchar = "VARCHAR2";
        String postgresVarchar = getMappedType(oracleVarchar);
        System.out.println("\nExample:");
        System.out.println("Oracle type: " + oracleVarchar + " -> PostgreSQL type: " + postgresVarchar);

        String oracleNumberWithPrecision = "NUMBER(10, 2)";
        // 注意：对于带精度和标度的 NUMBER，你需要在获取列信息时解析这些信息
        // 并构建更精确的 PostgreSQL 类型，例如 NUMERIC(10, 2)

        String oracleTimestampWithTZ = "TIMESTAMP WITH TIME ZONE";
        String postgresTimestampWithTZ = getMappedType(oracleTimestampWithTZ);
        System.out.println("Oracle type: " + oracleTimestampWithTZ + " -> PostgreSQL type: " + postgresTimestampWithTZ);
    }
}