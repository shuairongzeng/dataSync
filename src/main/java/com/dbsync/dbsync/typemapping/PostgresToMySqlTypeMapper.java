package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresToMySqlTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(PostgresToMySqlTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("PostgreSQL source column type is null, defaulting to VARCHAR(255).");
            return "VARCHAR(255)"; // A general default for MySQL
        }
        String upperType = sourceColumnType.toUpperCase().trim();

        // columnSize for PostgreSQL:
        // For varchar(n), char(n): n is character length.
        // For numeric(p,s): p is precision, s is scale.
        // For time(p), timestamp(p): p is fractional seconds precision.
        // decimalDigits is scale for numeric.

        switch (upperType) {
            // Character String Types
            case "VARCHAR":
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 65535) { // MySQL VARCHAR limit (bytes, effective chars depend on charset)
                        logger.warn("PostgreSQL VARCHAR({}) exceeds MySQL VARCHAR(65535) limit, mapping to TEXT.", columnSize);
                        return "TEXT";
                    }
                    return "VARCHAR(" + columnSize + ")";
                }
                // MySQL requires length for VARCHAR, default to a common one or TEXT
                return "VARCHAR(255)";
            case "CHAR":
            case "BPCHAR": // blank-padded char
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 255) { // MySQL CHAR limit
                        logger.warn("PostgreSQL CHAR({}) exceeds MySQL CHAR(255) limit, mapping to VARCHAR({}) or TEXT.", columnSize, columnSize);
                        return "VARCHAR(" + columnSize + ")"; // Or TEXT if very large
                    }
                    return "CHAR(" + columnSize + ")";
                }
                return "CHAR(1)";
            case "TEXT":
            case "NAME": // PostgreSQL internal type for identifiers
                return "TEXT"; // MySQL TEXT type

            // Numeric Types
            case "SMALLINT": // -32768 to +32767
            case "INT2":
                return "SMALLINT"; // MySQL SMALLINT is similar
            case "INTEGER": // -2,147,483,648 to +2,147,483,647
            case "INT":
            case "INT4":
                return "INT"; // MySQL INT or INTEGER
            case "BIGINT": // -9,223,372,036,854,775,808 to +9,223,372,036,854,775,807
            case "INT8":
                return "BIGINT"; // MySQL BIGINT is similar
            case "NUMERIC":
            case "DECIMAL":
                // MySQL DECIMAL(p,s) - p up to 65, s up to 30
                if (columnSize != null && decimalDigits != null) {
                    int p = Math.min(columnSize, 65);
                    int s = Math.min(decimalDigits, 30);
                    if (s > p) s = p; // Scale cannot be larger than precision
                    return "DECIMAL(" + p + ", " + s + ")";
                } else if (columnSize != null) {
                    int p = Math.min(columnSize, 65);
                    return "DECIMAL(" + p + ")"; // Default scale 0
                }
                return "DECIMAL"; // Default (DECIMAL(10,0) in MySQL)
            case "REAL": // Single-precision floating-point
            case "FLOAT4":
                return "FLOAT"; // MySQL FLOAT
            case "DOUBLE PRECISION": // Double-precision floating-point
            case "FLOAT8":
                return "DOUBLE"; // MySQL DOUBLE
            case "MONEY": // PostgreSQL money type
                logger.warn("PostgreSQL MONEY type mapped to DECIMAL(19,2). Verify currency representation in MySQL.");
                return "DECIMAL(19,2)"; // MySQL doesn't have a MONEY type

            // Date and Time Types
            case "DATE":
                return "DATE"; // MySQL DATE
            case "TIME":
            case "TIME WITHOUT TIME ZONE":
                 // columnSize is fractional seconds precision (0-6 for MySQL TIME(p))
                if (columnSize != null && columnSize >= 0 && columnSize <= 6) {
                    return "TIME(" + columnSize + ")";
                }
                return "TIME";
            case "TIMETZ":
            case "TIME WITH TIME ZONE":
                logger.warn("PostgreSQL TIME WITH TIME ZONE mapped to TIME. Timezone information will be lost or require application handling in MySQL.");
                if (columnSize != null && columnSize >= 0 && columnSize <= 6) {
                    return "TIME(" + columnSize + ")";
                }
                return "TIME";
            case "TIMESTAMP":
            case "TIMESTAMP WITHOUT TIME ZONE":
                // columnSize is fractional seconds precision (0-6 for MySQL TIMESTAMP(p) / DATETIME(p))
                // MySQL TIMESTAMP has a limited range and auto-update features, DATETIME is often preferred.
                if (columnSize != null && columnSize >= 0 && columnSize <= 6) {
                    return "DATETIME(" + columnSize + ")";
                }
                return "DATETIME";
            case "TIMESTAMPTZ":
            case "TIMESTAMP WITH TIME ZONE":
                logger.warn("PostgreSQL TIMESTAMP WITH TIME ZONE mapped to DATETIME. Timezone information will be lost or require application handling in MySQL.");
                 if (columnSize != null && columnSize >= 0 && columnSize <= 6) {
                    return "DATETIME(" + columnSize + ")";
                }
                return "DATETIME";
            case "INTERVAL":
                logger.warn("PostgreSQL INTERVAL type mapped to VARCHAR(255). MySQL does not have a direct INTERVAL type. Manual conversion needed.");
                return "VARCHAR(255)"; // MySQL has no direct interval type

            // Boolean Type
            case "BOOLEAN":
            case "BOOL":
                return "TINYINT(1)"; // Common way to represent boolean in MySQL

            // Binary Data Types
            case "BYTEA":
                // MySQL has TINYBLOB, BLOB, MEDIUMBLOB, LONGBLOB
                // Map to BLOB by default, or specific one if size is known, though columnSize isn't for BYTEA.
                return "BLOB";

            // Network Address Types
            case "CIDR":
            case "INET":
            case "MACADDR":
            case "MACADDR8":
                logger.info("PostgreSQL network type {} mapped to VARCHAR.", upperType);
                if (upperType.equals("INET")) return "VARCHAR(43)"; // Max IPv6 length
                if (upperType.equals("CIDR")) return "VARCHAR(43)";
                if (upperType.equals("MACADDR")) return "VARCHAR(17)";
                if (upperType.equals("MACADDR8")) return "VARCHAR(23)";
                return "VARCHAR(50)";

            // UUID Type
            case "UUID":
                return "CHAR(36)"; // Store as string representation

            // JSON Types
            case "JSON":
            case "JSONB":
                return "JSON"; // MySQL 5.7.8+ has native JSON type

            // XML Type
            case "XML":
                return "TEXT"; // MySQL does not have a native XML type, store as TEXT

            // Geometric Types
            case "POINT":
            case "LINE":
            case "LSEG":
            case "BOX":
            case "PATH":
            case "POLYGON":
            case "CIRCLE":
                logger.warn("PostgreSQL geometric type {} mapped to TEXT. For spatial features in MySQL, consider its spatial extensions.", upperType);
                return "TEXT"; // MySQL has spatial types, but TEXT is a fallback

            // Array types
            default:
                if (upperType.endsWith("[]")) {
                    logger.warn("PostgreSQL array type {} mapped to TEXT. MySQL does not support arrays directly; consider JSON or separate table.", upperType);
                    return "TEXT"; // Or JSON
                }
                logger.warn("Unknown PostgreSQL data type: {}. Defaulting to VARCHAR(255).", upperType);
                return "VARCHAR(255)";
        }
    }
}
