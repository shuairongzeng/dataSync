package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Vastbase is PostgreSQL-like, so extends PostgresToOracleTypeMapper
public class VastbaseToOracleTypeMapper extends PostgresToOracleTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(VastbaseToOracleTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Vastbase (PG-like) to Oracle mapping for type: {}, size: {}, digits: {}",
            sourceColumnType, columnSize, decimalDigits);
        
        // Delegate to PostgresToOracleTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        
        // Add any Vastbase-specific overrides here if they differ from PostgreSQL's mapping to Oracle.
        // For example, if Vastbase has a specific type that maps differently to Oracle
        // than its PostgreSQL equivalent would.
        // if (sourceColumnType.equalsIgnoreCase("VAST_UNIQUE_TYPE_FOR_ORACLE")) {
        //     mappedType = "ORACLE_EQUIVALENT_FOR_VAST_UNIQUE"; 
        // }

        logger.debug("Mapped to (via PostgresToOracle): {}", mappedType);
        return mappedType;
    }
}
