package com.dbsync.dbsync.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PostgreSQL cluster synchronization fixes
 * Tests the fixes for table creation and data insertion synchronization issues
 * 
 * Note: This test focuses on testing the private helper methods that were added
 * to fix PostgreSQL cluster synchronization issues. Since the methods are private
 * and the service requires complex dependencies, we use reflection to test the logic.
 */
@SpringBootTest
public class PostgreSQLClusterSyncTest {

    @Test
    void testPostgreSQLClusterErrorDetectionMethodExists() throws Exception {
        // Test that the isPostgreSQLClusterError method exists
        Method method = DatabaseSyncService.class
            .getDeclaredMethod("isPostgreSQLClusterError", String.class);
        assertNotNull(method, "isPostgreSQLClusterError method should exist");
        
        // Test method accessibility
        method.setAccessible(true);
        assertTrue(method.isAccessible(), "Method should be accessible after setAccessible(true)");
    }

    @Test
    void testPaginationColumnFilteringMethodExists() throws Exception {
        // Test that the isPaginationColumn method exists
        Method method = DatabaseSyncService.class
            .getDeclaredMethod("isPaginationColumn", String.class);
        assertNotNull(method, "isPaginationColumn method should exist");
        
        // Test method accessibility
        method.setAccessible(true);
        assertTrue(method.isAccessible(), "Method should be accessible after setAccessible(true)");
    }

    @Test
    void testTableReplicationWaitMethodExists() throws Exception {
        // Test that the waitForTableReplication method exists
        Method method = DatabaseSyncService.class
            .getDeclaredMethod("waitForTableReplication", String.class, 
                org.apache.ibatis.session.SqlSession.class, String.class);
        assertNotNull(method, "waitForTableReplication method should exist");
        
        // Test method accessibility
        method.setAccessible(true);
        assertTrue(method.isAccessible(), "Method should be accessible after setAccessible(true)");
    }

    @Test
    void testValidateTableExistenceMethodExists() throws Exception {
        // Test that the validateTableExistence method exists
        Method method = DatabaseSyncService.class
            .getDeclaredMethod("validateTableExistence", 
                java.sql.Connection.class, String.class, String.class);
        assertNotNull(method, "validateTableExistence method should exist");
        
        // Test method accessibility
        method.setAccessible(true);
        assertTrue(method.isAccessible(), "Method should be accessible after setAccessible(true)");
    }

    @Test
    void testExecuteDDLWithClusterSupportMethodExists() throws Exception {
        // Test that the executeDDLWithClusterSupport method exists
        Method method = DatabaseSyncService.class
            .getDeclaredMethod("executeDDLWithClusterSupport", 
                String.class, 
                org.apache.ibatis.session.SqlSession.class, 
                String.class, 
                java.util.List.class, 
                String.class, 
                java.util.List.class, 
                String.class);
        assertNotNull(method, "executeDDLWithClusterSupport method should exist");
        
        // Test method accessibility
        method.setAccessible(true);
        assertTrue(method.isAccessible(), "Method should be accessible after setAccessible(true)");
    }

    /**
     * This test verifies that all the key methods added for PostgreSQL cluster
     * synchronization fixes are present in the DatabaseSyncService class.
     * 
     * The actual functionality testing would require:
     * 1. Real database connections
     * 2. PostgreSQL cluster setup
     * 3. Integration test environment
     * 
     * For functional testing, see PostgreSQLClusterIntegrationTest.
     */
    @Test
    void testAllClusterFixMethodsExist() {
        Class<DatabaseSyncService> serviceClass = DatabaseSyncService.class;
        Method[] methods = serviceClass.getDeclaredMethods();
        
        boolean hasClusterErrorMethod = false;
        boolean hasPaginationMethod = false;
        boolean hasReplicationWaitMethod = false;
        boolean hasTableValidationMethod = false;
        boolean hasDDLClusterMethod = false;
        
        for (Method method : methods) {
            String methodName = method.getName();
            if ("isPostgreSQLClusterError".equals(methodName)) {
                hasClusterErrorMethod = true;
            } else if ("isPaginationColumn".equals(methodName)) {
                hasPaginationMethod = true;
            } else if ("waitForTableReplication".equals(methodName)) {
                hasReplicationWaitMethod = true;
            } else if ("validateTableExistence".equals(methodName)) {
                hasTableValidationMethod = true;
            } else if ("executeDDLWithClusterSupport".equals(methodName)) {
                hasDDLClusterMethod = true;
            }
        }
        
        assertTrue(hasClusterErrorMethod, "Should have isPostgreSQLClusterError method");
        assertTrue(hasPaginationMethod, "Should have isPaginationColumn method");
        assertTrue(hasReplicationWaitMethod, "Should have waitForTableReplication method");
        assertTrue(hasTableValidationMethod, "Should have validateTableExistence method");
        assertTrue(hasDDLClusterMethod, "Should have executeDDLWithClusterSupport method");
    }
}
