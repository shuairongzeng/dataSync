package com.dbsync.dbsync.typemapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OracleToPostgresTypeMapperTest {

    private OracleToPostgresTypeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OracleToPostgresTypeMapper();
    }

    @Test
    void testVarchar2() {
        assertEquals("VARCHAR(100)", mapper.mapType("VARCHAR2", 100, null));
        assertEquals("TEXT", mapper.mapType("VARCHAR2", null, null)); // Default if no length
        assertEquals("VARCHAR(4000)", mapper.mapType("VARCHAR2", 4000, null));
    }

    @Test
    void testNVarchar2() {
        assertEquals("VARCHAR(200)", mapper.mapType("NVARCHAR2", 200, null));
        assertEquals("TEXT", mapper.mapType("NVARCHAR2", null, null));
    }

    @Test
    void testChar() {
        assertEquals("CHAR(10)", mapper.mapType("CHAR", 10, null));
        assertEquals("CHAR(1)", mapper.mapType("CHAR", null, null)); // Default if no length
    }

    @Test
    void testNChar() {
        assertEquals("CHAR(20)", mapper.mapType("NCHAR", 20, null));
    }

    @Test
    void testClobNclobLong() {
        assertEquals("TEXT", mapper.mapType("CLOB", null, null));
        assertEquals("TEXT", mapper.mapType("NCLOB", null, null));
        assertEquals("TEXT", mapper.mapType("LONG", null, null));
    }

    @Test
    void testNumber_decimal() {
        // NUMBER(p,s) -> NUMERIC(p,s)
        assertEquals("NUMERIC(10,2)", mapper.mapType("NUMBER", 10, 2));
        assertEquals("NUMERIC(38,5)", mapper.mapType("NUMBER", null, 5)); // p defaults to 38
        assertEquals("NUMERIC(5,0)", mapper.mapType("NUMBER", 5, 0)); // No decimal digits
    }
    
    @Test
    void testNumber_integerApproximation() {
        // NUMBER(p) or NUMBER(p,0) -> SMALLINT, INTEGER, BIGINT, NUMERIC(p)
        assertEquals("SMALLINT", mapper.mapType("NUMBER", 3, 0)); // e.g. NUMBER(3,0)
        assertEquals("SMALLINT", mapper.mapType("NUMBER", 4, null)); // e.g. NUMBER(4)
        assertEquals("INTEGER", mapper.mapType("NUMBER", 9, 0));
        assertEquals("INTEGER", mapper.mapType("NUMBER", 5, null));
        assertEquals("BIGINT", mapper.mapType("NUMBER", 18, 0));
        assertEquals("BIGINT", mapper.mapType("NUMBER", 10, null));
        assertEquals("NUMERIC(38)", mapper.mapType("NUMBER", 38, 0));
        assertEquals("NUMERIC(20)", mapper.mapType("NUMBER", 20, null));
    }
    
    @Test
    void testNumber_noPrecisionScale() {
        assertEquals("NUMERIC", mapper.mapType("NUMBER", null, null));
    }

    @Test
    void testIntegerIntSmallint() {
        assertEquals("INTEGER", mapper.mapType("INTEGER", null, null));
        assertEquals("INTEGER", mapper.mapType("INT", null, null));
        assertEquals("SMALLINT", mapper.mapType("SMALLINT", null, null));
    }
    
    @Test
    void testFloatTypes() {
        // Oracle FLOAT(binary_precision)
        assertEquals("REAL", mapper.mapType("FLOAT", 23, null)); // p < 24
        assertEquals("DOUBLE PRECISION", mapper.mapType("FLOAT", 24, null)); // p >= 24
        assertEquals("DOUBLE PRECISION", mapper.mapType("FLOAT", 50, null));
        assertEquals("DOUBLE PRECISION", mapper.mapType("FLOAT", null, null)); // Default

        assertEquals("REAL", mapper.mapType("BINARY_FLOAT", null, null));
        assertEquals("DOUBLE PRECISION", mapper.mapType("BINARY_DOUBLE", null, null));
    }
    
    @Test
    void testDateAndTimestampTypes() {
        assertEquals("TIMESTAMP WITHOUT TIME ZONE", mapper.mapType("DATE", null, null));
        assertEquals("TIMESTAMP WITHOUT TIME ZONE", mapper.mapType("TIMESTAMP", null, null));
        assertEquals("TIMESTAMP WITHOUT TIME ZONE", mapper.mapType("TIMESTAMP(6)", 6, null)); // size here is fractional precision
        assertEquals("TIMESTAMP WITH TIME ZONE", mapper.mapType("TIMESTAMP WITH TIME ZONE", null, null));
        assertEquals("TIMESTAMP WITH TIME ZONE", mapper.mapType("TIMESTAMP WITH LOCAL TIME ZONE", null, null));
    }

    @Test
    void testBinaryTypes() {
        assertEquals("BYTEA", mapper.mapType("BLOB", null, null));
        assertEquals("BYTEA", mapper.mapType("RAW", 2000, null));
        assertEquals("BYTEA", mapper.mapType("LONG RAW", null, null));
    }
    
    @Test
    void testXmlType() {
        assertEquals("XML", mapper.mapType("XMLTYPE", null, null));
    }

    @Test
    void testUnknownType() {
        assertEquals("TEXT", mapper.mapType("SOMEUNKNOWNTYPE", 100, 2));
    }
    
    @Test
    void testNullType() {
        assertEquals("TEXT", mapper.mapType(null, 100, 2));
    }
}
