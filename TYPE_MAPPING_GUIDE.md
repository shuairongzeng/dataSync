# Data Type Mapping Extensibility Guide

This guide explains how to extend the data type mapping capabilities of the DBSync tool. If you encounter data types that are not handled correctly by the default mappers or if you need to customize mappings for specific scenarios, you can create and register your own `TypeMapper` implementations.

## 1. Overview of Type Mapping

The DBSync tool uses a `TypeMappingRegistry` to manage data type conversions between different source and target databases.
-   **`TypeMapper` Interface:** Each specific database-to-database mapping (e.g., Oracle to PostgreSQL) is handled by a class that implements the `com.dbsync.dbsync.typemapping.TypeMapper` interface.
-   **`TypeMappingRegistry`:** This class holds a collection of registered `TypeMapper` instances. When a type needs to be mapped, the registry looks for the most specific mapper available for the given source and target database types. If no specific mapper is found, it falls back to a `DefaultTypeMapper`.

## 2. The `TypeMapper` Interface

The `TypeMapper` interface is a functional interface defined as follows:

```java
package com.dbsync.dbsync.typemapping;

@FunctionalInterface
public interface TypeMapper {
    String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits);
}
```

-   `sourceColumnType`: The name of the data type from the source database (e.g., "VARCHAR2", "NUMBER", "INT").
-   `columnSize`: An integer representing the size or length of the column.
    -   For character types (VARCHAR, CHAR), this is typically the character length.
    -   For numeric types (NUMBER, DECIMAL), this often represents the precision.
    -   For types like Oracle's `FLOAT(binary_precision)`, this is the binary precision.
    -   It can be `null` if not applicable or not provided by the source database metadata.
-   `decimalDigits`: An integer representing the number of decimal digits (scale) for numeric types. It can be `null`.

The method should return a `String` representing the data type definition to be used in the target database's `CREATE TABLE` statement (e.g., "VARCHAR(100)", "NUMERIC(10,2)", "TEXT").

## 3. Creating a Custom `TypeMapper`

To create a custom type mapper:

1.  **Create a new Java class** in the `com.dbsync.dbsync.typemapping` package (or a sub-package).
2.  **Implement the `TypeMapper` interface.**
3.  **Implement the `mapType` method** with your custom logic. Use a `switch` statement or `if-else` blocks based on the `sourceColumnType` (case-insensitive checks are recommended). Consider `columnSize` and `decimalDigits` to generate accurate target types.
4.  **Use logging** (SLF4J) for warnings or informational messages about specific mappings if needed.

**Example: `CustomOracleToPostgresTypeMapper.java`**

Let's say you want to customize how Oracle's `NUMBER(1)` is mapped to PostgreSQL, perhaps to `BOOLEAN` instead of the default `SMALLINT`.

```java
package com.dbsync.dbsync.typemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomOracleToPostgresTypeMapper implements TypeMapper {

    private static final Logger logger = LoggerFactory.getLogger(CustomOracleToPostgresTypeMapper.class);
    private final OracleToPostgresTypeMapper defaultOracleToPg = new OracleToPostgresTypeMapper(); // Delegate for other types

    @Override
    public String mapType(String sourceColumnType, Integer columnSize, Integer decimalDigits) {
        if (sourceColumnType == null) {
            return defaultOracleToPg.mapType(null, columnSize, decimalDigits);
        }
        String upperType = sourceColumnType.toUpperCase();

        if ("NUMBER".equals(upperType)) {
            if (Integer.valueOf(1).equals(columnSize) && (decimalDigits == null || decimalDigits == 0)) {
                logger.info("Custom Mapping: Oracle NUMBER(1,0) or NUMBER(1) to PostgreSQL BOOLEAN");
                return "BOOLEAN";
            }
        }
        
        // For all other Oracle types, delegate to the existing OracleToPostgresTypeMapper
        return defaultOracleToPg.mapType(sourceColumnType, columnSize, decimalDigits);
    }
}
```

## 4. Registering Your Custom `TypeMapper`

After creating your custom mapper, you need to register it with the `TypeMappingRegistry`. This is typically done in the constructor of `TypeMappingRegistry.java`.

1.  Open `src/main/java/com/dbsync/dbsync/typemapping/TypeMappingRegistry.java`.
2.  In the constructor, add a call to `registerMapper` for your new mapper. The key is formed by concatenating the lowercase source DB type, "2", and the lowercase target DB type.

**Example Registration:**

```java
// Inside TypeMappingRegistry constructor

public TypeMappingRegistry() {
    // Initialize with a default mapper
    registerMapper(DEFAULT_MAPPER_KEY, DEFAULT_MAPPER_KEY, new DefaultTypeMapper());
    
    // Register standard mappers...
    // ... (other existing registrations) ...

    // Register your custom mapper (ensure it overrides any existing one for this pair if intended)
    registerMapper("oracle", "postgresql", new CustomOracleToPostgresTypeMapper()); 
    logger.info("Registered CustomOracleToPostgresTypeMapper for oracle to postgresql.");

    // Or, if you want it for a new DB combination:
    // registerMapper("oracle", "mycustomdb", new OracleToMyCustomDbTypeMapper());
}
```
**Important:** If you are overriding an existing mapping (like "oracle" to "postgresql"), ensure your registration either replaces the existing one or that your custom mapper correctly delegates to the original mapper for types it doesn't explicitly handle (as shown in the `CustomOracleToPostgresTypeMapper` example). The `mappers.put(...)` in `registerMapper` will overwrite if the key is the same.

## 5. Finding Existing Mappers

Existing `TypeMapper` implementations are located in the `com.dbsync.dbsync.typemapping` package. Reviewing these can provide good examples of how to handle various data types and parameters. Key mappers include:
*   `OracleToPostgresTypeMapper.java`
*   `MySqlToPostgresTypeMapper.java`
*   `SqlServerToPostgresTypeMapper.java`
*   `PostgresToOracleTypeMapper.java`
*   And mappers for Dameng and Vastbase, often extending the Oracle or PostgreSQL mappers.

By following these steps, you can effectively customize and extend the data type mapping behavior of the DBSync tool to suit your specific database synchronization needs.
```
