package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresToOracleTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(PostgresToOracleTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("PostgreSQL source column type is null, defaulting to VARCHAR2(255).");
            return "VARCHAR2(255)"; // A general default for Oracle
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
                    if (columnSize > 4000) {
                        logger.warn("PostgreSQL VARCHAR({}) exceeds Oracle VARCHAR2(4000) byte limit, mapping to CLOB.", columnSize);
                        return "CLOB"; // Oracle VARCHAR2 max is 4000 bytes (or 32767 with MAX_STRING_SIZE=EXTENDED)
                    }
                    return "VARCHAR2(" + columnSize + ")";
                }
                return "CLOB"; // Default for unspecified length, Oracle prefers CLOB for large text
            case "CHAR":
            case "BPCHAR": // blank-padded char, PostgreSQL internal name for CHAR
                if (columnSize != null && columnSize > 0) {
                     if (columnSize > 2000) {
                        logger.warn("PostgreSQL CHAR({}) exceeds Oracle CHAR(2000) byte limit, mapping to CLOB.", columnSize);
                        return "CLOB";
                    }
                    return "CHAR(" + columnSize + ")";
                }
                return "CHAR(1)";
            case "TEXT":
                return "CLOB";
            case "NAME": // PostgreSQL internal type for identifiers
                 return "VARCHAR2(63)"; // Max length of identifiers in PG is often 63 bytes

            // Numeric Types
            case "SMALLINT": // -32768 to +32767
            case "INT2":
                return "NUMBER(5)"; // Oracle NUMBER can represent this
            case "INTEGER": // -2,147,483,648 to +2,147,483,647
            case "INT":
            case "INT4":
                return "NUMBER(10)"; // Or INTEGER if available and preferred
            case "BIGINT": // -9,223,372,036,854,775,808 to +9,223,372,036,854,775,807
            case "INT8":
                return "NUMBER(19)";
            case "NUMERIC":
            case "DECIMAL":
                if (columnSize != null && decimalDigits != null) { // NUMERIC(p,s)
                    // Oracle NUMBER precision p is 1 to 38. Scale s is -84 to 127.
                    int p = Math.min(columnSize, 38);
                    int s = decimalDigits;
                    if (s < -84) s = -84;
                    if (s > 127) s = 127;
                    return "NUMBER(" + p + ", " + s + ")";
                } else if (columnSize != null) { // NUMERIC(p)
                     int p = Math.min(columnSize, 38);
                    return "NUMBER(" + p + ")";
                }
                return "NUMBER"; // Default precision
            case "REAL": // Single-precision floating-point
            case "FLOAT4":
                return "BINARY_FLOAT"; // Oracle's single-precision float
            case "DOUBLE PRECISION": // Double-precision floating-point
            case "FLOAT8":
                return "BINARY_DOUBLE"; // Oracle's double-precision float
            case "MONEY": // PostgreSQL money type (locale-dependent, fixed precision)
                logger.warn("PostgreSQL MONEY type mapped to NUMBER(19,2). Verify locale and precision requirements.");
                return "NUMBER(19,2)"; // Map to a NUMBER with typical currency precision

            // Date and Time Types
            case "DATE": // Date (no time of day)
                return "DATE"; // Oracle DATE includes time, but time part will be 00:00:00
            case "TIME":
            case "TIME WITHOUT TIME ZONE":
                // Oracle does not have a TIME-only type. Map to VARCHAR2 or TIMESTAMP.
                // Storing as VARCHAR2 is safer if only time is needed.
                // columnSize here is fractional second precision for TIME(p)
                if (columnSize != null && columnSize > 0) {
                     logger.warn("PostgreSQL TIME({}) mapped to VARCHAR2(15). Oracle has no direct TIME type.", columnSize);
                    return "VARCHAR2(15)"; // HH:MM:SS.FFFFFF
                }
                return "VARCHAR2(8)"; // HH:MM:SS
            case "TIMETZ":
            case "TIME WITH TIME ZONE":
                logger.warn("PostgreSQL TIMETZ mapped to VARCHAR2(21). Oracle has no direct TIME WITH TIMEZONE type.");
                return "VARCHAR2(21)"; // HH:MM:SS.FFFFFF+TZ
            case "TIMESTAMP":
            case "TIMESTAMP WITHOUT TIME ZONE":
                // columnSize here is fractional second precision for TIMESTAMP(p)
                if (columnSize != null && columnSize >= 0 && columnSize <= 9) {
                     return "TIMESTAMP(" + columnSize + ")";
                }
                return "TIMESTAMP"; // Defaults to TIMESTAMP(6) in Oracle
            case "TIMESTAMPTZ":
            case "TIMESTAMP WITH TIME ZONE":
                 if (columnSize != null && columnSize >= 0 && columnSize <= 9) {
                    return "TIMESTAMP(" + columnSize + ") WITH TIME ZONE";
                }
                return "TIMESTAMP WITH TIME ZONE";
            case "INTERVAL":
                // Oracle supports INTERVAL YEAR TO MONTH and INTERVAL DAY TO SECOND
                // This is a complex mapping, defaulting to VARCHAR2
                logger.warn("PostgreSQL INTERVAL type mapped to VARCHAR2(255). Manual conversion might be needed for full fidelity.");
                return "VARCHAR2(255)";

            // Boolean Type
            case "BOOLEAN":
            case "BOOL":
                return "NUMBER(1)"; // Common way to represent boolean in Oracle (0 or 1)

            // Binary Data Types
            case "BYTEA":
                return "BLOB"; // Or RAW(size) if size is <= 2000 and known

            // Network Address Types
            case "CIDR":
            case "INET":
            case "MACADDR":
            case "MACADDR8":
                logger.info("PostgreSQL network type {} mapped to VARCHAR2.", upperType);
                if (upperType.equals("INET")) return "VARCHAR2(43)"; // Max IPv6 length
                if (upperType.equals("CIDR")) return "VARCHAR2(43)"; // Max IPv6/mask
                if (upperType.equals("MACADDR")) return "VARCHAR2(17)";
                if (upperType.equals("MACADDR8")) return "VARCHAR2(23)";
                return "VARCHAR2(50)";

            // UUID Type
            case "UUID":
                return "RAW(16)"; // Store as 16-byte raw, or VARCHAR2(36) for string representation

            // JSON Types
            case "JSON":
            case "JSONB":
                // Oracle 12c+ supports JSON in VARCHAR2/CLOB/BLOB. Oracle 21c+ has native JSON type.
                // Assuming older Oracle for wider compatibility, map to CLOB.
                // If MAX_STRING_SIZE=EXTENDED and JSON is small, VARCHAR2(32767) could be an option.
                logger.info("PostgreSQL JSON/JSONB type mapped to CLOB. Consider native JSON type if using Oracle 21c+.");
                return "CLOB";

            // XML Type
            case "XML":
                return "XMLTYPE";

            // Geometric Types (POINT, LINE, LSEG, BOX, PATH, POLYGON, CIRCLE)
            // These require Oracle Spatial or mapping to VARCHAR2/CLOB. Defaulting to CLOB.
            case "POINT":
            case "LINE":
            case "LSEG":
            case "BOX":
            case "PATH":
            case "POLYGON":
            case "CIRCLE":
                logger.warn("PostgreSQL geometric type {} mapped to CLOB. For spatial features, consider Oracle Spatial.", upperType);
                return "CLOB";

            // Array types (e.g., INTEGER[], TEXT[])
            // Oracle has VARRAY or nested tables. This is a complex mapping. Default to CLOB.
            default:
                if (upperType.endsWith("[]")) {
                    logger.warn("PostgreSQL array type {} mapped to CLOB. Manual handling required for array structures in Oracle.", upperType);
                    return "CLOB";
                }
                logger.warn("Unknown PostgreSQL data type: {}. Defaulting to VARCHAR2(255).", upperType);
                return "VARCHAR2(255)";
        }
    }
}
