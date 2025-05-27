package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlServerToOracleTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(SqlServerToOracleTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("SQL Server source column type is null, defaulting to VARCHAR2(255) for Oracle.");
            return "VARCHAR2(255)";
        }
        String upperType = sourceColumnType.toUpperCase().trim();

        switch (upperType) {
            // Character String Types
            case "VARCHAR":
                if (columnSize != null && columnSize == -1) return "CLOB"; // varchar(max)
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 4000 && columnSize <= 32767) { // Check for extended string size
                         logger.info("SQL Server VARCHAR({}) mapped to VARCHAR2({}). Ensure Oracle MAX_STRING_SIZE=EXTENDED if length > 4000 bytes.", columnSize, columnSize);
                    } else if (columnSize > 32767) {
                        return "CLOB";
                    }
                    return "VARCHAR2(" + columnSize + ")";
                }
                return "VARCHAR2(255)"; // Default
            case "NVARCHAR":
                if (columnSize != null && columnSize == -1) return "NCLOB"; // nvarchar(max)
                if (columnSize != null && columnSize > 0) {
                     if (columnSize * 2 > 4000 && columnSize <= 32767/2) { // NCHAR/NVARCHAR2 byte factor for older Oracle
                         logger.info("SQL Server NVARCHAR({}) mapped to NVARCHAR2({}). Ensure Oracle MAX_STRING_SIZE=EXTENDED if byte length > 4000.", columnSize, columnSize);
                     } else if (columnSize * 2 > 32767) { // Approx byte limit
                        return "NCLOB";
                     }
                    return "NVARCHAR2(" + columnSize + ")";
                }
                return "NVARCHAR2(255)";
            case "CHAR":
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 2000) return "VARCHAR2(" + columnSize + ")";
                    return "CHAR(" + columnSize + ")";
                }
                return "CHAR(1)";
            case "NCHAR":
                if (columnSize != null && columnSize > 0) {
                     if (columnSize > 2000/2) return "NVARCHAR2(" + columnSize + ")"; // Approx char limit for NCHAR
                    return "NCHAR(" + columnSize + ")";
                }
                return "NCHAR(1)";
            case "TEXT": // SQL Server TEXT deprecated
                logger.warn("SQL Server TEXT type mapped to CLOB. Consider VARCHAR(MAX) in SQL Server.");
                return "CLOB";
            case "NTEXT": // SQL Server NTEXT deprecated
                logger.warn("SQL Server NTEXT type mapped to NCLOB. Consider NVARCHAR(MAX) in SQL Server.");
                return "NCLOB";

            // Numeric Types
            case "TINYINT": // 0 to 255
                return "NUMBER(3)";
            case "SMALLINT": // -32,768 to 32,767
                return "NUMBER(5)"; // Or SMALLINT if Oracle supports it directly and preferred
            case "INT":
            case "INTEGER": // -2,147,483,648 to 2,147,483,647
                return "NUMBER(10)"; // Or INTEGER
            case "BIGINT": // +/-9*10^18
                return "NUMBER(19)";
            case "DECIMAL":
            case "NUMERIC":
                // SQL Server p up to 38. Oracle p up to 38.
                int p = (columnSize != null) ? Math.min(columnSize, 38) : 38;
                int s = (decimalDigits != null) ? decimalDigits : 0;
                if (s > p) s = p;
                if (s < -84) s = -84;
                if (s > 127) s = 127;
                return "NUMBER(" + p + "," + s + ")";
            case "MONEY": // SQL Server MONEY (approx 19,4)
                return "NUMBER(19,4)";
            case "SMALLMONEY": // SQL Server SMALLMONEY (approx 10,4)
                return "NUMBER(10,4)";
            case "FLOAT": // SQL Server FLOAT(n) - n is bits. n=1-24 -> REAL, n=25-53 -> DOUBLE
                // Oracle BINARY_FLOAT (single), BINARY_DOUBLE (double)
                if (columnSize != null && columnSize <= 24) return "BINARY_FLOAT";
                return "BINARY_DOUBLE";
            case "REAL": // SQL Server REAL is float(24)
                return "BINARY_FLOAT";

            // Date and Time Types
            case "DATE": // YYYY-MM-DD
                return "DATE"; // Oracle DATE includes time, time will be 00:00:00
            case "TIME": // SQL Server TIME(p) - p is fractional seconds (0-7)
                // Oracle has no TIME-only type. Map to INTERVAL or TIMESTAMP.
                logger.warn("SQL Server TIME type mapped to Oracle INTERVAL DAY(0) TO SECOND. Check precision needs.");
                int frac = (decimalDigits != null) ? Math.min(decimalDigits, 9) : 0; // decimalDigits may hold precision for TIME(p)
                return "INTERVAL DAY(0) TO SECOND(" + frac + ")";
            case "DATETIME": // SQL Server DATETIME (older type, ~3ms precision)
                logger.warn("SQL Server DATETIME mapped to Oracle TIMESTAMP(3). Consider DATETIME2 in SQL Server.");
                return "TIMESTAMP(3)";
            case "DATETIME2": // SQL Server DATETIME2(p) - p is frac seconds (0-7)
                frac = (decimalDigits != null) ? Math.min(decimalDigits, 9) : 7; // decimalDigits may hold precision for DATETIME2(p)
                return "TIMESTAMP(" + frac + ")";
            case "SMALLDATETIME": // SQL Server SMALLDATETIME (no fractional seconds, nearest minute)
                return "TIMESTAMP(0)";
            case "DATETIMEOFFSET": // SQL Server DATETIMEOFFSET(p)
                frac = (decimalDigits != null) ? Math.min(decimalDigits, 9) : 7; // decimalDigits may hold precision
                return "TIMESTAMP(" + frac + ") WITH TIME ZONE";

            // Binary String Types
            case "BINARY":
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 2000) return "BLOB"; // Oracle RAW limit
                    return "RAW(" + columnSize + ")";
                }
                return "RAW(1)";
            case "VARBINARY":
                if (columnSize != null && columnSize == -1) return "BLOB"; // varbinary(max)
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 4000) return "BLOB"; // Oracle RAW limit for standard string size
                    return "RAW(" + columnSize + ")";
                }
                return "BLOB";
            case "IMAGE": // SQL Server IMAGE deprecated
                logger.warn("SQL Server IMAGE type mapped to BLOB. Consider VARBINARY(MAX) in SQL Server.");
                return "BLOB";

            // Other Types
            case "BIT": // 0, 1, or NULL
                return "NUMBER(1)"; // Common boolean representation in Oracle
            case "UNIQUEIDENTIFIER": // GUID
                return "RAW(16)"; // Store as 16-byte RAW, or VARCHAR2(36)
            case "XML":
                return "XMLTYPE"; // Oracle XMLTYPE
            case "ROWVERSION": // Also SQL Server TIMESTAMP type
            case "TIMESTAMP": // This is not date/time in SQL Server
                logger.info("SQL Server ROWVERSION/TIMESTAMP type mapped to RAW(8) for Oracle.");
                return "RAW(8)";
            case "SQL_VARIANT":
                logger.warn("SQL Server SQL_VARIANT type mapped to CLOB. Data type information will be lost.");
                return "CLOB";
            case "GEOMETRY":
            case "GEOGRAPHY":
                logger.warn("SQL Server spatial type {} mapped to BLOB for Oracle. Requires Oracle Spatial for functionality.", upperType);
                return "BLOB"; // Store raw WKB/WKT or use SDO_GEOMETRY
            case "HIERARCHYID":
                logger.warn("SQL Server HIERARCHYID type mapped to VARCHAR2(4000) for Oracle. Functionality lost.");
                return "VARCHAR2(4000)"; // Store string representation

            default:
                logger.warn("Unknown SQL Server data type: {}. Defaulting to VARCHAR2(255) for Oracle.", upperType);
                return "VARCHAR2(255)";
        }
    }
}
