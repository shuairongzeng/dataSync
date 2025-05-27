package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Vastbase is PostgreSQL-like and Dameng is Oracle-like,
// so extends PostgresToOracleTypeMapper
public class VastbaseToDamengTypeMapper extends PostgresToOracleTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(VastbaseToDamengTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Vastbase (PG-like) to Dameng (Oracle-like) mapping for type: {}, size: {}, digits: {}",
            sourceColumnType, columnSize, decimalDigits);
        
        // Delegate to PostgresToOracleTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        
        // Add any Vastbase-to-Dameng specific overrides here if they differ from PostgreSQL-to-Oracle.
        // For example, if Vastbase has a specific type that maps differently to Dameng
        // than its PostgreSQL equivalent would map to Oracle.
        // if (sourceColumnType.equalsIgnoreCase("VAST_UNIQUE_TYPE_FOR_DAMENG")) {
        //     mappedType = "DAMENG_EQUIVALENT_FOR_VAST_UNIQUE"; 
        // }

        logger.debug("Mapped to (via PostgresToOracle): {}", mappedType);
        return mappedType;
    }
}
