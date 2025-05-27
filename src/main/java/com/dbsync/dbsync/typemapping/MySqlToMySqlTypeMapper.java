package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles MySQL to MySQL type mapping (identity or near-identity).
 */
class MySqlToMySqlTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(MySqlToMySqlTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            logger.warn("MySQL source column type is null. Returning as is for MySQL target.");
            return null;
        }
        String upperType = sourceColumnType.toUpperCase();

        // Most MySQL types map to themselves.
        // This mapper primarily ensures correct formatting for types with parameters.
        switch (upperType) {
            case "VARCHAR":
                return columnSize != null ? "VARCHAR(" + columnSize + ")" : "VARCHAR(255)"; // Default size
            case "CHAR":
                return columnSize != null ? "CHAR(" + columnSize + ")" : "CHAR(1)";
            case "TINYTEXT":
            case "TEXT":
            case "MEDIUMTEXT":
            case "LONGTEXT":
                return upperType;
            case "ENUM": // ENUM('val1','val2',...) - this mapper doesn't know the values, pass as is
            case "SET":  // SET('val1','val2',...)
                logger.info("MySQL ENUM/SET type {} passed through. Definition including values must be handled by DDL generation.", upperType);
                return sourceColumnType; // Pass the original definition

            case "TINYINT":
            case "SMALLINT":
            case "MEDIUMINT":
            case "INT":
            case "INTEGER":
            case "BIGINT":
                // Display width for integers (e.g., INT(11)) is usually metadata, not storage related.
                // Pass through type name, DDL generation might add display width if needed.
                return upperType;
            case "FLOAT": // FLOAT(p) or FLOAT(M,D)
            case "DOUBLE": // DOUBLE(M,D)
            case "DOUBLE PRECISION":
            case "DECIMAL": // DECIMAL(M,D)
            case "DEC":
            case "NUMERIC":
                if (columnSize != null && decimalDigits != null) {
                    return upperType + "(" + columnSize + "," + decimalDigits + ")";
                } else if (columnSize != null) {
                    return upperType + "(" + columnSize + ")";
                }
                return upperType; // e.g. FLOAT, DECIMAL
            case "BIT": // BIT(M)
                return columnSize != null ? "BIT(" + columnSize + ")" : "BIT(1)";

            case "DATE":
                return "DATE";
            case "DATETIME": // DATETIME(fsp)
            case "TIMESTAMP": // TIMESTAMP(fsp)
            case "TIME": // TIME(fsp)
                if (decimalDigits != null) { // decimalDigits might be used for fsp if columnSize is not
                    return upperType + "(" + decimalDigits + ")";
                }
                 // columnSize might also be used for fsp, depends on how it's passed.
                 // Assuming decimalDigits is the fractional seconds precision here based on previous mappers.
                return upperType;
            case "YEAR": // YEAR or YEAR(4)
                return "YEAR";

            case "BINARY":
            case "VARBINARY":
                return columnSize != null ? upperType + "(" + columnSize + ")" : upperType + "(1)"; // Default size for BINARY(1)
            case "TINYBLOB":
            case "BLOB":
            case "MEDIUMBLOB":
            case "LONGBLOB":
                return upperType;

            case "GEOMETRY":
            case "POINT":
            case "LINESTRING":
            case "POLYGON":
            case "MULTIPOINT":
            case "MULTILINESTRING":
            case "MULTIPOLYGON":
            case "GEOMETRYCOLLECTION":
                return upperType; // Spatial types

            case "JSON":
                return "JSON";

            default:
                logger.warn("MySqlToMySqlTypeMapper: Unknown MySQL type '{}', passing through.", sourceColumnType);
                return sourceColumnType;
        }
    }
}
