package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles SQL Server to SQL Server type mapping (identity or near-identity).
 */
class SqlServerToSqlServerTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(SqlServerToSqlServerTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("SQL Server source column type is null. Returning as is for SQL Server target.");
            return null;
        }
        String upperType = sourceColumnType.toUpperCase();

        // Most SQL Server types map to themselves.
        // This mapper primarily ensures correct formatting for types with parameters.
        switch (upperType) {
            case "VARCHAR":
            case "NVARCHAR":
                if (columnSize != null && columnSize == -1) return upperType + "(MAX)";
                return columnSize != null ? upperType + "(" + columnSize + ")" : upperType + "(255)"; // Default size
            case "CHAR":
            case "NCHAR":
                return columnSize != null ? upperType + "(" + columnSize + ")" : upperType + "(1)";
            case "TEXT": // Deprecated
            case "NTEXT": // Deprecated
                logger.warn("SQL Server TEXT/NTEXT type (deprecated) passed through. Consider VARCHAR(MAX)/NVARCHAR(MAX).");
                return upperType;

            case "TINYINT":
            case "SMALLINT":
            case "INT":
            case "INTEGER":
            case "BIGINT":
                return upperType;
            case "DECIMAL":
            case "NUMERIC":
                if (columnSize != null && decimalDigits != null) {
                    return upperType + "(" + columnSize + "," + decimalDigits + ")";
                } else if (columnSize != null) {
                    return upperType + "(" + columnSize + ")";
                }
                return upperType; // Default precision/scale (e.g., DECIMAL(18,0))
            case "MONEY":
            case "SMALLMONEY":
                return upperType;
            case "FLOAT": // FLOAT(n)
            case "REAL":  // FLOAT(24)
                return columnSize != null ? "FLOAT(" + columnSize + ")" : upperType; // REAL is FLOAT(24)

            case "DATE":
                return "DATE";
            case "TIME": // TIME(p)
            case "DATETIME2": // DATETIME2(p)
            case "DATETIMEOFFSET": // DATETIMEOFFSET(p)
                // decimalDigits might be used for precision 'p'
                return decimalDigits != null ? upperType + "(" + decimalDigits + ")" : upperType;
            case "DATETIME": // Older type
            case "SMALLDATETIME":
                return upperType;

            case "BINARY":
            case "VARBINARY":
                if (columnSize != null && columnSize == -1) return upperType + "(MAX)";
                return columnSize != null ? upperType + "(" + columnSize + ")" : upperType + "(1)";
            case "IMAGE": // Deprecated
                logger.warn("SQL Server IMAGE type (deprecated) passed through. Consider VARBINARY(MAX).");
                return "IMAGE";

            case "BIT":
                return "BIT";
            case "UNIQUEIDENTIFIER":
                return "UNIQUEIDENTIFIER";
            case "XML":
                return "XML";
            case "ROWVERSION": // Also TIMESTAMP
            case "TIMESTAMP": // Not a date/time type
                return upperType;
            case "SQL_VARIANT":
                return "SQL_VARIANT";
            case "GEOMETRY":
            case "GEOGRAPHY":
            case "HIERARCHYID":
                return upperType;

            default:
                logger.warn("SqlServerToSqlServerTypeMapper: Unknown SQL Server type '{}', passing through.", sourceColumnType);
                return sourceColumnType;
        }
    }
}
