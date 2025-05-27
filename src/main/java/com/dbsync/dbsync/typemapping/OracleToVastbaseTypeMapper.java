package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Vastbase is PostgreSQL-compatible
public class OracleToVastbaseTypeMapper extends OracleToPostgresTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(OracleToVastbaseTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Oracle to Vastbase (PG compatible) mapping for type: {}, size: {}, digits: {}", 
            sourceColumnType, columnSize, decimalDigits);
        
        // Delegate to OracleToPostgresTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        
        logger.debug("Mapped to (via OracleToPostgres): {}", mappedType);
        return mappedType;
    }
}
