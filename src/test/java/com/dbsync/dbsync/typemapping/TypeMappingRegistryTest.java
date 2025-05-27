package com.dbsync.dbsync.typemapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TypeMappingRegistryTest {

    private TypeMappingRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new TypeMappingRegistry(); // This will register all default mappers
    }

    @Test
    void testGetSpecificMapper_OracleToPostgres() {
        // We can't directly get the mapper instance with the current TypeMappingRegistry API.
        // We infer the correct mapper is used by checking the output of a known mapping.
        assertEquals("VARCHAR(100)", registry.mapType("VARCHAR2", 100, null, "oracle", "postgresql"));
    }
    
    @Test
    void testGetSpecificMapper_MySqlToPostgres() {
         assertEquals("VARCHAR(50)", registry.mapType("VARCHAR", 50, null, "mysql", "postgresql"));
    }

    @Test
    void testGetSpecificMapper_SqlServerToPostgres() {
        // VARCHAR(MAX) is expected based on SqlServerToPostgresTypeMapper for -1 size
        assertEquals("VARCHAR(MAX)", registry.mapType("VARCHAR", -1, null, "sqlserver", "postgresql"));
    }
    
    @Test
    void testGetSpecificMapper_PostgresToOracle() {
        assertEquals("VARCHAR2(200)", registry.mapType("VARCHAR", 200, null, "postgresql", "oracle"));
    }

    @Test
    void testFallbackToDefaultMapper_UnknownCombination() {
        // Use a highly unlikely combination to ensure fallback to DefaultTypeMapper
        // "SOME_UNKNOWN_TYPE" does not contain CHAR, TEXT, or STRING, so it should fallback to the final TEXT.
        assertEquals("TEXT", registry.mapType("SOME_UNKNOWN_TYPE", 100, null, "unknownsource", "unknowntarget"));
    }
    
    @Test
    void testFallbackToDefaultMapper_UnknownCombinationWithStringHint() {
        // "CHAR_LIKE_UNKNOWN" contains "CHAR", so DefaultTypeMapper should use VARCHAR(size)
        assertEquals("VARCHAR(50)", registry.mapType("CHAR_LIKE_UNKNOWN", 50, null, "unknownsource", "unknowntarget"));
    }


    @Test
    void testDefaultMapper_CommonCasesViaUnregisteredPair() {
        // These tests rely on "default_test" source/target not having specific mappers registered,
        // thus invoking the DefaultTypeMapper.
        
        // VARCHAR like
        assertEquals("VARCHAR(100)", registry.mapType("ANYVARCHAR", 100, null, "default_test", "default_test_target"));
        assertEquals("TEXT", registry.mapType("ANYTEXT", null, null, "default_test", "default_test_target"));
        assertEquals("VARCHAR(3999)", registry.mapType("VARCHARLIKE", 3999, null, "default_test", "default_test_target"));
        assertEquals("TEXT", registry.mapType("VARCHARLIKE_LARGE", 4000, null, "default_test", "default_test_target"));


        // INTEGER like
        assertEquals("INTEGER", registry.mapType("ANYINT", null, null, "default_test", "default_test_target"));
        assertEquals("BIGINT", registry.mapType("ANYBIGINT", null, null, "default_test", "default_test_target"));
        assertEquals("SMALLINT", registry.mapType("ANYSMALLINT", null, null, "default_test", "default_test_target"));


        // NUMERIC like
        assertEquals("NUMERIC(10,2)", registry.mapType("ANYDECIMAL", 10, 2, "default_test", "default_test_target"));
        assertEquals("NUMERIC(20)", registry.mapType("ANYNUMERIC_P", 20, null, "default_test", "default_test_target"));
        assertEquals("NUMERIC", registry.mapType("ANYNUMERIC", null, null, "default_test", "default_test_target"));


        // DATE/TIME like
        assertEquals("TIMESTAMP WITHOUT TIME ZONE", registry.mapType("ANYDATETIME", null, null, "default_test", "default_test_target"));

        // BLOB like
        assertEquals("BYTEA", registry.mapType("ANYBLOB", null, null, "default_test", "default_test_target"));
        
        // BOOLEAN like
        assertEquals("BOOLEAN", registry.mapType("ANYBOOLEAN", null, null, "default_test", "default_test_target"));

        // Unknown type
        assertEquals("TEXT", registry.mapType("COMPLETELY_UNKNOWN_XYZ", 100, 5, "default_test", "default_test_target"));
    }
}
