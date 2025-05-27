package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Dameng is Oracle-like, so extends MySqlToOracleTypeMapper
public class MySqlToDamengTypeMapper extends MySqlToOracleTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(MySqlToDamengTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("MySQL to Dameng (Oracle-like) mapping for type: {}, size: {}, digits: {}",
            sourceColumnType, columnSize, decimalDigits);
        
        // Delegate to MySqlToOracleTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        
        // Add any Dameng-specific overrides here if they differ from Oracle
        // For example, if Dameng has different LOB type names or size limits
        // if (mappedType.equals("CLOB")) {
        //     mappedType = "DM_CLOB_TYPE"; // Example override
        // }

        logger.debug("Mapped to (via MySqlToOracle): {}", mappedType);
        return mappedType;
    }
}
