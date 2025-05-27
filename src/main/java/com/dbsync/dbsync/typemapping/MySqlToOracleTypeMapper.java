package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MySqlToOracleTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(MySqlToOracleTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("MySQL source column type is null, defaulting to VARCHAR2(255) for Oracle.");
            return "VARCHAR2(255)";
        }
        String upperType = sourceColumnType.toUpperCase().trim();

        switch (upperType) {
            // Character String Types
            case "VARCHAR":
                if (columnSize != null && columnSize > 0) {
                    // Oracle VARCHAR2 max 4000 bytes/chars (or 32767 with MAX_STRING_SIZE=EXTENDED)
                    int oraLength = columnSize;
                    if (oraLength > 4000 && oraLength <= 32767) {
                         logger.info("MySQL VARCHAR({}) mapped to VARCHAR2({}). Ensure Oracle MAX_STRING_SIZE=EXTENDED if length > 4000 bytes.", columnSize, oraLength);
                         // Potentially requires extended string size in Oracle.
                    } else if (oraLength > 32767) {
                        logger.warn("MySQL VARCHAR({}) exceeds Oracle VARCHAR2 max (32767), mapping to CLOB.", columnSize);
                        return "CLOB";
                    }
                    return "VARCHAR2(" + oraLength + ")";
                }
                return "VARCHAR2(255)"; // Default
            case "CHAR":
                if (columnSize != null && columnSize > 0) {
                    // Oracle CHAR max 2000 bytes
                    if (columnSize > 2000) {
                        logger.warn("MySQL CHAR({}) exceeds Oracle CHAR(2000) limit, mapping to VARCHAR2.", columnSize);
                        return "VARCHAR2(" + columnSize + ")";
                    }
                    return "CHAR(" + columnSize + ")";
                }
                return "CHAR(1)";
            case "TINYTEXT": // Max 255 bytes
                return "VARCHAR2(255)";
            case "TEXT": // Max 65,535 bytes
                // If MAX_STRING_SIZE=STANDARD, map to CLOB. If EXTENDED, VARCHAR2(32767) might fit.
                logger.info("MySQL TEXT mapped to CLOB for Oracle. Consider VARCHAR2(32767) if MAX_STRING_SIZE=EXTENDED and content fits.");
                return "CLOB"; // Safest bet
            case "MEDIUMTEXT": // Max 16,777,215 bytes
            case "LONGTEXT": // Max 4,294,967,295 bytes
                return "CLOB";
            case "ENUM":
            case "SET":
                logger.info("MySQL ENUM/SET type '{}' mapped to VARCHAR2(255) for Oracle. Constraints must be handled by application.", upperType);
                return "VARCHAR2(255)"; // Or based on max possible length of enum/set values

            // Numeric Types
            case "TINYINT": // MySQL TINYINT can be -128 to 127 (signed) or 0 to 255 (unsigned)
                return "NUMBER(3)"; // Sufficient for signed/unsigned TINYINT
            case "SMALLINT": // MySQL SMALLINT -32768 to 32767 (signed) or 0 to 65535 (unsigned)
                return "NUMBER(5)";
            case "MEDIUMINT": // MySQL MEDIUMINT -8,388,608 to 8,388,607 (signed) or 0 to 16,777,215 (unsigned)
                return "NUMBER(8)";
            case "INT":
            case "INTEGER": // MySQL INT -2,147,483,648 to 2,147,483,647 (signed) or 0 to 4,294,967,295 (unsigned)
                return "NUMBER(10)"; // Or INTEGER if Oracle INTEGER type is preferred
            case "BIGINT": // MySQL BIGINT +/-9*10^18
                return "NUMBER(19)"; // Oracle NUMBER(19) can hold this
            case "FLOAT": // MySQL FLOAT(p) - single precision
                return "BINARY_FLOAT";
            case "DOUBLE":
            case "DOUBLE PRECISION": // MySQL DOUBLE - double precision
                return "BINARY_DOUBLE";
            case "DECIMAL":
            case "DEC":
            case "NUMERIC":
                // MySQL DECIMAL(p,s), p up to 65, s up to 30. Oracle NUMBER(p,s) p up to 38.
                int p = (columnSize != null) ? Math.min(columnSize, 38) : 38; // Cap precision at Oracle's max
                int s = (decimalDigits != null) ? decimalDigits : 0;
                if (s > p) s = p; // Scale cannot be larger than precision
                if (s < -84) s = -84; // Oracle scale limits
                if (s > 127) s = 127;
                return "NUMBER(" + p + "," + s + ")";
            case "BIT": // BIT(M) in MySQL
                // Oracle doesn't have a direct BIT type. Map to RAW or NUMBER.
                if (columnSize != null && columnSize == 1) return "NUMBER(1)"; // Often for boolean
                logger.info("MySQL BIT({}) mapped to RAW({}). Max M is 64 for MySQL.", columnSize, (columnSize + 7) / 8);
                return "RAW(" + ((columnSize != null ? Math.min(columnSize, 64) : 1) + 7) / 8 + ")"; // Store as bytes

            // Date and Time Types
            case "DATE":
                return "DATE"; // Oracle DATE includes time, time will be 00:00:00
            case "DATETIME": // MySQL DATETIME 'YYYY-MM-DD HH:MM:SS[.ffffff]'
                // columnSize for MySQL DATETIME(fsp) is fsp (0-6)
                int frac = (columnSize != null) ? Math.min(columnSize, 6) : 0;
                return "TIMESTAMP(" + frac + ")";
            case "TIMESTAMP": // MySQL TIMESTAMP 'YYYY-MM-DD HH:MM:SS[.ffffff]' (UTC stored/retrieved)
                // columnSize for MySQL TIMESTAMP(fsp) is fsp (0-6)
                frac = (columnSize != null) ? Math.min(columnSize, 6) : 0;
                logger.warn("MySQL TIMESTAMP mapped to Oracle TIMESTAMP WITH LOCAL TIME ZONE. Verify timezone behavior.");
                return "TIMESTAMP(" + frac + ") WITH LOCAL TIME ZONE"; // Oracle equivalent for UTC behavior
            case "TIME": // MySQL TIME 'HHH:MM:SS[.ffffff]'
                // columnSize for MySQL TIME(fsp) is fsp (0-6)
                // Oracle has no TIME-only type. Map to VARCHAR2 or INTERVAL DAY TO SECOND.
                logger.warn("MySQL TIME mapped to Oracle INTERVAL DAY(0) TO SECOND({}). Check fractional precision.", columnSize != null ? columnSize : 0);
                 // Precision of interval is for seconds, max 9. MySQL TIME(p) p is fractional seconds.
                frac = (columnSize != null) ? Math.min(columnSize, 9) : 0;
                return "INTERVAL DAY(0) TO SECOND(" + frac + ")";
            case "YEAR": // MySQL YEAR(4) 'YYYY' or YEAR(2) 'YY'
                return "NUMBER(4)"; // Store as number

            // Binary String Types
            case "BINARY": // Fixed length
                if (columnSize != null && columnSize > 0) {
                    if (columnSize > 2000) {
                        logger.warn("MySQL BINARY({}) exceeds Oracle RAW(2000) limit, mapping to BLOB.", columnSize);
                        return "BLOB";
                    }
                    return "RAW(" + columnSize + ")";
                }
                return "RAW(1)";
            case "VARBINARY": // Variable length
                if (columnSize != null && columnSize > 0) {
                     if (columnSize > 4000) { // Assuming standard string size for RAW
                        logger.warn("MySQL VARBINARY({}) may exceed Oracle RAW(4000) limit with MAX_STRING_SIZE=STANDARD, mapping to BLOB.", columnSize);
                        return "BLOB"; // Safest if > 2000 (RAW limit) or > 4000 (VARCHAR2-like limit for RAW)
                    }
                    // Oracle RAW can go up to 32767 with MAX_STRING_SIZE=EXTENDED, but that's not typical.
                    // Sticking to more common limit for RAW.
                    return "RAW(" + Math.min(columnSize, 2000) + ")";
                }
                return "BLOB";
            case "TINYBLOB": // Max 255 bytes
            case "BLOB": // Max 65,535 bytes
            case "MEDIUMBLOB": // Max 16,777,215 bytes
            case "LONGBLOB": // Max 4,294,967,295 bytes
                return "BLOB";

            // Spatial Types (GEOMETRY, POINT, etc.)
            // Oracle Spatial required for these. Default to CLOB/BLOB for storage.
            case "GEOMETRY": case "POINT": case "LINESTRING": case "POLYGON":
            case "MULTIPOINT": case "MULTILINESTRING": case "MULTIPOLYGON": case "GEOMETRYCOLLECTION":
                logger.warn("MySQL spatial type {} mapped to BLOB for Oracle. Requires Oracle Spatial for functionality.", upperType);
                return "BLOB"; // Store raw geometry data

            // JSON Type
            case "JSON": // MySQL JSON type
                logger.info("MySQL JSON mapped to CLOB for Oracle. Consider native JSON type if Oracle 12c R2+ with MAX_STRING_SIZE=EXTENDED or Oracle 21c+.");
                return "CLOB"; // Oracle 12c+ can store JSON in CLOB/VARCHAR2/BLOB. Native JSON type in 21c+.

            default:
                logger.warn("Unknown MySQL data type: {}. Defaulting to VARCHAR2(255) for Oracle.", upperType);
                return "VARCHAR2(255)";
        }
    }
}
