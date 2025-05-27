package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Vastbase to Vastbase type mapping (identity or near-identity).
 * Assumes Vastbase is highly PostgreSQL-compatible, so extends PostgresToPostgresTypeMapper.
 */
class VastbaseToVastbaseTypeMapper extends PostgresToPostgresTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(VastbaseToVastbaseTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Vastbase to Vastbase mapping for type: {}, size: {}, digits: {}", 
            sourceColumnType, columnSize, decimalDigits);

        // Delegate to PostgresToPostgresTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);

        // Add any Vastbase-specific identity overrides if they differ from PostgreSQL's identity.
        // For example, if Vastbase has a specific type that needs explicit self-mapping.
        // if (sourceColumnType.equalsIgnoreCase("VAST_UNIQUE_SELF_TYPE")) {
        //     mappedType = "VAST_UNIQUE_SELF_TYPE"; 
        // }
        
        logger.debug("Mapped to (via PostgresToPostgres): {}", mappedType);
        return mappedType;
    }
}
