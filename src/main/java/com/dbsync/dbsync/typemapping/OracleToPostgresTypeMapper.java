package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OracleToPostgresTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(OracleToPostgresTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("Oracle source column type is null, defaulting to TEXT.");
            return "TEXT";
        }
        String upperSourceColumnType = sourceColumnType.toUpperCase().trim();
        // Normalize DATA_LENGTH, DATA_PRECISION, DATA_SCALE from Oracle
        // For Oracle, DATA_LENGTH can be for CHARs, DATA_PRECISION/DATA_SCALE for NUMBERs
        // The 'columnSize' parameter here should correspond to DATA_LENGTH for chars,
        // and DATA_PRECISION for numbers. 'decimalDigits' corresponds to DATA_SCALE.

        switch (upperSourceColumnType) {
            // Character Types
            case "VARCHAR2":
            case "NVARCHAR2": // PostgreSQL VARCHAR handles Unicode
                if (columnSize != null && columnSize > 0) {
                    // Oracle's DATA_LENGTH for VARCHAR2 is in bytes by default for some setups,
                    // or characters. Assuming characters for simplicity, but this can be a nuance.
                    // PostgreSQL VARCHAR length is in characters.
                    // Multiplying by 2 as a simple heuristic if byte semantics were used,
                    // but it's better to rely on character length directly if available.
                    // For now, let's assume columnSize is character length.
                    return "VARCHAR(" + columnSize + ")";
                }
                return "TEXT"; // Default if no length specified
            case "CHAR":
            case "NCHAR":
                if (columnSize != null && columnSize > 0) {
                    return "CHAR(" + columnSize + ")";
                }
                return "CHAR(1)"; // Default if no length specified
            case "CLOB":
            case "NCLOB": // PostgreSQL TEXT handles large character objects
            case "LONG":  // Oracle LONG is deprecated, map to TEXT
                return "TEXT";

            // Numeric Types
            case "NUMBER":
                if (columnSize == null && decimalDigits == null) { // NUMBER without precision/scale
                    return "NUMERIC";
                }
                // NUMBER(p,s)
                if (decimalDigits != null && decimalDigits > 0) { // Has scale (decimal part)
                    // columnSize here is precision (p)
                    return "NUMERIC(" + (columnSize != null ? columnSize : 38) + "," + decimalDigits + ")";
                } else { // Integer types or NUMBER(p)
                    // decimalDigits is 0 or null
                    if (columnSize != null) { // NUMBER(p) or NUMBER(p,0)
                        if (columnSize <= 4) { // Approximation
                            return "SMALLINT"; // Up to 32767
                        } else if (columnSize <= 9) {
                            return "INTEGER";  // Up to 2,147,483,647
                        } else if (columnSize <= 18) {
                            return "BIGINT";   // Up to 9,223,372,036,854,775,807
                        } else {
                            return "NUMERIC(" + columnSize + ")"; // For very large integers
                        }
                    }
                    return "NUMERIC"; // If only NUMBER or NUMBER(*)
                }
            case "INTEGER":
            case "INT":
                return "INTEGER";
            case "SMALLINT":
                return "SMALLINT";
            case "FLOAT": // Oracle FLOAT(p) where p is binary precision
                // PostgreSQL REAL is single-precision, DOUBLE PRECISION is double-precision
                // Oracle FLOAT(p) where p < 24 maps to REAL, else DOUBLE PRECISION
                if (columnSize != null && columnSize < 24) { // columnSize here is binary precision
                    return "REAL";
                }
                return "DOUBLE PRECISION";
            case "BINARY_FLOAT": // Oracle 32-bit floating point
                return "REAL";
            case "BINARY_DOUBLE": // Oracle 64-bit floating point
                return "DOUBLE PRECISION";
            case "DECIMAL": // Oracle DECIMAL(p,s) is same as NUMBER(p,s)
            case "DEC":
            case "NUMERIC": // Standard SQL NUMERIC
                 if (columnSize != null && decimalDigits != null) {
                    return "NUMERIC(" + columnSize + "," + decimalDigits + ")";
                } else if (columnSize != null) {
                    return "NUMERIC(" + columnSize + ")";
                }
                return "NUMERIC";

            // Date and Time Types
            case "DATE": // Oracle DATE contains both date and time
                return "TIMESTAMP WITHOUT TIME ZONE";
            case "TIMESTAMP": // Usually TIMESTAMP(6) in Oracle
            case "TIMESTAMP(0)":
            case "TIMESTAMP(1)":
            case "TIMESTAMP(2)":
            case "TIMESTAMP(3)":
            case "TIMESTAMP(4)":
            case "TIMESTAMP(5)":
            case "TIMESTAMP(6)":
                return "TIMESTAMP WITHOUT TIME ZONE";
            case "TIMESTAMP WITH TIME ZONE":
                return "TIMESTAMP WITH TIME ZONE";
            case "TIMESTAMP WITH LOCAL TIME ZONE": // PostgreSQL handles this via session settings
                return "TIMESTAMP WITH TIME ZONE";

            // Binary Types
            case "BLOB":
            case "RAW": // RAW(size)
            case "LONG RAW": // Deprecated
                return "BYTEA";

            // Other types (less common for direct mapping, might need specific handling)
            case "ROWID":
            case "UROWID":
                return "VARCHAR(40)"; // Store as string, not directly usable as rowid in PG
            case "XMLTYPE":
                return "XML";
            // Oracle BFILE is an external file locator, no direct equivalent for storage.
            // Interval types (INTERVAL YEAR TO MONTH, INTERVAL DAY TO SECOND) have PG equivalents
            // but might need more complex parsing if precision/fractions are involved.

            default:
                logger.warn("Unknown Oracle data type: {}. Defaulting to TEXT.", upperSourceColumnType);
                return "TEXT"; // Fallback for unmapped types
        }
    }
}
