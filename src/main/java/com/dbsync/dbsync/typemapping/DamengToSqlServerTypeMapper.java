package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Dameng is Oracle-like, so extends OracleToSqlServerTypeMapper
public class DamengToSqlServerTypeMapper extends OracleToSqlServerTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(DamengToSqlServerTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Dameng (Oracle-like) to SQL Server mapping for type: {}, size: {}, digits: {}",
            sourceColumnType, columnSize, decimalDigits);
        
        // Delegate to OracleToSqlServerTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        
        // Add any Dameng-specific overrides here if they differ from Oracle's mapping to SQL Server
        // For example, if Dameng has a specific type that maps differently than Oracle's equivalent
        // if (sourceColumnType.equalsIgnoreCase("DM_SPECIFIC_TYPE_FOR_SQLSERVER")) {
        //     mappedType = "SQLSERVER_EQUIVALENT_FOR_DM_SPECIFIC"; 
        // }

        logger.debug("Mapped to (via OracleToSqlServer): {}", mappedType);
        return mappedType;
    }
}
