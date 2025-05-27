package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlServerToMySqlTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(SqlServerToMySqlTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("SQL Server source column type is null, defaulting to VARCHAR(255) for MySQL.");
            return "VARCHAR(255)";
        }
        String upperType = sourceColumnType.toUpperCase().trim();

        switch (upperType) {
            // Character String Types
            case "VARCHAR":
                if (columnSize != null && columnSize == -1) return "LONGTEXT"; // varchar(max)
                if (columnSize != null && columnSize > 0) {
                    // MySQL VARCHAR limit 65535 bytes
                    if (columnSize > 21844) { // Approx for utf8mb3
                        logger.warn("SQL Server VARCHAR({}) might exceed MySQL effective VARCHAR length, mapping to TEXT.", columnSize);
                        return "TEXT"; // Or LONGTEXT if very large
                    }
                    return "VARCHAR(" + columnSize + ")";
                }
                return "VARCHAR(255)";
            case "NVARCHAR":
                if (columnSize != null && columnSize == -1) return "LONGTEXT CHARACTER SET utf8mb4"; // nvarchar(max)
                if (columnSize != null && columnSize > 0) {
                     if (columnSize > 16383) { // Approx for utf8mb4 (4 bytes/char) in 65535 byte limit
                        logger.warn("SQL Server NVARCHAR({}) might exceed MySQL effective VARCHAR length, mapping to TEXT CHARACTER SET utf8mb4.", columnSize);
                        return "TEXT CHARACTER SET utf8mb4";
                    }
                    return "VARCHAR(" + columnSize + ") CHARACTER SET utf8mb4";
                }
                return "VARCHAR(255) CHARACTER SET utf8mb4";
            case "CHAR":
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 255) return "VARCHAR(" + columnSize + ")";
                    return "CHAR(" + columnSize + ")";
                }
                return "CHAR(1)";
            case "NCHAR":
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 255) return "VARCHAR(" + columnSize + ") CHARACTER SET utf8mb4";
                    return "CHAR(" + columnSize + ") CHARACTER SET utf8mb4";
                }
                return "CHAR(1) CHARACTER SET utf8mb4";
            case "TEXT":
                logger.warn("SQL Server TEXT type (deprecated) mapped to TEXT for MySQL.");
                return "TEXT";
            case "NTEXT":
                logger.warn("SQL Server NTEXT type (deprecated) mapped to TEXT CHARACTER SET utf8mb4 for MySQL.");
                return "TEXT CHARACTER SET utf8mb4";

            // Numeric Types
            case "TINYINT": // SQL Server 0 to 255
                return "TINYINT UNSIGNED"; // MySQL TINYINT UNSIGNED
            case "SMALLINT": // +/- 32k
                return "SMALLINT";
            case "INT":
            case "INTEGER": // +/- 2*10^9
                return "INT";
            case "BIGINT": // +/- 9*10^18
                return "BIGINT";
            case "DECIMAL":
            case "NUMERIC":
                // SQL Server p up to 38. MySQL p up to 65.
                int p = (columnSize != null) ? Math.min(columnSize, 65) : 38; // Use SQL server precision if fits, else MySQL max
                int s = (decimalDigits != null) ? Math.min(decimalDigits, 30) : 0; // MySQL scale limit
                if (s > p) s = p;
                return "DECIMAL(" + p + "," + s + ")";
            case "MONEY": // SQL Server MONEY (19,4)
                return "DECIMAL(19,4)";
            case "SMALLMONEY": // SQL Server SMALLMONEY (10,4)
                return "DECIMAL(10,4)";
            case "FLOAT": // SQL Server FLOAT(n)
                // n=1-24 -> REAL (single precision in SQL Server) -> FLOAT in MySQL
                // n=25-53 -> FLOAT (double precision in SQL Server) -> DOUBLE in MySQL
                if (columnSize != null && columnSize <= 24) return "FLOAT";
                return "DOUBLE";
            case "REAL": // SQL Server REAL is float(24)
                return "FLOAT"; // MySQL FLOAT for single-precision

            // Date and Time Types
            case "DATE":
                return "DATE";
            case "TIME": // SQL Server TIME(p)
                int frac = (decimalDigits != null) ? Math.min(decimalDigits, 6) : 0; // decimalDigits may hold precision
                return "TIME(" + frac + ")";
            case "DATETIME": // SQL Server DATETIME (older type)
                logger.warn("SQL Server DATETIME mapped to MySQL DATETIME(3). Consider DATETIME2 in SQL Server.");
                return "DATETIME(3)"; // MySQL DATETIME with millisecond precision
            case "DATETIME2": // SQL Server DATETIME2(p)
                frac = (decimalDigits != null) ? Math.min(decimalDigits, 6) : 6; // decimalDigits may hold precision
                return "DATETIME(" + frac + ")";
            case "SMALLDATETIME":
                return "DATETIME"; // No fractional seconds
            case "DATETIMEOFFSET": // SQL Server DATETIMEOFFSET(p)
                frac = (decimalDigits != null) ? Math.min(decimalDigits, 6) : 6;
                logger.warn("SQL Server DATETIMEOFFSET mapped to MySQL TIMESTAMP({}). Timezone handling differs; MySQL converts to UTC.", frac);
                return "TIMESTAMP(" + frac + ")"; // MySQL TIMESTAMP stores in UTC

            // Binary String Types
            case "BINARY":
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 255) { // MySQL BINARY limit
                        logger.warn("SQL Server BINARY({}) exceeds MySQL BINARY(255) limit, mapping to VARBINARY or BLOB.", columnSize);
                        return "VARBINARY(" + Math.min(columnSize, 65535) + ")"; // VARBINARY if fits, else BLOB
                    }
                    return "BINARY(" + columnSize + ")";
                }
                return "BINARY(1)";
            case "VARBINARY":
                if (columnSize != null && columnSize == -1) return "LONGBLOB"; // varbinary(max)
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 65535) return "LONGBLOB"; // MySQL VARBINARY limit
                    return "VARBINARY(" + columnSize + ")";
                }
                return "BLOB"; // Default for unspecified length
            case "IMAGE":
                logger.warn("SQL Server IMAGE type (deprecated) mapped to LONGBLOB for MySQL.");
                return "LONGBLOB";

            // Other Types
            case "BIT": // SQL Server BIT (0,1,NULL)
                return "BIT(1)"; // MySQL BIT(1)
            case "UNIQUEIDENTIFIER":
                return "CHAR(36)"; // Store as string
            case "XML":
                return "LONGTEXT"; // Store XML as text
            case "ROWVERSION":
            case "TIMESTAMP": // SQL Server specific binary type
                logger.info("SQL Server ROWVERSION/TIMESTAMP type mapped to BINARY(8) for MySQL.");
                return "BINARY(8)";
            case "SQL_VARIANT":
                logger.warn("SQL Server SQL_VARIANT type mapped to LONGTEXT for MySQL. Data type information lost.");
                return "LONGTEXT";
            case "GEOMETRY":
            case "GEOGRAPHY":
                logger.info("SQL Server spatial type {} mapped to GEOMETRY for MySQL.", upperType);
                return "GEOMETRY"; // MySQL has GEOMETRY
            case "HIERARCHYID":
                logger.warn("SQL Server HIERARCHYID mapped to VARCHAR(4000) for MySQL. Functionality lost.");
                return "VARCHAR(4000)"; // Store string representation

            default:
                logger.warn("Unknown SQL Server data type: {}. Defaulting to VARCHAR(255) for MySQL.", upperType);
                return "VARCHAR(255)";
        }
    }
}
