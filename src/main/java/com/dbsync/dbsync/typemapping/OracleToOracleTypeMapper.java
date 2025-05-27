package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Oracle to Oracle type mapping (identity or near-identity).
 * Useful for completeness and for tasks within the same DB type.
 */
class OracleToOracleTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(OracleToOracleTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("Oracle source column type is null. Returning as is for Oracle target.");
            return null; // Or handle as an error
        }
        String upperType = sourceColumnType.toUpperCase();
        // Most Oracle types would map to themselves.
        switch (upperType) {
            case "VARCHAR2":
                return columnSize != null ? "VARCHAR2(" + columnSize + ")" : "VARCHAR2(4000)"; // Default size
            case "NVARCHAR2":
                return columnSize != null ? "NVARCHAR2(" + columnSize + ")" : "NVARCHAR2(2000)";
            case "CHAR":
                 return columnSize != null ? "CHAR(" + columnSize + ")" : "CHAR(1)";
            case "NCHAR":
                 return columnSize != null ? "NCHAR(" + columnSize + ")" : "NCHAR(1)";
            case "CLOB":
            case "NCLOB":
            case "LONG":
                return upperType;
            case "NUMBER":
                if (columnSize != null && decimalDigits != null) {
                    return "NUMBER(" + columnSize + "," + decimalDigits + ")";
                } else if (columnSize != null) {
                    return "NUMBER(" + columnSize + ")";
                }
                return "NUMBER";
            case "INTEGER":
            case "INT":
            case "SMALLINT":
                return upperType; // These are often subtypes of NUMBER but can be used directly
            case "FLOAT": // FLOAT(binary_precision)
                return columnSize != null ? "FLOAT(" + columnSize + ")" : "FLOAT";
            case "BINARY_FLOAT":
            case "BINARY_DOUBLE":
                return upperType;
            case "DEC": // ANSI Types, NUMBER is the Oracle equivalent
            case "DECIMAL":
            case "NUMERIC":
                 if (columnSize != null && decimalDigits != null) {
                    return "NUMBER(" + columnSize + "," + decimalDigits + ")";
                } else if (columnSize != null) {
                    return "NUMBER(" + columnSize + ")";
                }
                return "NUMBER";
            case "DATE":
                return "DATE";
            case "TIMESTAMP":
                // TIMESTAMP(p)
                return columnSize != null ? "TIMESTAMP(" + columnSize + ")" : "TIMESTAMP";
            case "TIMESTAMP WITH TIME ZONE":
                 return columnSize != null ? "TIMESTAMP(" + columnSize + ") WITH TIME ZONE" : "TIMESTAMP WITH TIME ZONE";
            case "TIMESTAMP WITH LOCAL TIME ZONE":
                 return columnSize != null ? "TIMESTAMP(" + columnSize + ") WITH LOCAL TIME ZONE" : "TIMESTAMP WITH LOCAL TIME ZONE";
            case "BLOB":
                return "BLOB";
            case "RAW": // RAW(size)
                 return columnSize != null ? "RAW(" + columnSize + ")" : "RAW(2000)"; // Default size
            case "LONG RAW":
                return "LONG RAW";
            case "ROWID":
            case "UROWID":
                return upperType;
            case "XMLTYPE":
                return "XMLTYPE";
            // BFILE, INTERVAL YEAR TO MONTH, INTERVAL DAY TO SECOND etc. can be added
            default:
                logger.warn("OracleToOracleTypeMapper: Unknown Oracle type '{}', passing through.", sourceColumnType);
                return sourceColumnType; // Pass through if unknown or already specific enough
        }
    }
}
