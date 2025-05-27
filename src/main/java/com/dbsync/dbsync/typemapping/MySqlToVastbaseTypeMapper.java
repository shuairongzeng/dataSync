package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Vastbase is PostgreSQL-like, so extends MySqlToPostgresTypeMapper
public class MySqlToVastbaseTypeMapper extends MySqlToPostgresTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(MySqlToVastbaseTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("MySQL to Vastbase (PG-like) mapping for type: {}, size: {}, digits: {}",
            sourceColumnType, columnSize, decimalDigits);
        
        // Delegate to MySqlToPostgresTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        
        // Add any Vastbase-specific overrides here if they differ from PostgreSQL
        // For example, if Vastbase has different LOB type names or size limits
        // if (mappedType.equals("TEXT")) {
        //     mappedType = "VAST_TEXT_TYPE"; // Example override
        // }

        logger.debug("Mapped to (via MySqlToPostgres): {}", mappedType);
        return mappedType;
    }
}
