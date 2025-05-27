package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlServerToPostgresTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(SqlServerToPostgresTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("SQL Server source column type is null, defaulting to TEXT.");
            return "TEXT";
        }
        String upperType = sourceColumnType.toUpperCase().trim();
        // columnSize for SQL Server:
        // For char/varchar/binary/varbinary: length in characters/bytes. -1 for MAX.
        // For nchar/nvarchar: length in characters. -1 for MAX.
        // For decimal/numeric: precision.
        // For float: number of bits (<=24 for REAL, >24 for DOUBLE PRECISION in PG).
        // decimalDigits for SQL Server: scale for decimal/numeric.

        switch (upperType) {
            // Character String Types
            case "VARCHAR":
                if (columnSize != null && columnSize == -1) return "TEXT"; // varchar(max)
                if (columnSize != null && columnSize > 0) return "VARCHAR(" + columnSize + ")";
                return "TEXT";
            case "NVARCHAR":
                if (columnSize != null && columnSize == -1) return "TEXT"; // nvarchar(max)
                if (columnSize != null && columnSize > 0) return "VARCHAR(" + columnSize + ")"; // PG VARCHAR is Unicode
                return "TEXT";
            case "CHAR":
                if (columnSize != null && columnSize > 0) return "CHAR(" + columnSize + ")";
                return "CHAR(1)";
            case "NCHAR":
                if (columnSize != null && columnSize > 0) return "CHAR(" + columnSize + ")"; // PG CHAR is Unicode
                return "CHAR(1)";
            case "TEXT": // SQL Server TEXT is deprecated
            case "NTEXT": // SQL Server NTEXT is deprecated
                logger.warn("SQL Server TEXT/NTEXT type encountered, mapping to TEXT. Consider using VARCHAR(MAX) or NVARCHAR(MAX) in SQL Server.");
                return "TEXT";

            // Numeric Types
            case "TINYINT": // 0 to 255
                return "SMALLINT"; // PG SMALLINT is -32768 to 32767
            case "SMALLINT": // -32,768 to 32,767
                return "SMALLINT";
            case "INT": // -2,147,483,648 to 2,147,483,647
            case "INTEGER":
                return "INTEGER";
            case "BIGINT": // -9,223,372,036,854,775,808 to 9,223,372,036,854,775,807
                return "BIGINT";
            case "DECIMAL":
            case "NUMERIC":
                // SQL Server NUMERIC(p,s) or DECIMAL(p,s)
                if (columnSize != null && decimalDigits != null) { // p, s
                    return "NUMERIC(" + columnSize + ", " + decimalDigits + ")";
                } else if (columnSize != null) { // p, s=0
                    return "NUMERIC(" + columnSize + ")";
                }
                return "NUMERIC"; // Default precision
            case "MONEY": // -922,337,203,685,477.5808 to 922,337,203,685,477.5807 (4 decimal places)
                return "NUMERIC(19, 4)"; // PG NUMERIC can handle this
            case "SMALLMONEY": // -214,748.3648 to 214,748.3647 (4 decimal places)
                return "NUMERIC(10, 4)";
            case "FLOAT": // SQL Server FLOAT(n) - n is number of bits for mantissa
                          // n between 1-24 -> float(24) -> REAL in PG
                          // n between 25-53 -> float(53) -> DOUBLE PRECISION in PG
                if (columnSize != null && columnSize <= 24) {
                    return "REAL";
                }
                return "DOUBLE PRECISION";
            case "REAL": // Equivalent to FLOAT(24) in SQL Server
                return "REAL";

            // Date and Time Types
            case "DATE": // YYYY-MM-DD
                return "DATE";
            case "TIME": // hh:mm:ss[.nnnnnnn]
                // columnSize here can be scale for time(n)
                if (decimalDigits != null && decimalDigits > 0) { // decimalDigits might represent fractional seconds precision for TIME(n)
                     return "TIME(" + decimalDigits + ") WITHOUT TIME ZONE";
                }
                return "TIME WITHOUT TIME ZONE";
            case "DATETIME": // YYYY-MM-DD hh:mm:ss[.mmm] - Deprecated, use DATETIME2
                logger.warn("SQL Server DATETIME type encountered, mapping to TIMESTAMP. Consider using DATETIME2 in SQL Server for better precision.");
                return "TIMESTAMP(3) WITHOUT TIME ZONE";
            case "DATETIME2": // YYYY-MM-DD hh:mm:ss[.nnnnnnn]
                 if (decimalDigits != null && decimalDigits > 0) { // decimalDigits might represent fractional seconds precision for DATETIME2(n)
                     return "TIMESTAMP(" + decimalDigits + ") WITHOUT TIME ZONE";
                }
                return "TIMESTAMP WITHOUT TIME ZONE";
            case "SMALLDATETIME": // YYYY-MM-DD hh:mm:ss (no fractions, rounded to nearest minute)
                return "TIMESTAMP(0) WITHOUT TIME ZONE";
            case "DATETIMEOFFSET": // YYYY-MM-DD hh:mm:ss[.nnnnnnn] [+|-]hh:mm
                if (decimalDigits != null && decimalDigits > 0) {
                     return "TIMESTAMP(" + decimalDigits + ") WITH TIME ZONE";
                }
                return "TIMESTAMP WITH TIME ZONE";

            // Binary String Types
            case "BINARY":
                if (columnSize != null && columnSize > 0) return "BYTEA"; // Fixed length, PG BYTEA will handle
                return "BYTEA";
            case "VARBINARY":
                if (columnSize != null && columnSize == -1) return "BYTEA"; // varbinary(max)
                if (columnSize != null && columnSize > 0) return "BYTEA";
                return "BYTEA";
            case "IMAGE": // SQL Server IMAGE is deprecated
                logger.warn("SQL Server IMAGE type encountered, mapping to BYTEA. Consider using VARBINARY(MAX) in SQL Server.");
                return "BYTEA";

            // Other Types
            case "BIT": // 0, 1, or NULL
                return "BOOLEAN";
            case "UNIQUEIDENTIFIER": // GUID
                return "UUID"; // PostgreSQL UUID type
            case "XML":
                return "XML";
            case "ROWVERSION": // Also TIMESTAMP, auto-updating binary(8)
            case "TIMESTAMP": // This is not a date/time type in SQL Server, it's a synonym for ROWVERSION
                logger.info("SQL Server ROWVERSION/TIMESTAMP type mapped to BYTEA(8). This is an auto-generated binary number, not a date/time.");
                return "BYTEA(8)"; // Store as binary data
            case "SQL_VARIANT":
                logger.warn("SQL Server SQL_VARIANT type mapped to TEXT. Data type information will be lost.");
                return "TEXT"; // Complex type, best effort is to map to TEXT
            // Spatial types (GEOMETRY, GEOGRAPHY) - map to TEXT or PostGIS types
            case "GEOMETRY":
            case "GEOGRAPHY":
                logger.warn("SQL Server spatial type {} mapped to TEXT. For full functionality, ensure PostGIS is installed and use specific PostGIS types.", upperType);
                return "TEXT"; // Or "GEOMETRY" / "GEOGRAPHY" if PostGIS is available

            // CLR types (HIERARCHYID, etc.) - highly specific
            case "HIERARCHYID":
                logger.warn("SQL Server HIERARCHYID type mapped to TEXT. Functionality will be lost.");
                return "TEXT";

            default:
                logger.warn("Unknown SQL Server data type: {}. Defaulting to TEXT.", upperType);
                return "TEXT";
        }
    }
}
