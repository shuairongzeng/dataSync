package com.dbsync.dbsync.typemapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TypeMappingRegistry {

    private static final String DEFAULT_MAPPER_KEY = "default";
    private final Map<String, TypeMapper> mappers = new HashMap<>();

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TypeMappingRegistry {

    private static final Logger logger = LoggerFactory.getLogger(TypeMappingRegistry.class);
    private static final String DEFAULT_MAPPER_KEY = "default";
    private final Map<String, TypeMapper> mappers = new HashMap<>();

    public TypeMappingRegistry() {
        // Initialize with a default mapper that provides a basic fallback
        registerMapper(DEFAULT_MAPPER_KEY, DEFAULT_MAPPER_KEY, new DefaultTypeMapper());
        
        // Register specific mappers
        registerMapper("oracle", "postgresql", new OracleToPostgresTypeMapper());
        registerMapper("mysql", "postgresql", new MySqlToPostgresTypeMapper());
        registerMapper("sqlserver", "postgresql", new SqlServerToPostgresTypeMapper());
        registerMapper("postgresql", "oracle", new PostgresToOracleTypeMapper());
        registerMapper("postgresql", "mysql", new PostgresToMySqlTypeMapper());
        registerMapper("postgresql", "sqlserver", new PostgresToSqlServerTypeMapper());

        // Dameng mappers (assuming similar to Oracle for now)
        registerMapper("dameng", "postgresql", new DamengToPostgresTypeMapper());
        registerMapper("postgresql", "dameng", new PostgresToDamengTypeMapper());

        // Vastbase mappers (assuming similar to PostgreSQL for now)
        registerMapper("vastbase", "postgresql", new VastbaseToPostgresTypeMapper());
        registerMapper("postgresql", "vastbase", new PostgresToVastbaseTypeMapper());

        // Oracle to X mappers (PostgreSQL already done)
        registerMapper("oracle", "mysql", new OracleToMySqlTypeMapper());
        registerMapper("oracle", "sqlserver", new OracleToSqlServerTypeMapper());
        registerMapper("oracle", "dameng", new OracleToDamengTypeMapper()); // Assumes Dameng is Oracle-like
        registerMapper("oracle", "vastbase", new OracleToVastbaseTypeMapper()); // Assumes Vastbase is PG-like, uses OracleToPostgres

        // MySQL to X mappers (PostgreSQL already done)
        registerMapper("mysql", "oracle", new MySqlToOracleTypeMapper());
        registerMapper("mysql", "sqlserver", new MySqlToSqlServerTypeMapper());
        registerMapper("mysql", "dameng", new MySqlToDamengTypeMapper()); // Assumes Dameng is Oracle-like
        registerMapper("mysql", "vastbase", new MySqlToVastbaseTypeMapper()); // Assumes Vastbase is PG-like

        // SQLServer to X mappers (PostgreSQL already done)
        registerMapper("sqlserver", "oracle", new SqlServerToOracleTypeMapper());
        registerMapper("sqlserver", "mysql", new SqlServerToMySqlTypeMapper());
        registerMapper("sqlserver", "dameng", new SqlServerToDamengTypeMapper()); // Assumes Dameng is Oracle-like
        registerMapper("sqlserver", "vastbase", new SqlServerToVastbaseTypeMapper()); // Assumes Vastbase is PG-like

        // Dameng to X mappers (PostgreSQL already done, Oracle-like for Dameng itself)
        registerMapper("dameng", "mysql", new DamengToMySqlTypeMapper());
        registerMapper("dameng", "sqlserver", new DamengToSqlServerTypeMapper());
        registerMapper("dameng", "vastbase", new DamengToVastbaseTypeMapper()); // Dameng (Ora) to Vastbase (PG)

        // Vastbase to X mappers (PostgreSQL already done, PG-like for Vastbase itself)
        registerMapper("vastbase", "oracle", new VastbaseToOracleTypeMapper());
        registerMapper("vastbase", "mysql", new VastbaseToMySqlTypeMapper());
        registerMapper("vastbase", "sqlserver", new VastbaseToSqlServerTypeMapper());
        registerMapper("vastbase", "dameng", new VastbaseToDamengTypeMapper()); // Vastbase (PG) to Dameng (Ora)

        // Identity Mappers
        registerMapper("postgresql", "postgresql", new PostgresToPostgresTypeMapper());
        registerMapper("oracle", "oracle", new OracleToOracleTypeMapper());
        registerMapper("mysql", "mysql", new MySqlToMySqlTypeMapper());
        registerMapper("sqlserver", "sqlserver", new SqlServerToSqlServerTypeMapper());
        registerMapper("dameng", "dameng", new DamengToDamengTypeMapper()); // Assumes Dameng is Oracle-like
        registerMapper("vastbase", "vastbase", new VastbaseToVastbaseTypeMapper()); // Assumes Vastbase is PG-like
        // Future mappers will be registered here:
    }

    /**
     * Registers a specific TypeMapper for a given source and target database combination.
     *
     * @param sourceDbType The source database type (e.g., "oracle", "mysql").
     * @param targetDbType The target database type (e.g., "postgresql", "oracle").
     * @param typeMapper   The TypeMapper implementation for this combination.
     */
    public void registerMapper(String sourceDbType, String targetDbType, TypeMapper typeMapper) {
        Objects.requireNonNull(sourceDbType, "sourceDbType cannot be null");
        Objects.requireNonNull(targetDbType, "targetDbType cannot be null");
        Objects.requireNonNull(typeMapper, "typeMapper cannot be null");
        mappers.put(getKey(sourceDbType, targetDbType), typeMapper);
    }

    /**
     * Maps a source column type to a target database column type.
     *
     * @param sourceColumnType The name of the source column type (e.g., "VARCHAR2", "NUMBER").
     * @param columnSize       The size of the column (e.g., for VARCHAR(255), this would be 255). Can be null.
     * @param decimalDigits    The number of decimal digits for numeric types (scale). Can be null.
     * @param sourceDbType     The source database type (e.g., "oracle", "mysql").
     * @param targetDbType     The target database type (e.g., "postgresql", "oracle").
     * @return The corresponding column type definition string for the target database.
     */
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits, String sourceDbType, String targetDbType) {
        Objects.requireNonNull(sourceColumnType, "sourceColumnType cannot be null");
        Objects.requireNonNull(sourceDbType, "sourceDbType cannot be null");
        Objects.requireNonNull(targetDbType, "targetDbType cannot be null");

        TypeMapper specificMapper = mappers.get(getKey(sourceDbType, targetDbType));

        if (specificMapper != null) {
            return specificMapper.mapType(sourceColumnType, columnSize, decimalDigits);
        }

        // If no specific mapper, try a source-generic mapper (e.g. any source to PostgreSQL)
        TypeMapper sourceGenericMapper = mappers.get(getKey(DEFAULT_MAPPER_KEY, targetDbType));
        if (sourceGenericMapper != null) {
            // This might need more context or a different interface if type names vary too much
            // For now, assuming the sourceColumnType is somewhat standard or the mapper handles it
            return sourceGenericMapper.mapType(sourceColumnType, columnSize, decimalDigits);
        }
        
        // Fallback to a completely default mapper
        TypeMapper defaultMapper = mappers.get(getKey(DEFAULT_MAPPER_KEY, DEFAULT_MAPPER_KEY));
        logger.warn("Warning: No specific type mapper found for {} to {} for type {}. Using default fallback.",
                sourceDbType, targetDbType, sourceColumnType);
        return defaultMapper.mapType(sourceColumnType, columnSize, decimalDigits);
    }

    private String getKey(String sourceDbType, String targetDbType) {
        return sourceDbType.toLowerCase() + "2" + targetDbType.toLowerCase();
    }

    /**
     * Default TypeMapper implementation. Provides very basic fallback mappings.
     */
    private static class DefaultTypeMapper implements TypeMapper {
        @Override
        public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
            logger.debug("DefaultTypeMapper: Mapping type {} with size {} and decimalDigits {}",
                    sourceColumnType, columnSize, decimalDigits);

            String upperType = sourceColumnType.toUpperCase();

            if (upperType.contains("CHAR") || upperType.contains("TEXT") || upperType.contains("STRING")) {
                if (columnSize != null && columnSize > 0 && columnSize < 4000) { // Common general limit
                    return "VARCHAR(" + columnSize + ")";
                }
                return "TEXT"; // Default for unspecified length or very large strings
            }
            if (upperType.contains("INT")) { // Catches INTEGER, BIGINT, SMALLINT, TINYINT
                if (upperType.contains("BIG")) return "BIGINT";
                if (upperType.contains("SMALL")) return "SMALLINT";
                return "INTEGER";
            }
            if (upperType.contains("NUMERIC") || upperType.contains("DECIMAL") || upperType.contains("NUMBER")) {
                if (columnSize != null && decimalDigits != null) {
                    return "NUMERIC(" + columnSize + "," + decimalDigits + ")";
                } else if (columnSize != null) {
                    return "NUMERIC(" + columnSize + ")";
                }
                return "NUMERIC"; // Default precision
            }
            if (upperType.contains("FLOAT") || upperType.contains("REAL")) {
                return "REAL"; // Single precision
            }
            if (upperType.contains("DOUBLE")) {
                return "DOUBLE PRECISION";
            }
            if (upperType.contains("DATE") || upperType.contains("TIME")) { // Catches DATETIME, TIMESTAMP
                return "TIMESTAMP WITHOUT TIME ZONE"; // A common generic representation
            }
            if (upperType.contains("BLOB") || upperType.contains("BINARY") || upperType.contains("BYTEA")) {
                return "BYTEA"; // PostgreSQL specific, but common for binary data
            }
            if (upperType.contains("BOOL")) {
                return "BOOLEAN";
            }
            // Fallback for truly unknown types
            logger.warn("Warning: DefaultTypeMapper could not determine a specific mapping for {}. Defaulting to TEXT.", sourceColumnType);
            return "TEXT";
        }
    }
}
