package com.dbsync.dbsync.typemapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SqlServerToPostgresTypeMapperTest {

    private SqlServerToPostgresTypeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SqlServerToPostgresTypeMapper();
    }

    @Test
    void testVarchar() {
        assertEquals("VARCHAR(100)", mapper.mapType("VARCHAR", 100, null));
        assertEquals("TEXT", mapper.mapType("VARCHAR", null, null)); // Default for unspecified
        assertEquals("TEXT", mapper.mapType("VARCHAR", -1, null)); // -1 for MAX
    }

    @Test
    void testNVarchar() {
        assertEquals("VARCHAR(200)", mapper.mapType("NVARCHAR", 200, null));
        assertEquals("TEXT", mapper.mapType("NVARCHAR", -1, null)); // -1 for MAX
    }

    @Test
    void testChar() {
        assertEquals("CHAR(10)", mapper.mapType("CHAR", 10, null));
        assertEquals("CHAR(1)", mapper.mapType("CHAR", null, null));
    }

    @Test
    void testNChar() {
        assertEquals("CHAR(20)", mapper.mapType("NCHAR", 20, null));
    }

    @Test
    void testTextNText() {
        assertEquals("TEXT", mapper.mapType("TEXT", null, null));
        assertEquals("TEXT", mapper.mapType("NTEXT", null, null));
    }

    @Test
    void testIntegerTypes() {
        assertEquals("SMALLINT", mapper.mapType("TINYINT", null, null));
        assertEquals("SMALLINT", mapper.mapType("SMALLINT", null, null));
        assertEquals("INTEGER", mapper.mapType("INT", null, null));
        assertEquals("BIGINT", mapper.mapType("BIGINT", null, null));
    }

    @Test
    void testDecimalNumeric() {
        assertEquals("NUMERIC(19,4)", mapper.mapType("DECIMAL", 19, 4));
        assertEquals("NUMERIC(38,0)", mapper.mapType("NUMERIC", 38, 0)); // Max precision, scale 0
        assertEquals("NUMERIC", mapper.mapType("DECIMAL", null, null)); // Default
    }
    
    @Test
    void testMoneyTypes() {
        assertEquals("NUMERIC(19,4)", mapper.mapType("MONEY", null, null));
        assertEquals("NUMERIC(10,4)", mapper.mapType("SMALLMONEY", null, null));
    }

    @Test
    void testFloatReal() {
        // SQL Server FLOAT(n) where n is bits. PG REAL for n<=24, DOUBLE PRECISION otherwise.
        assertEquals("REAL", mapper.mapType("FLOAT", 24, null));
        assertEquals("DOUBLE PRECISION", mapper.mapType("FLOAT", 25, null));
        assertEquals("DOUBLE PRECISION", mapper.mapType("FLOAT", null, null)); // Default for FLOAT is 53 bits
        assertEquals("REAL", mapper.mapType("REAL", null, null)); // SQL Server REAL is FLOAT(24)
    }

    @Test
    void testDateTimeTypes() {
        assertEquals("DATE", mapper.mapType("DATE", null, null));
        // TIME(p) -> TIME(p) WITHOUT TIME ZONE. decimalDigits should hold 'p'.
        assertEquals("TIME(3) WITHOUT TIME ZONE", mapper.mapType("TIME", null, 3)); 
        assertEquals("TIME WITHOUT TIME ZONE", mapper.mapType("TIME", null, null));

        assertEquals("TIMESTAMP(3) WITHOUT TIME ZONE", mapper.mapType("DATETIME", null, null)); // DATETIME is ~ms precision
        assertEquals("TIMESTAMP(7) WITHOUT TIME ZONE", mapper.mapType("DATETIME2", null, 7));
        assertEquals("TIMESTAMP WITHOUT TIME ZONE", mapper.mapType("DATETIME2", null, null)); // Default precision for DATETIME2

        assertEquals("TIMESTAMP(0) WITHOUT TIME ZONE", mapper.mapType("SMALLDATETIME", null, null));
        
        assertEquals("TIMESTAMP(7) WITH TIME ZONE", mapper.mapType("DATETIMEOFFSET", null, 7));
        assertEquals("TIMESTAMP WITH TIME ZONE", mapper.mapType("DATETIMEOFFSET", null, null));
    }

    @Test
    void testBinaryTypes() {
        assertEquals("BYTEA", mapper.mapType("BINARY", 50, null));
        assertEquals("BYTEA", mapper.mapType("VARBINARY", 500, null));
        assertEquals("BYTEA", mapper.mapType("VARBINARY", -1, null)); // varbinary(max)
        assertEquals("BYTEA", mapper.mapType("IMAGE", null, null));
    }

    @Test
    void testOtherTypes() {
        assertEquals("BOOLEAN", mapper.mapType("BIT", null, null));
        assertEquals("UUID", mapper.mapType("UNIQUEIDENTIFIER", null, null));
        assertEquals("XML", mapper.mapType("XML", null, null));
        assertEquals("BYTEA(8)", mapper.mapType("ROWVERSION", null, null));
        assertEquals("BYTEA(8)", mapper.mapType("TIMESTAMP", null, null)); // SQL Server TIMESTAMP is rowversion
        assertEquals("TEXT", mapper.mapType("SQL_VARIANT", null, null));
        assertEquals("TEXT", mapper.mapType("HIERARCHYID", null, null));
        assertEquals("TEXT", mapper.mapType("GEOMETRY", null, null));
        assertEquals("TEXT", mapper.mapType("GEOGRAPHY", null, null));
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
