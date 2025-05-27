package com.dbsync.dbsync.typemapping;

/**
 * Interface for data type mapping between different database systems.
 */
@FunctionalInterface
public interface TypeMapper {

    /**
     * Maps a source column type to a target database column type.
     *
     * @param sourceColumnType The name of the source column type (e.g., "VARCHAR2", "NUMBER", "TEXT").
     * @param columnSize The size of the column (e.g., for VARCHAR(255), this would be 255). Can be null.
     * @param decimalDigits The number of decimal digits for numeric types (scale). Can be null.
     * @return The corresponding column type definition string for the target database.
     */
    String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits);
}
