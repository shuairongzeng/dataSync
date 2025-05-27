package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assuming Vastbase types are highly compatible with PostgreSQL
public class PostgresToVastbaseTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(PostgresToVastbaseTypeMapper.class);
    private final TypeMapper pgToPgHelper = new PostgresToPostgresTypeMapper();


    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        logger.debug("PostgreSQL to Vastbase mapping for type: {}, size: {}, digits: {}", sourceColumnType, columnSize, decimalDigits);
        // Assuming Vastbase is PostgreSQL compatible.
        // If Vastbase has specific types not in standard PG, or different naming, they would need explicit rules here.
        String mappedType = pgToPgHelper.mapType(sourceColumnType, columnSize, decimalDigits);
        logger.debug("Mapped to: {}", mappedType);
        return mappedType;
    }
}
