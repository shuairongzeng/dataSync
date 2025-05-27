package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Dameng to Dameng type mapping (identity or near-identity).
 * Assumes Dameng is highly Oracle-compatible, so extends OracleToOracleTypeMapper.
 */
class DamengToDamengTypeMapper extends OracleToOracleTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(DamengToDamengTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Dameng to Dameng mapping for type: {}, size: {}, digits: {}", 
            sourceColumnType, columnSize, decimalDigits);

        // Delegate to OracleToOracleTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);

        // Add any Dameng-specific identity overrides if they differ from Oracle's identity.
        // For example, if Dameng has a specific type that needs explicit self-mapping.
        // if (sourceColumnType.equalsIgnoreCase("DM_UNIQUE_SELF_TYPE")) {
        //     mappedType = "DM_UNIQUE_SELF_TYPE"; 
        // }
        
        logger.debug("Mapped to (via OracleToOracle): {}", mappedType);
        return mappedType;
    }
}
