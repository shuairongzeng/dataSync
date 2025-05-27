package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresToSqlServerTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(PostgresToSqlServerTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("PostgreSQL source column type is null, defaulting to VARCHAR(255).");
            return "VARCHAR(255)"; // A general default for SQL Server
        }
        String upperType = sourceColumnType.toUpperCase().trim();

        // columnSize for PostgreSQL: varchar(n), char(n) -> n; numeric(p,s) -> p; time(p), timestamp(p) -> p (precision)
        // decimalDigits for PostgreSQL: numeric(p,s) -> s (scale)

        switch (upperType) {
            // Character String Types
            case "VARCHAR":
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 8000) { // SQL Server VARCHAR limit (bytes)
                        logger.warn("PostgreSQL VARCHAR({}) exceeds SQL Server VARCHAR(8000) limit, mapping to VARCHAR(MAX).", columnSize);
                        return "VARCHAR(MAX)";
                    }
                    return "VARCHAR(" + columnSize + ")";
                }
                return "VARCHAR(MAX)"; // Default for unspecified or very large
            case "CHAR":
            case "BPCHAR": // blank-padded char
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 8000) {
                        logger.warn("PostgreSQL CHAR({}) exceeds SQL Server CHAR(8000) limit, mapping to VARCHAR(MAX).", columnSize);
                        return "VARCHAR(MAX)";
                    }
                    return "CHAR(" + columnSize + ")";
                }
                return "CHAR(1)";
            case "TEXT":
            case "NAME": // PostgreSQL internal type for identifiers
                return "VARCHAR(MAX)"; // SQL Server VARCHAR(MAX) for large text

            // Numeric Types
            case "SMALLINT": // -32768 to +32767
            case "INT2":
                return "SMALLINT"; // SQL Server SMALLINT is identical
            case "INTEGER": // -2,147,483,648 to +2,147,483,647
            case "INT":
            case "INT4":
                return "INT"; // SQL Server INT or INTEGER
            case "BIGINT": // -9,223,372,036,854,775,808 to +9,223,372,036,854,775,807
            case "INT8":
                return "BIGINT"; // SQL Server BIGINT is identical
            case "NUMERIC":
            case "DECIMAL":
                // SQL Server DECIMAL(p,s) or NUMERIC(p,s) - p up to 38, s up to p
                if (columnSize != null && decimalDigits != null) {
                    int p = Math.min(columnSize, 38);
                    int s = Math.min(decimalDigits, p); // Scale cannot be larger than precision
                    return "DECIMAL(" + p + ", " + s + ")";
                } else if (columnSize != null) {
                    int p = Math.min(columnSize, 38);
                    return "DECIMAL(" + p + ")"; // Default scale 0
                }
                return "DECIMAL"; // Default (DECIMAL(18,0) in SQL Server)
            case "REAL": // Single-precision floating-point
            case "FLOAT4":
                return "REAL"; // SQL Server REAL (float(24))
            case "DOUBLE PRECISION": // Double-precision floating-point
            case "FLOAT8":
                return "FLOAT"; // SQL Server FLOAT (float(53))
            case "MONEY":
                logger.warn("PostgreSQL MONEY type mapped to MONEY. Verify currency representation in SQL Server.");
                return "MONEY"; // SQL Server has a MONEY type

            // Date and Time Types
            case "DATE":
                return "DATE"; // SQL Server DATE
            case "TIME":
            case "TIME WITHOUT TIME ZONE":
                // columnSize is fractional seconds precision (0-7 for SQL Server TIME(p))
                if (columnSize != null && columnSize >= 0 && columnSize <= 7) {
                    return "TIME(" + columnSize + ")";
                }
                return "TIME"; // Default TIME(7) in SQL Server
            case "TIMETZ":
            case "TIME WITH TIME ZONE":
                logger.warn("PostgreSQL TIME WITH TIME ZONE mapped to DATETIMEOFFSET. Check precision and timezone handling.");
                // SQL Server DATETIMEOFFSET includes timezone
                if (columnSize != null && columnSize >=0 && columnSize <= 7) {
                    return "DATETIMEOFFSET(" + columnSize + ")";
                }
                return "DATETIMEOFFSET";
            case "TIMESTAMP":
            case "TIMESTAMP WITHOUT TIME ZONE":
                // columnSize is fractional seconds precision (0-7 for SQL Server DATETIME2(p))
                if (columnSize != null && columnSize >= 0 && columnSize <= 7) {
                    return "DATETIME2(" + columnSize + ")";
                }
                return "DATETIME2"; // SQL Server DATETIME2 is preferred over DATETIME
            case "TIMESTAMPTZ":
            case "TIMESTAMP WITH TIME ZONE":
                 logger.warn("PostgreSQL TIMESTAMP WITH TIME ZONE mapped to DATETIMEOFFSET. Check precision.");
                if (columnSize != null && columnSize >=0 && columnSize <= 7) {
                    return "DATETIMEOFFSET(" + columnSize + ")";
                }
                return "DATETIMEOFFSET";
            case "INTERVAL":
                logger.warn("PostgreSQL INTERVAL type mapped to VARCHAR(255). SQL Server does not have a direct INTERVAL type. Manual conversion needed.");
                return "VARCHAR(255)"; // No direct equivalent

            // Boolean Type
            case "BOOLEAN":
            case "BOOL":
                return "BIT"; // SQL Server BIT for boolean (0, 1, NULL)

            // Binary Data Types
            case "BYTEA":
                return "VARBINARY(MAX)"; // SQL Server VARBINARY(MAX) for large binary data

            // Network Address Types
            case "CIDR":
            case "INET":
            case "MACADDR":
            case "MACADDR8":
                logger.info("PostgreSQL network type {} mapped to VARCHAR(MAX).", upperType);
                return "VARCHAR(MAX)"; // Store as string

            // UUID Type
            case "UUID":
                return "UNIQUEIDENTIFIER"; // SQL Server native UUID type

            // JSON Types
            case "JSON":
            case "JSONB":
                // SQL Server 2016+ has native JSON support (stored as NVARCHAR(MAX))
                // with functions like ISJSON, JSON_VALUE, JSON_QUERY.
                logger.info("PostgreSQL JSON/JSONB type mapped to NVARCHAR(MAX). Use SQL Server JSON functions for querying.");
                return "NVARCHAR(MAX)";

            // XML Type
            case "XML":
                return "XML"; // SQL Server has native XML type

            // Geometric Types
            case "POINT":
            case "LINE":
            case "LSEG":
            case "BOX":
            case "PATH":
            case "POLYGON":
            case "CIRCLE":
                logger.warn("PostgreSQL geometric type {} mapped to VARCHAR(MAX). For spatial features in SQL Server, consider its spatial types (GEOMETRY/GEOGRAPHY).", upperType);
                return "VARCHAR(MAX)";

            // Array types
            default:
                if (upperType.endsWith("[]")) {
                    logger.warn("PostgreSQL array type {} mapped to VARCHAR(MAX). SQL Server does not support arrays directly; consider XML, JSON, or separate table.", upperType);
                    return "VARCHAR(MAX)"; // Or store as XML/JSON
                }
                logger.warn("Unknown PostgreSQL data type: {}. Defaulting to VARCHAR(255).", upperType);
                return "VARCHAR(255)";
        }
    }
}
