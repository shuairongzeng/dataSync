package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Dameng types are similar to Oracle for this mapping
public class DamengToPostgresTypeMapper extends OracleToPostgresTypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(DamengToPostgresTypeMapper.class);

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("Dameng to PostgreSQL mapping for type: {}, size: {}, digits: {}", sourceColumnType, columnSize, decimalDigits);
        // Delegate to OracleToPostgresTypeMapper, can add Dameng specific overrides if known
        String mappedType = super.mapType(sourceColumnType, columnSize, decimalDigits);
        logger.debug("Mapped to: {}", mappedType);
        return mappedType;
    }
}
