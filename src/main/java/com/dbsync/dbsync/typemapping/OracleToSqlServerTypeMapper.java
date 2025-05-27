package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OracleToSqlServerTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(OracleToSqlServerTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("Oracle source column type is null, defaulting to VARCHAR(255) for SQL Server.");
            return "VARCHAR(255)";
        }
        String upperType = sourceColumnType.toUpperCase().trim();

        switch (upperType) {
            // Character Types
            case "VARCHAR2":
            case "NVARCHAR2":
                if (columnSize != null && columnSize > 0) {
                    // SQL Server VARCHAR/NVARCHAR limit is 8000 bytes/chars, or MAX for larger
                    // Oracle VARCHAR2 can be 4000 bytes/chars or 32767 bytes.
                    if (columnSize > 8000) {
                        logger.warn("Oracle type {} ({}) exceeds SQL Server non-MAX length, mapping to VARCHAR(MAX).", upperType, columnSize);
                        return upperType.startsWith("N") ? "NVARCHAR(MAX)" : "VARCHAR(MAX)";
                    }
                    return (upperType.startsWith("N") ? "NVARCHAR(" : "VARCHAR(") + columnSize + ")";
                }
                return upperType.startsWith("N") ? "NVARCHAR(MAX)" : "VARCHAR(MAX)";
            case "CHAR":
            case "NCHAR":
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 8000) {
                         logger.warn("Oracle type {} ({}) exceeds SQL Server non-MAX length, mapping to VARCHAR(MAX).", upperType, columnSize);
                        return upperType.startsWith("N") ? "NVARCHAR(MAX)" : "VARCHAR(MAX)";
                    }
                    return (upperType.startsWith("N") ? "NCHAR(" : "CHAR(") + columnSize + ")";
                }
                return upperType.startsWith("N") ? "NCHAR(1)" : "CHAR(1)";
            case "CLOB":
            case "NCLOB":
            case "LONG": // LONG is deprecated
                return upperType.startsWith("N") || upperType.equals("CLOB") /* CLOB can be UTF8 */ ? "NVARCHAR(MAX)" : "VARCHAR(MAX)";

            // Numeric Types
            case "NUMBER":
                // SQL Server NUMERIC/DECIMAL precision p up to 38.
                if (decimalDigits != null && decimalDigits > 0) { // Has scale (decimal part)
                    int p = (columnSize != null) ? Math.min(columnSize, 38) : 38; // Default precision 38
                    int s = Math.min(decimalDigits, p); // Scale can't be > precision
                    return "DECIMAL(" + p + "," + s + ")";
                } else { // Integer types or NUMBER(p)
                    if (columnSize != null) { // NUMBER(p) or NUMBER(p,0)
                        if (columnSize <= 2) return "TINYINT";    // SQL Server TINYINT 0-255
                        else if (columnSize <= 4) return "SMALLINT"; // SQL Server SMALLINT +/- 32k
                        else if (columnSize <= 9) return "INT";      // SQL Server INT +/- 2*10^9
                        else if (columnSize <= 18) return "BIGINT";   // SQL Server BIGINT +/- 9*10^18
                        else return "DECIMAL(" + Math.min(columnSize, 38) + ",0)";
                    }
                    return "DECIMAL(38,10)"; // General purpose decimal
                }
            case "INTEGER":
            case "INT":
                return "INT";
            case "SMALLINT":
                return "SMALLINT";
            case "FLOAT": // Oracle FLOAT(binary_precision)
                // SQL Server REAL (float(24)) for single, FLOAT (float(53)) for double
                if (columnSize != null && columnSize < 24) {
                    return "REAL";
                }
                return "FLOAT"; // Defaults to float(53)
            case "BINARY_FLOAT":
                return "REAL"; // SQL Server REAL
            case "BINARY_DOUBLE":
                return "FLOAT"; // SQL Server FLOAT(53)
            case "DEC":
            case "DECIMAL":
            case "NUMERIC":
                 int p = (columnSize != null) ? Math.min(columnSize, 38) : 38;
                 int s = (decimalDigits != null) ? Math.min(decimalDigits, p) : 0;
                 return "DECIMAL(" + p + "," + s + ")";

            // Date and Time Types
            case "DATE": // Oracle DATE contains date and time
                return "DATETIME2(3)"; // SQL Server DATETIME2 is preferred, (3) for ms precision like old DATETIME
            case "TIMESTAMP":
            case "TIMESTAMP(0)":
            case "TIMESTAMP(1)":
            case "TIMESTAMP(2)":
            case "TIMESTAMP(3)":
            case "TIMESTAMP(4)":
            case "TIMESTAMP(5)":
            case "TIMESTAMP(6)":
                int frac = 6;
                if(upperType.contains("(")) {
                    try {
                        frac = Integer.parseInt(upperType.substring(upperType.indexOf("(") + 1, upperType.indexOf(")")));
                        frac = Math.min(frac, 7); // SQL Server DATETIME2 precision 0-7
                    } catch (Exception e) { /* ignore */ }
                }
                return "DATETIME2(" + frac + ")";
            case "TIMESTAMP WITH TIME ZONE":
                frac = 7; // Default for DATETIMEOFFSET
                if(upperType.contains("(")) {
                    String partBeforeTZ = upperType.split(" WITH")[0];
                     if(partBeforeTZ.contains("(")) {
                         try {
                            frac = Integer.parseInt(partBeforeTZ.substring(partBeforeTZ.indexOf("(") + 1, partBeforeTZ.indexOf(")")));
                            frac = Math.min(frac, 7);
                         } catch (Exception e) { /* ignore */ }
                     }
                 }
                return "DATETIMEOFFSET(" + frac + ")";
            case "TIMESTAMP WITH LOCAL TIME ZONE":
                logger.warn("Oracle TIMESTAMP WITH LOCAL TIME ZONE mapped to DATETIMEOFFSET. SQL Server behavior depends on server/session settings for timezone.");
                // Map as TIMESTAMP WITH TIME ZONE
                 frac = 7;
                return "DATETIMEOFFSET(" + frac + ")";

            // Binary Types
            case "BLOB":
                return "VARBINARY(MAX)";
            case "RAW": // RAW(size)
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 8000) {
                        logger.warn("Oracle RAW({}) exceeds SQL Server VARBINARY(8000) limit, mapping to VARBINARY(MAX).", columnSize);
                        return "VARBINARY(MAX)";
                    }
                    return "VARBINARY(" + columnSize + ")";
                }
                return "VARBINARY(MAX)"; // Fallback
            case "LONG RAW": // Deprecated
                return "VARBINARY(MAX)";

            // Other
            case "ROWID":
            case "UROWID":
                // SQL Server has no direct equivalent. Can be stored as string if needed for reference.
                logger.info("Oracle ROWID/UROWID mapped to VARCHAR(40) for SQL Server. Not directly usable as row identifier.");
                return "VARCHAR(40)";
            case "XMLTYPE":
                return "XML"; // SQL Server has native XML type

            default:
                logger.warn("Unknown Oracle data type: {}. Defaulting to VARCHAR(MAX) for SQL Server.", upperType);
                return "VARCHAR(MAX)";
        }
    }
}
