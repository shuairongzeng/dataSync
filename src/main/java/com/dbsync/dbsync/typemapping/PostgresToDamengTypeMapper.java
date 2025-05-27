package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Dameng types are similar to Oracle for this mapping
public class PostgresToDamengTypeMapper extends PostgresToOracleTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(PostgresToDamengTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("PostgreSQL to Dameng mapping for type: {}, size: {}, digits: {}", sourceColumnType, columnSize, decimalDigits);
        // Delegate to PostgresToOracleTypeMapper, can add Dameng specific overrides if known
        // For example, Dameng might have different size limits or specific type names
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);

        // Example override: If Oracle mapping results in BLOB, Dameng might prefer a different LOB type
        // if (mappedType.equals("BLOB")) {
        //     logger.info("Adjusting BLOB to a Dameng-specific LOB type if necessary.");
        //     // return "DAMENG_LOB_TYPE"; 
        // }

        logger.debug("Mapped to: {}", mappedType);
        return mappedType;
    }
}
