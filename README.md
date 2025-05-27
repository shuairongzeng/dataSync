# Database Synchronization Tool (DBSync)

## 1. Overview

DBSync is a Java-based utility designed for synchronizing data between various database systems. It allows for configurable synchronization tasks, including schema and data migration, with a focus on handling data type differences between heterogeneous databases. The tool provides progress reporting through detailed logging.

## 2. Features

*   **Multi-Database Support:** Synchronize data between Oracle, PostgreSQL, MySQL, SQLServer, Dameng, and Vastbase.
*   **Configurable Sync Tasks:** Define specific synchronization jobs, including source/target connections, schemas, tables, and pre-sync data truncation options.
*   **Heterogeneous Data Type Mapping:** Automatic mapping of common data types between different database systems. Includes an extensible system for custom mapping logic.
*   **Progress Reporting:** Detailed logging of task and table-level synchronization progress, including record counts and status updates.

## 3. Configuration

Configuration is managed through the `src/main/resources/application.properties` file.

### 3.1. Database Connections

Define all database connections that the tool can use. Each connection is identified by a unique ID.

**Format:**
`db.connections.<connectionId>.property=value`

**Properties:**
*   `url`: The JDBC URL for the database.
*   `username`: Username for the database connection.
*   `password`: Password for the database connection.
*   `driverClassName`: The fully qualified name of the JDBC driver class.
*   `dbType`: A short identifier for the database type. Supported values: `oracle`, `postgresql`, `mysql`, `sqlserver`, `dameng`, `vastbase`. This is crucial for type mapping and metadata queries.

**Examples:**

```properties
# Oracle Connection
db.connections.oracleProd.url=jdbc:oracle:thin:@//hostname:1521/serviceName
db.connections.oracleProd.username=user_oracle
db.connections.oracleProd.password=pass_oracle
db.connections.oracleProd.driverClassName=oracle.jdbc.driver.OracleDriver
db.connections.oracleProd.dbType=oracle

# PostgreSQL Connection
db.connections.pgStaging.url=jdbc:postgresql://hostname:5432/stagingdb
db.connections.pgStaging.username=user_pg
db.connections.pgStaging.password=pass_pg
db.connections.pgStaging.driverClassName=org.postgresql.Driver
db.connections.pgStaging.dbType=postgresql

# MySQL Connection
db.connections.mysqlMain.url=jdbc:mysql://hostname:3306/maindb?serverTimezone=UTC
db.connections.mysqlMain.username=user_mysql
db.connections.mysqlMain.password=pass_mysql
db.connections.mysqlMain.driverClassName=com.mysql.cj.jdbc.Driver
db.connections.mysqlMain.dbType=mysql

# SQLServer Connection
db.connections.sqlServerReporting.url=jdbc:sqlserver://hostname:1433;databaseName=ReportingDB
db.connections.sqlServerReporting.username=user_sql
db.connections.sqlServerReporting.password=pass_sql
db.connections.sqlServerReporting.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver
db.connections.sqlServerReporting.dbType=sqlserver

# Dameng Connection (Uses Dameng JDBC Driver)
db.connections.damengInternal.url=jdbc:dm://hostname:5236/DAMENG
db.connections.damengInternal.username=user_dm
db.connections.damengInternal.password=pass_dm
db.connections.damengInternal.driverClassName=dm.jdbc.driver.DmDriver
db.connections.damengInternal.dbType=dameng

# Vastbase Connection (Uses PostgreSQL JDBC Driver typically)
db.connections.vastbaseCluster.url=jdbc:postgresql://hostname:5432/vastbasedb
db.connections.vastbaseCluster.username=user_vb
db.connections.vastbaseCluster.password=pass_vb
db.connections.vastbaseCluster.driverClassName=org.postgresql.Driver 
db.connections.vastbaseCluster.dbType=vastbase # or 'postgresql' if using its mapper directly
```

### 3.2. Synchronization Tasks

Define one or more synchronization tasks. Each task specifies what data to sync and where.

**Format:**
`sync.tasks[index].property=value`

**Properties:**
*   `name`: A descriptive name for the synchronization task (e.g., `OracleHRToPostgresStaging`). This name is also used as the `taskId` for progress reporting.
*   `sourceConnectionId`: The ID of the source database connection (defined in `db.connections`).
*   `targetConnectionId`: The ID of the target database connection.
*   `sourceSchemaName`: (Optional) The name of the schema in the source database. If omitted, behavior depends on the database's default schema resolution (e.g., user's default schema in Oracle, current database in MySQL).
*   `targetSchemaName`: (Optional) The name of the schema in the target database where tables should be created/synchronized if not using the default schema of the target connection user. Note: Explicit schema qualification in DDL for the target is a potential future enhancement; currently, target operations often rely on the default schema of the target connection.
*   `tables`: A comma-separated list of table names to synchronize (e.g., `EMPLOYEES,DEPARTMENTS,PRODUCTS`).
*   `truncateBeforeSync`: `true` or `false`. If `true`, target tables will be truncated before data synchronization.

**Example:**

```properties
sync.tasks[0].name=OracleHRToPostgresStaging
sync.tasks[0].sourceConnectionId=oracleProd
sync.tasks[0].targetConnectionId=pgStaging
sync.tasks[0].sourceSchemaName=HR
sync.tasks[0].targetSchemaName=public
sync.tasks[0].tables=EMPLOYEES,DEPARTMENTS
sync.tasks[0].truncateBeforeSync=true

sync.tasks[1].name=MySQLBackupImportantTables
sync.tasks[1].sourceConnectionId=mysqlMain
sync.tasks[1].targetConnectionId=pgStaging 
# sourceSchemaName omitted, will use default database/schema from mysqlMain connection
sync.tasks[1].targetSchemaName=mysql_backup 
sync.tasks[1].tables=ORDERS,CUSTOMER_FEEDBACK
sync.tasks[1].truncateBeforeSync=false
```

## 4. Data Type Mapping

The tool automatically handles common data type conversions between different database systems.

*   **Mechanism:** Type mapping is managed by the `TypeMappingRegistry`, which contains specific `TypeMapper` implementations for various database source-target pairs (e.g., `OracleToPostgresTypeMapper`, `MySqlToSqlServerTypeMapper`).
*   **Database Assumptions:** For compatibility:
    *   Dameng is generally treated as Oracle-like.
    *   Vastbase is generally treated as PostgreSQL-like.
    Relevant mappers (e.g., `OracleToDamengTypeMapper` extends `OracleToOracleTypeMapper`) reflect this.
*   **Coverage:** Common data types are covered. However, very specific, rare, or custom user-defined types might fall back to a default mapping (e.g., to `TEXT` or `VARCHAR`).
*   **Extensibility:** The system is extensible. You can add support for new type mappings or customize existing ones by:
    1.  Creating a new Java class that implements the `com.dbsync.dbsync.typemapping.TypeMapper` interface.
    2.  Implementing the `mapType` method with your custom logic.
    3.  Registering an instance of your new mapper in the `TypeMappingRegistry` constructor.
    For more details, please see `TYPE_MAPPING_GUIDE.md`.

## 5. Progress Reporting

Synchronization progress is logged to the console and log files.
*   The `ProgressManager` service tracks the status of each synchronization task and the tables within it.
*   Log entries include:
    *   Task start and completion (status: `COMPLETED_SUCCESS`, `COMPLETED_WITH_ERRORS`, `FAILED`).
    *   Table synchronization start, completion, or failure.
    *   Number of records processed for each table batch.
    *   Total records synced per table and per task.
*   While currently log-based, the `ProgressManager` could be exposed via an API for more direct status queries in future enhancements.

## 6. Running the Application

1.  Ensure Java 8 (or higher) and Maven are installed.
2.  Configure your database connections and synchronization tasks in `src/main/resources/application.properties`.
3.  Add necessary JDBC drivers to `pom.xml` if not already present (Oracle, PostgreSQL, MySQL, SQLServer, and Dameng drivers are pre-configured).
4.  Run the application using Maven:
    ```bash
    mvn spring-boot:run
    ```
5.  Check the console output and log files (e.g., in a `logs` directory if configured, or console by default) for progress and status updates.

## 7. Testing

*   **Unit Tests:** The project includes JUnit 5 tests for the type mapping logic, covering `TypeMappingRegistry` and individual mappers like `OracleToPostgresTypeMapper`, `MySqlToPostgresTypeMapper`, etc. These can be found in `src/test/java/com/dbsync/dbsync/typemapping/`.
*   **Integration Test Task:** An example test task (`TestPgToPgTableCopy`) is provided in `application.properties`. When the application runs, the `CommandLineRunner` will execute this task and log a "PASSED" or "FAILED" status based on its completion. For this test to run meaningfully, you would typically need to manually create the source table (`dbsync_test_source_table`) in your test PostgreSQL database (`pgTestSource` connection) beforehand.

## 8. Troubleshooting

*   **JDBC Driver Errors:**
    *   Ensure all required JDBC drivers are listed in `pom.xml`.
    *   Some drivers, like for Dameng, might have specific installation requirements or might need to be manually installed into your local Maven repository if not publicly available (though `DmJdbcDriver18` is usually available on Maven Central).
*   **Connection Problems:**
    *   Verify JDBC URLs, usernames, and passwords in `application.properties`.
    *   Check network connectivity and firewall rules between the application server and the database servers.
*   **Type Mapping Issues:**
    *   If a specific data type is not mapping as expected, it might be using a default mapping. Refer to the extensibility section in this README and the `TYPE_MAPPING_GUIDE.md` to create a custom mapper.
*   **Schema Name Issues:**
    *   Ensure `sourceSchemaName` is correctly specified if your source tables are not in the default schema for the connection user.
    *   The `TableMetadataSqlProvider` attempts to use database-specific defaults if `schemaName` is not provided (e.g., `current_schema()` in PostgreSQL, `DATABASE()` in MySQL). However, explicit configuration is often more reliable.
```
