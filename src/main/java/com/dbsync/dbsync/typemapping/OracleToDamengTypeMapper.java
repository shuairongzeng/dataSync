package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Dameng is highly compatible with Oracle
public class OracleToDamengTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(OracleToDamengTypeMapper.class);
    private final TypeMapper oracleToOracleHelper = new OracleToOracleTypeMapper();


    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Oracle to Dameng mapping for type: {}, size: {}, digits: {}", sourceColumnType, columnSize, decimalDigits);
        // Delegate to OracleToOracle mapper
        // Add Dameng-specific overrides if they are known after delegation.
        String mappedType = oracleToOracleHelper.mapType(sourceColumnType, columnSize, decimalDigits);
        
        // Example: If Dameng has a slightly different name for a LOB type than Oracle
        // if (mappedType.equals("CLOB") && sourceColumnType.equalsIgnoreCase("CLOB")) {
        //     logger.info("Adjusting Oracle CLOB to potential Dameng specific CLOB if needed.");
        //     // mappedType = "DM_CLOB_TYPE"; // No change needed if it's also CLOB
        // }

        logger.debug("Mapped to (via OracleToOracle): {}", mappedType);
        return mappedType;
    }
}
