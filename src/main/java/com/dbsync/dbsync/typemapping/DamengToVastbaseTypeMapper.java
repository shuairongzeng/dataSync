package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Dameng is Oracle-like and Vastbase is PostgreSQL-like,
// so extends OracleToPostgresTypeMapper
public class DamengToVastbaseTypeMapper extends OracleToPostgresTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(DamengToVastbaseTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Dameng (Oracle-like) to Vastbase (PG-like) mapping for type: {}, size: {}, digits: {}",
            sourceColumnType, columnSize, decimalDigits);
        
        // Delegate to OracleToPostgresTypeMapper
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        
        // Add any Dameng-to-Vastbase specific overrides here if they differ from Oracle-to-PostgreSQL.
        // For example, if Dameng has a specific type that maps differently to Vastbase
        // than its Oracle equivalent would map to PostgreSQL.
        // if (sourceColumnType.equalsIgnoreCase("DM_UNIQUE_TYPE")) {
        //     mappedType = "VASTBASE_EQUIVALENT_FOR_DM_UNIQUE"; 
        // }

        logger.debug("Mapped to (via OracleToPostgres): {}", mappedType);
        return mappedType;
    }
}
