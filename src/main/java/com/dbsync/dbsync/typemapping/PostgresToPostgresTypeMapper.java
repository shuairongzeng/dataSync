package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal helper class for PostgreSQL to PostgreSQL mapping.
 * This is effectively an identity or near-identity mapper for standard PG types.
 */
class PostgresToPostgresTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(PostgresToPostgresTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        String upperType = sourceColumnType.toUpperCase();
        // Basic pass-through for common types, can be expanded
        switch (upperType) {
            case "VARCHAR":
                return columnSize != null ? "VARCHAR(" + columnSize + ")" : "TEXT";
            case "CHAR":
            case "BPCHAR":
                return columnSize != null ? "CHAR(" + columnSize + ")" : "CHAR(1)";
            case "TEXT":
            case "NAME":
                return "TEXT";
            case "SMALLINT":
            case "INT2":
                return "SMALLINT";
            case "INTEGER":
            case "INT":
            case "INT4":
                return "INTEGER";
            case "BIGINT":
            case "INT8":
                return "BIGINT";
            case "NUMERIC":
            case "DECIMAL":
                if (columnSize != null && decimalDigits != null) {
                    return "NUMERIC(" + columnSize + ", " + decimalDigits + ")";
                } else if (columnSize != null) {
                    return "NUMERIC(" + columnSize + ")";
                }
                return "NUMERIC";
            case "REAL":
            case "FLOAT4":
                return "REAL";
            case "DOUBLE PRECISION":
            case "FLOAT8":
                return "DOUBLE PRECISION";
            case "MONEY":
                return "MONEY";
            case "DATE":
                return "DATE";
            case "TIME":
            case "TIME WITHOUT TIME ZONE":
                return columnSize != null ? "TIME(" + columnSize + ") WITHOUT TIME ZONE" : "TIME WITHOUT TIME ZONE";
            case "TIMETZ":
            case "TIME WITH TIME ZONE":
                 return columnSize != null ? "TIME(" + columnSize + ") WITH TIME ZONE" : "TIME WITH TIME ZONE";
            case "TIMESTAMP":
            case "TIMESTAMP WITHOUT TIME ZONE":
                 return columnSize != null ? "TIMESTAMP(" + columnSize + ") WITHOUT TIME ZONE" : "TIMESTAMP WITHOUT TIME ZONE";
            case "TIMESTAMPTZ":
            case "TIMESTAMP WITH TIME ZONE":
                return columnSize != null ? "TIMESTAMP(" + columnSize + ") WITH TIME ZONE" : "TIMESTAMP WITH TIME ZONE";
            case "INTERVAL":
                return "INTERVAL";
            case "BOOLEAN":
            case "BOOL":
                return "BOOLEAN";
            case "BYTEA":
                return "BYTEA";
            case "CIDR":
                return "CIDR";
            case "INET":
                return "INET";
            case "MACADDR":
                return "MACADDR";
            case "MACADDR8":
                return "MACADDR8";
            case "UUID":
                return "UUID";
            case "JSON":
                return "JSON";
            case "JSONB":
                return "JSONB";
            case "XML":
                return "XML";
            // For geometric types, PostGIS also uses these names.
            case "POINT": return "POINT";
            case "LINE": return "LINE";
            case "LSEG": return "LSEG";
            case "BOX": return "BOX";
            case "PATH": return "PATH";
            case "POLYGON": return "POLYGON";
            case "CIRCLE": return "CIRCLE";
            default:
                if (upperType.endsWith("[]")) {
                    logger.info("Passing through PostgreSQL array type {} as is.", upperType);
                    return upperType; // Assumes direct compatibility for arrays
                }
                logger.warn("PostgresToPostgresTypeMapper: Unknown PostgreSQL type '{}', passing through.", sourceColumnType);
                return sourceColumnType; // Pass through if unknown
        }
    }
}
