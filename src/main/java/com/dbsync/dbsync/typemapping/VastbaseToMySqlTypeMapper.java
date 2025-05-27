package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Vastbase is PostgreSQL-like, so extends PostgresToMySqlTypeMapper
public class VastbaseToMySqlTypeMapper extends PostgresToMySqlTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(VastbaseToMySqlTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Vastbase (PG-like) to MySQL mapping for type: {}, size: {}, digits: {}",
            sourceColumnType, columnSize, decimalDigits);
        
        // Delegate to PostgresToMySqlTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        
        // Add any Vastbase-specific overrides here if they differ from PostgreSQL's mapping to MySQL.
        // For example, if Vastbase has a specific type that maps differently to MySQL
        // than its PostgreSQL equivalent would.
        // if (sourceColumnType.equalsIgnoreCase("VAST_UNIQUE_TYPE_FOR_MYSQL")) {
        //     mappedType = "MYSQL_EQUIVALENT_FOR_VAST_UNIQUE"; 
        // }

        logger.debug("Mapped to (via PostgresToMySql): {}", mappedType);
        return mappedType;
    }
}
