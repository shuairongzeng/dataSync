package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Dameng is Oracle-like, so extends SqlServerToOracleTypeMapper
public class SqlServerToDamengTypeMapper extends SqlServerToOracleTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(SqlServerToDamengTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("SQL Server to Dameng (Oracle-like) mapping for type: {}, size: {}, digits: {}",
            sourceColumnType, columnSize, decimalDigits);
        
        // Delegate to SqlServerToOracleTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        
        // Add any Dameng-specific overrides here if they differ from Oracle
        // For example, if Dameng has different LOB type names or size limits
        // if (mappedType.equals("CLOB")) {
        //     mappedType = "DM_CLOB_TYPE"; // Example override
        // }

        logger.debug("Mapped to (via SqlServerToOracle): {}", mappedType);
        return mappedType;
    }
}
