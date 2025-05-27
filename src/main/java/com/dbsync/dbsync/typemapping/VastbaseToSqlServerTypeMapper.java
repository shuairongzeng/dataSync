package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Vastbase is PostgreSQL-like, so extends PostgresToSqlServerTypeMapper
public class VastbaseToSqlServerTypeMapper extends PostgresToSqlServerTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(VastbaseToSqlServerTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Vastbase (PG-like) to SQL Server mapping for type: {}, size: {}, digits: {}",
            sourceColumnType, columnSize, decimalDigits);
        
        // Delegate to PostgresToSqlServerTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        
        // Add any Vastbase-specific overrides here if they differ from PostgreSQL's mapping to SQL Server.
        // For example, if Vastbase has a specific type that maps differently to SQL Server
        // than its PostgreSQL equivalent would.
        // if (sourceColumnType.equalsIgnoreCase("VAST_UNIQUE_TYPE_FOR_SQLSERVER")) {
        //     mappedType = "SQLSERVER_EQUIVALENT_FOR_VAST_UNIQUE"; 
        // }

        logger.debug("Mapped to (via PostgresToSqlServer): {}", mappedType);
        return mappedType;
    }
}
