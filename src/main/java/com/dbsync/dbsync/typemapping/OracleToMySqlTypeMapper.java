package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OracleToMySqlTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(OracleToMySqlTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("Oracle source column type is null, defaulting to VARCHAR(255).");
            return "VARCHAR(255)";
        }
        String upperType = sourceColumnType.toUpperCase().trim();

        switch (upperType) {
            // Character Types
            case "VARCHAR2":
            case "NVARCHAR2":
                if (columnSize != null && columnSize > 0) {
                    // MySQL VARCHAR limit is 65,535 bytes (effective chars depend on charset)
                    // Oracle VARCHAR2 can be 4000 bytes/chars or 32767 bytes.
                    int mySqlLength = columnSize;
                    if (mySqlLength > 21844) { // Approx 65532 bytes / 3 bytes per char (for utf8mb3)
                         logger.warn("Oracle type {} ({}) may exceed MySQL VARCHAR length, mapping to TEXT.", upperType, columnSize);
                        return "TEXT";
                    }
                    return "VARCHAR(" + mySqlLength + ")";
                }
                return "TEXT"; // Default for unspecified length
            case "CHAR":
            case "NCHAR":
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 255) { // MySQL CHAR limit
                        logger.warn("Oracle type {} ({}) exceeds MySQL CHAR(255) limit, mapping to VARCHAR.", upperType, columnSize);
                         return "VARCHAR(" + columnSize + ")";
                    }
                    return "CHAR(" + columnSize + ")";
                }
                return "CHAR(1)";
            case "CLOB":
            case "NCLOB":
            case "LONG": // LONG is deprecated
                return "LONGTEXT"; // MySQL LONGTEXT for large character data

            // Numeric Types
            case "NUMBER":
                if (decimalDigits != null && decimalDigits > 0) { // Has scale (decimal part)
                    // NUMBER(p,s) -> DECIMAL(p,s)
                    // MySQL DECIMAL precision p up to 65, scale s up to 30
                    int p = (columnSize != null) ? Math.min(columnSize, 65) : 65; // Default precision 65 if not specified by Oracle NUMBER
                    int s = Math.min(decimalDigits, 30);
                    if (s > p) s = p; // Scale can't be > precision
                    return "DECIMAL(" + p + "," + s + ")";
                } else { // Integer types or NUMBER(p)
                    if (columnSize != null) { // NUMBER(p) or NUMBER(p,0)
                        if (columnSize <= 2) return "TINYINT";     // Approx up to 255 if unsigned
                        else if (columnSize <= 4) return "SMALLINT";  // Approx up to 65535 if unsigned
                        else if (columnSize <= 9) return "INT";       // Approx up to 4*10^9 if unsigned
                        else if (columnSize <= 18) return "BIGINT";   // Approx up to 1.8*10^19 if unsigned
                        else return "DECIMAL(" + Math.min(columnSize, 65) + ",0)"; // Large integers
                    }
                    // NUMBER without p,s -> typically a very large float or decimal
                    return "DECIMAL(65,10)"; // A general purpose decimal
                }
            case "INTEGER":
            case "INT":
                return "INT";
            case "SMALLINT":
                return "SMALLINT";
            case "FLOAT": // Oracle FLOAT(binary_precision)
                // MySQL FLOAT stores single-precision. DOUBLE stores double-precision.
                // Oracle FLOAT(p) where p < 24 maps to single precision
                if (columnSize != null && columnSize < 24) {
                    return "FLOAT";
                }
                return "DOUBLE"; // Oracle FLOAT(p) p >= 24 maps to double
            case "BINARY_FLOAT":
                return "FLOAT";
            case "BINARY_DOUBLE":
                return "DOUBLE";
            case "DEC": // ANSI types, map like NUMBER
            case "DECIMAL":
            case "NUMERIC":
                 int p = (columnSize != null) ? Math.min(columnSize, 65) : 65;
                 int s = (decimalDigits != null) ? Math.min(decimalDigits, 30) : 0;
                 if (s > p) s = p;
                 return "DECIMAL(" + p + "," + s + ")";

            // Date and Time Types
            case "DATE": // Oracle DATE contains date and time
                return "DATETIME"; // MySQL DATETIME stores both
            case "TIMESTAMP": // Oracle TIMESTAMP(frac_seconds_precision)
            case "TIMESTAMP(0)":
            case "TIMESTAMP(1)":
            case "TIMESTAMP(2)":
            case "TIMESTAMP(3)":
            case "TIMESTAMP(4)":
            case "TIMESTAMP(5)":
            case "TIMESTAMP(6)":
                // MySQL TIMESTAMP or DATETIME can have fractional seconds up to 6 digits
                int frac = 6; // default
                if(upperType.contains("(")) {
                    try {
                        frac = Integer.parseInt(upperType.substring(upperType.indexOf("(") + 1, upperType.indexOf(")")));
                        frac = Math.min(frac, 6); // Cap at MySQL's max
                    } catch (Exception e) { /* ignore, use default */ }
                }
                return "DATETIME(" + frac + ")";
            case "TIMESTAMP WITH TIME ZONE":
                // MySQL TIMESTAMP stores in UTC and converts. DATETIME does not store TZ.
                logger.warn("Oracle TIMESTAMP WITH TIME ZONE mapped to MySQL TIMESTAMP. Timezone behavior differs; MySQL converts to/from UTC based on session timezone.");
                 frac = 6;
                 if(upperType.contains("(")) { // e.g. TIMESTAMP(9) WITH TIME ZONE
                    String partBeforeTZ = upperType.split(" WITH")[0];
                     if(partBeforeTZ.contains("(")) {
                         try {
                            frac = Integer.parseInt(partBeforeTZ.substring(partBeforeTZ.indexOf("(") + 1, partBeforeTZ.indexOf(")")));
                            frac = Math.min(frac, 6);
                         } catch (Exception e) { /* ignore */ }
                     }
                 }
                return "TIMESTAMP(" + frac + ")";
            case "TIMESTAMP WITH LOCAL TIME ZONE":
                 logger.warn("Oracle TIMESTAMP WITH LOCAL TIME ZONE mapped to MySQL TIMESTAMP. MySQL handles this via session timezone settings.");
                 frac = 6; // Similar logic as above to extract fractional seconds if present
                 // Simplified: assume it's like TIMESTAMP WITH TIME ZONE for MySQL target
                return "TIMESTAMP(" + frac + ")";


            // Binary Types
            case "BLOB":
                return "LONGBLOB"; // For large binary data
            case "RAW": // RAW(size)
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 65535) {
                        logger.warn("Oracle RAW({}) exceeds MySQL VARBINARY(65535) limit, mapping to BLOB.", columnSize);
                        return "BLOB";
                    }
                    return "VARBINARY(" + columnSize + ")";
                }
                return "BLOB"; // fallback
            case "LONG RAW": // Deprecated
                return "LONGBLOB";

            // Other
            case "ROWID":
            case "UROWID":
                logger.info("Oracle ROWID/UROWID mapped to VARCHAR(40) for MySQL. Not directly usable as row identifier.");
                return "VARCHAR(40)";
            case "XMLTYPE":
                return "LONGTEXT"; // Store XML as text

            default:
                logger.warn("Unknown Oracle data type: {}. Defaulting to TEXT for MySQL.", upperType);
                return "TEXT";
        }
    }
}
