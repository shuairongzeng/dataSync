package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySqlToPostgresTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(MySqlToPostgresTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("MySQL source column type is null, defaulting to TEXT.");
            return "TEXT";
        }
        String upperType = sourceColumnType.toUpperCase().trim();

        // Note: columnSize for MySQL often refers to character length for strings,
        // or display width for integers (which doesn't directly map to PG storage).
        // decimalDigits is the scale for DECIMAL/NUMERIC.

        switch (upperType) {
            // Character String Types
            case "VARCHAR":
                if (columnSize != null && columnSize > 0) {
                    return "VARCHAR(" + columnSize + ")";
                }
                return "TEXT"; // Default for unspecified length
            case "CHAR":
                if (columnSize != null && columnSize > 0) {
                    return "CHAR(" + columnSize + ")";
                }
                return "CHAR(1)";
            case "TINYTEXT":
            case "TEXT":
            case "MEDIUMTEXT":
            case "LONGTEXT":
                return "TEXT";
            case "ENUM": // Map ENUM to TEXT; constraints can be handled separately if needed
            case "SET":  // Map SET to TEXT or an array of TEXT
                logger.info("MySQL ENUM/SET type '{}' mapped to TEXT. Consider application-level validation or PostgreSQL ENUM type if appropriate.", upperType);
                return "TEXT";

            // Numeric Types
            case "TINYINT": // MySQL TINYINT can be boolean (TINYINT(1)) or small integer
                if (columnSize != null && columnSize == 1) {
                    // This is often used as a boolean
                    // return "BOOLEAN"; // Or stick to SMALLINT if direct numeric mapping is preferred
                }
                return "SMALLINT"; // PostgreSQL SMALLINT is -32768 to +32767
            case "SMALLINT":
                return "SMALLINT";
            case "MEDIUMINT": // No direct equivalent, map to INTEGER
                return "INTEGER";
            case "INT":
            case "INTEGER":
                return "INTEGER";
            case "BIGINT":
                return "BIGINT";
            case "FLOAT": // MySQL FLOAT(p) - p is precision in bits.
                          // If p <= 24, it's single-precision. If 24 < p <= 53, it's double-precision.
                if (columnSize != null && columnSize <= 24) { // columnSize here is total digits for FLOAT(M,D) or bits for FLOAT(p)
                     return "REAL"; // Single-precision
                }
                return "DOUBLE PRECISION"; // Default or for p > 24
            case "DOUBLE":
            case "DOUBLE PRECISION":
                return "DOUBLE PRECISION";
            case "DECIMAL":
            case "DEC":
            case "NUMERIC": // NUMERIC is often an alias for DECIMAL in MySQL
                if (columnSize != null && decimalDigits != null) { // DECIMAL(p,s)
                    return "NUMERIC(" + columnSize + ", " + decimalDigits + ")";
                } else if (columnSize != null) { // DECIMAL(p)
                    return "NUMERIC(" + columnSize + ")";
                }
                return "NUMERIC"; // Default precision
            case "BIT": // BIT(M) in MySQL stores M bits (1 to 64)
                if (columnSize != null && columnSize == 1) {
                    return "BOOLEAN"; // BIT(1) is often used as boolean
                }
                // PostgreSQL has BIT(n) or BIT VARYING(n)
                if (columnSize != null && columnSize > 0) {
                    return "BIT(" + columnSize + ")";
                }
                return "BIT(1)"; // Default mapping

            // Date and Time Types
            case "DATE":
                return "DATE";
            case "DATETIME": // DATETIME range '1000-01-01 00:00:00' to '9999-12-31 23:59:59'
                return "TIMESTAMP WITHOUT TIME ZONE";
            case "TIMESTAMP": // TIMESTAMP range '1970-01-01 00:00:01' UTC to '2038-01-19 03:14:07' UTC
                              // Stored as UTC, converted to/from session time_zone
                return "TIMESTAMP WITH TIME ZONE"; // Or WITHOUT TIME ZONE if UTC storage is not desired in PG
            case "TIME":
                return "TIME WITHOUT TIME ZONE";
            case "YEAR": // YEAR(2) or YEAR(4)
                return "SMALLINT"; // Store as integer; constraints can be added if needed

            // Binary String Types
            case "BINARY":
                if (columnSize != null && columnSize > 0) {
                    return "BYTEA"; // Or potentially CHAR(n) with appropriate encoding if it's fixed length text
                }
                return "BYTEA";
            case "VARBINARY":
                 if (columnSize != null && columnSize > 0) {
                    return "BYTEA"; // Or VARCHAR(n) with encoding
                }
                return "BYTEA";
            case "TINYBLOB":
            case "BLOB":
            case "MEDIUMBLOB":
            case "LONGBLOB":
                return "BYTEA";

            // Spatial Types (basic mapping to TEXT, specific types like GEOMETRY can be used in PG with PostGIS)
            case "GEOMETRY":
            case "POINT":
            case "LINESTRING":
            case "POLYGON":
            case "MULTIPOINT":
            case "MULTILINESTRING":
            case "MULTIPOLYGON":
            case "GEOMETRYCOLLECTION":
                logger.warn("MySQL spatial type {} mapped to TEXT. For full functionality, ensure PostGIS is installed and use specific PostGIS types.", upperType);
                return "TEXT"; // Or specific PostGIS types if PostGIS is available e.g. "GEOMETRY"

            // JSON Type
            case "JSON":
                return "JSONB"; // PostgreSQL JSONB is generally preferred over JSON

            default:
                logger.warn("Unknown MySQL data type: {}. Defaulting to TEXT.", upperType);
                return "TEXT";
        }
    }
}
