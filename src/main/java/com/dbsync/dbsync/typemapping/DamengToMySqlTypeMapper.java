package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Dameng is Oracle-like, so extends OracleToMySqlTypeMapper
public class DamengToMySqlTypeMapper extends OracleToMySqlTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(DamengToMySqlTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Dameng (Oracle-like) to MySQL mapping for type: {}, size: {}, digits: {}",
            sourceColumnType, columnSize, decimalDigits);
        
        // Delegate to OracleToMySqlTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        
        // Add any Dameng-specific overrides here if they differ from Oracle's mapping to MySQL
        // For example, if Dameng has a specific type that maps differently than Oracle's equivalent
        // if (sourceColumnType.equalsIgnoreCase("DM_SPECIFIC_TYPE")) {
        //     mappedType = "MYSQL_EQUIVALENT_FOR_DM_SPECIFIC"; 
        // }

        logger.debug("Mapped to (via OracleToMySql): {}", mappedType);
        return mappedType;
    }
}
