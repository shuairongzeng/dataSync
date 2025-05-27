package com.dbsync.dbsync.typemapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MySqlToPostgresTypeMapperTest {

    private MySqlToPostgresTypeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MySqlToPostgresTypeMapper();
    }

    @Test
    void testVarchar() {
        assertEquals("VARCHAR(255)", mapper.mapType("VARCHAR", 255, null));
        assertEquals("TEXT", mapper.mapType("VARCHAR", null, null)); // Default for unspecified length
        assertEquals("VARCHAR(65535)", mapper.mapType("VARCHAR", 65535, null));
    }

    @Test
    void testChar() {
        assertEquals("CHAR(10)", mapper.mapType("CHAR", 10, null));
        assertEquals("CHAR(1)", mapper.mapType("CHAR", null, null));
    }

    @Test
    void testTextTypes() {
        assertEquals("TEXT", mapper.mapType("TINYTEXT", null, null));
        assertEquals("TEXT", mapper.mapType("TEXT", null, null));
        assertEquals("TEXT", mapper.mapType("MEDIUMTEXT", null, null));
        assertEquals("TEXT", mapper.mapType("LONGTEXT", null, null));
    }
    
    @Test
    void testEnumSet() {
        assertEquals("TEXT", mapper.mapType("ENUM", null, null));
        assertEquals("TEXT", mapper.mapType("SET", null, null));
    }

    @Test
    void testIntegerTypes() {
        assertEquals("SMALLINT", mapper.mapType("TINYINT", null, null));
        assertEquals("SMALLINT", mapper.mapType("TINYINT", 1, null)); // TINYINT(1) often boolean
        assertEquals("SMALLINT", mapper.mapType("SMALLINT", null, null));
        assertEquals("INTEGER", mapper.mapType("MEDIUMINT", null, null));
        assertEquals("INTEGER", mapper.mapType("INT", null, null));
        assertEquals("INTEGER", mapper.mapType("INTEGER", null, null));
        assertEquals("BIGINT", mapper.mapType("BIGINT", null, null));
    }

    @Test
    void testFloatDouble() {
        // MySQL FLOAT(p) where p is precision in bits (not total digits like DECIMAL)
        // MySQL FLOAT without precision is single. FLOAT(p<=24) single, FLOAT(p>24) double.
        assertEquals("REAL", mapper.mapType("FLOAT", 10, null)); // p<=24 -> REAL
        assertEquals("DOUBLE PRECISION", mapper.mapType("FLOAT", 30, null)); // p>24 -> DOUBLE PRECISION
        assertEquals("REAL", mapper.mapType("FLOAT", null, null)); // Default for FLOAT is single
        
        assertEquals("DOUBLE PRECISION", mapper.mapType("DOUBLE", null, null));
        assertEquals("DOUBLE PRECISION", mapper.mapType("DOUBLE PRECISION", null, null));
    }

    @Test
    void testDecimalNumeric() {
        assertEquals("NUMERIC(10,2)", mapper.mapType("DECIMAL", 10, 2));
        assertEquals("NUMERIC(65,0)", mapper.mapType("DECIMAL", 65, null)); // Max precision, default scale 0
        assertEquals("NUMERIC", mapper.mapType("DECIMAL", null, null)); // Default NUMERIC
        assertEquals("NUMERIC(8,4)", mapper.mapType("NUMERIC", 8, 4));
    }
    
    @Test
    void testBit() {
        assertEquals("BOOLEAN", mapper.mapType("BIT", 1, null));
        assertEquals("BIT(8)", mapper.mapType("BIT", 8, null));
        assertEquals("BIT(1)", mapper.mapType("BIT", null, null)); // Default for BIT is BIT(1)
    }

    @Test
    void testDateTimeTypes() {
        assertEquals("DATE", mapper.mapType("DATE", null, null));
        assertEquals("TIMESTAMP WITHOUT TIME ZONE", mapper.mapType("DATETIME", null, null));
        assertEquals("TIMESTAMP WITH TIME ZONE", mapper.mapType("TIMESTAMP", null, null)); // MySQL TIMESTAMP is UTC-based
        assertEquals("TIME WITHOUT TIME ZONE", mapper.mapType("TIME", null, null));
        assertEquals("SMALLINT", mapper.mapType("YEAR", null, null));
    }

    @Test
    void testBinaryTypes() {
        assertEquals("BYTEA", mapper.mapType("BINARY", 10, null));
        assertEquals("BYTEA", mapper.mapType("VARBINARY", 100, null));
        assertEquals("BYTEA", mapper.mapType("TINYBLOB", null, null));
        assertEquals("BYTEA", mapper.mapType("BLOB", null, null));
        assertEquals("BYTEA", mapper.mapType("MEDIUMBLOB", null, null));
        assertEquals("BYTEA", mapper.mapType("LONGBLOB", null, null));
    }
    
    @Test
    void testJson() {
        assertEquals("JSONB", mapper.mapType("JSON", null, null));
    }

    @Test
    void testSpatialTypes() {
        assertEquals("TEXT", mapper.mapType("GEOMETRY", null, null)); // Default mapping in provider for simplicity
        assertEquals("TEXT", mapper.mapType("POINT", null, null));
    }
    
    @Test
    void testUnknownType() {
        assertEquals("TEXT", mapper.mapType("SOMEUNKNOWNTYPE", null, null));
    }
    
    @Test
    void testNullType() {
        assertEquals("TEXT", mapper.mapType(null, null, null));
    }
}
