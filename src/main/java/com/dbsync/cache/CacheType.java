package com.dbsync.cache;

/**
 * Cache type enumeration
 */
public enum CacheType {
    TABLE_LIST("table_list"),
    QUERY_RESULT("query_result"),
    TABLE_SCHEMA("table_schema"),
    CONNECTION_METADATA("connection_metadata"),
    DATABASE_SCHEMA("database_schema");
    
    private final String value;
    
    CacheType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
}