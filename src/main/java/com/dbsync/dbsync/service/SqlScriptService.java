package com.dbsync.dbsync.service;

import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.util.SqlScriptParser;
import com.dbsync.dbsync.util.SqlScriptParser.SqlStatement;
import com.dbsync.dbsync.util.SqlScriptParser.StatementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQL脚本执行服务
 * 支持多语句脚本执行，特别针对Oracle数据库优化
 */
@Service
public class SqlScriptService {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlScriptService.class);
    
    @Autowired
    private DbConnectionService dbConnectionService;
    
    /**
     * 执行SQL脚本
     * 
     * @param connectionId 数据库连接ID
     * @param script 完整的SQL脚本
     * @param schema 数据库schema
     * @param executeInTransaction 是否在事务中执行
     * @return 脚本执行结果
     */
    public ScriptExecutionResult executeScript(Long connectionId, String script, String schema, boolean executeInTransaction) {
        long startTime = System.currentTimeMillis();
        
        // 获取数据库连接配置
        DbConnection dbConnection = dbConnectionService.getConnectionById(connectionId);
        if (dbConnection == null) {
            throw new RuntimeException("数据库连接不存在: " + connectionId);
        }
        
        if (!dbConnection.getEnabled()) {
            throw new RuntimeException("数据库连接已禁用: " + dbConnection.getName());
        }
        
        // 解析SQL脚本
        List<SqlStatement> statements = SqlScriptParser.parseScript(script, dbConnection.getDbType());
        if (statements.isEmpty()) {
            return new ScriptExecutionResult("脚本为空或无有效语句", 0, 0, System.currentTimeMillis() - startTime);
        }
        
        logger.info("开始执行SQL脚本，连接ID: {}, 语句数量: {}, 事务模式: {}", 
                   connectionId, statements.size(), executeInTransaction);
        
        // 构建JDBC URL
        String jdbcUrl = buildJdbcUrl(dbConnection, schema);
        
        // 执行脚本
        ScriptExecutionResult result;
        if (executeInTransaction) {
            result = executeInTransaction(jdbcUrl, dbConnection, statements, startTime);
        } else {
            result = executeWithoutTransaction(jdbcUrl, dbConnection, statements, startTime);
        }
        
        logger.info("SQL脚本执行完成，连接ID: {}, 成功: {}, 失败: {}, 耗时: {}ms", 
                   connectionId, result.getSuccessCount(), result.getFailedCount(), result.getTotalTime());
        
        return result;
    }
    
    /**
     * 在事务中执行脚本
     */
    private ScriptExecutionResult executeInTransaction(String jdbcUrl, DbConnection dbConnection, 
                                                      List<SqlStatement> statements, long startTime) {
        Connection conn = null;
        try {
            conn = createConnection(jdbcUrl, dbConnection);
            conn.setAutoCommit(false); // 开启事务
            
            ScriptExecutionResult result = executeStatements(conn, statements, startTime);
            
            if (result.getFailedCount() == 0) {
                conn.commit();
                logger.info("事务提交成功，执行了 {} 条语句", result.getSuccessCount());
                result.setMessage("事务执行成功，所有语句已提交");
            } else {
                conn.rollback();
                logger.warn("事务回滚，成功: {}, 失败: {}", result.getSuccessCount(), result.getFailedCount());
                result.setMessage("事务执行失败已回滚，成功: " + result.getSuccessCount() + ", 失败: " + result.getFailedCount());
            }
            
            return result;
            
        } catch (SQLException e) {
            // 确保回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.info("异常情况下事务已回滚");
                } catch (SQLException rollbackEx) {
                    logger.error("事务回滚失败", rollbackEx);
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            logger.error("脚本执行失败: {}", e.getMessage(), e);
            return new ScriptExecutionResult("脚本执行失败: " + e.getMessage(), 0, 1, totalTime);
            
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.error("关闭数据库连接失败", e);
                }
            }
        }
    }
    
    /**
     * 不在事务中执行脚本（每个语句独立提交）
     */
    private ScriptExecutionResult executeWithoutTransaction(String jdbcUrl, DbConnection dbConnection, 
                                                           List<SqlStatement> statements, long startTime) {
        Connection conn = null;
        try {
            conn = createConnection(jdbcUrl, dbConnection);
            conn.setAutoCommit(true); // 自动提交
            
            return executeStatements(conn, statements, startTime);
            
        } catch (SQLException e) {
            long totalTime = System.currentTimeMillis() - startTime;
            logger.error("脚本执行失败: {}", e.getMessage(), e);
            return new ScriptExecutionResult("脚本执行失败: " + e.getMessage(), 0, 1, totalTime);
            
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.error("关闭数据库连接失败", e);
                }
            }
        }
    }
    
    /**
     * 执行语句列表
     */
    private ScriptExecutionResult executeStatements(Connection conn, List<SqlStatement> statements, long startTime) {
        int successCount = 0;
        int failedCount = 0;
        List<StatementResult> results = new ArrayList<>();
        
        for (int i = 0; i < statements.size(); i++) {
            SqlStatement stmt = statements.get(i);
            long stmtStartTime = System.currentTimeMillis();
            
            try {
                logger.debug("执行语句 {}/{}: [{}] {}", i + 1, statements.size(), stmt.getType(), 
                           stmt.getSql().length() > 100 ? stmt.getSql().substring(0, 100) + "..." : stmt.getSql());
                
                StatementResult stmtResult = executeSingleStatement(conn, stmt, stmtStartTime);
                results.add(stmtResult);
                
                if (stmtResult.isSuccess()) {
                    successCount++;
                    logger.debug("语句执行成功: {} ms", stmtResult.getExecutionTime());
                } else {
                    failedCount++;
                    logger.warn("语句执行失败: {}", stmtResult.getErrorMessage());
                }
                
            } catch (Exception e) {
                failedCount++;
                long stmtTime = System.currentTimeMillis() - stmtStartTime;
                StatementResult errorResult = new StatementResult(
                    stmt, false, e.getMessage(), 0, stmtTime, stmt.getStartLine()
                );
                results.add(errorResult);
                logger.error("语句执行异常: {}", e.getMessage(), e);
            }
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        String message = String.format("脚本执行完成，成功: %d, 失败: %d, 总耗时: %d ms", 
                                     successCount, failedCount, totalTime);
        
        ScriptExecutionResult result = new ScriptExecutionResult(message, successCount, failedCount, totalTime);
        result.setStatementResults(results);
        
        return result;
    }
    
    /**
     * 执行单个语句
     */
    private StatementResult executeSingleStatement(Connection conn, SqlStatement stmt, long startTime) throws SQLException {
        try (Statement sqlStmt = conn.createStatement()) {
            // 设置超时时间
            sqlStmt.setQueryTimeout(300); // 5分钟超时，DDL语句可能需要较长时间
            
            boolean hasResultSet = sqlStmt.execute(stmt.getSql());
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (stmt.getType() == StatementType.QUERY && hasResultSet) {
                // 查询语句，返回结果集信息
                try (ResultSet rs = sqlStmt.getResultSet()) {
                    int rowCount = 0;
                    if (rs.last()) {
                        rowCount = rs.getRow();
                    }
                    return new StatementResult(stmt, true, "查询成功", rowCount, executionTime, stmt.getStartLine());
                }
            } else {
                // DDL/DML语句，返回影响行数
                int updateCount = sqlStmt.getUpdateCount();
                String message;
                
                switch (stmt.getType()) {
                    case DDL:
                        message = stmt.getCategory() + " 执行成功";
                        break;
                    case DML:
                        message = "影响 " + updateCount + " 行";
                        break;
                    default:
                        message = "执行成功";
                        break;
                }
                
                return new StatementResult(stmt, true, message, updateCount, executionTime, stmt.getStartLine());
            }
        }
    }
    
    /**
     * 创建数据库连接
     */
    private Connection createConnection(String jdbcUrl, DbConnection dbConnection) throws SQLException {
        // 设置连接超时
        DriverManager.setLoginTimeout(30);
        
        Connection conn = DriverManager.getConnection(jdbcUrl, dbConnection.getUsername(), dbConnection.getPassword());
        
        // 设置网络超时
        try {
            conn.setNetworkTimeout(null, 300000); // 5分钟网络超时
        } catch (SQLException e) {
            logger.warn("无法设置网络超时: {}", e.getMessage());
        }
        
        return conn;
    }
    
    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(DbConnection connection, String schema) {
        String baseUrl = dbConnectionService.buildJdbcUrl(connection);
        
        // Oracle不需要在URL中指定schema，通过用户权限控制
        // 其他数据库类型的schema处理保持原有逻辑
        if (schema != null && !schema.trim().isEmpty()) {
            String dbType = connection.getDbType().toLowerCase();
            switch (dbType) {
                case "postgresql":
                case "vastbase":
                    baseUrl += (baseUrl.contains("?") ? "&" : "?") + "currentSchema=" + schema;
                    break;
                default:
                    // 其他数据库类型暂时不在URL中处理schema
                    break;
            }
        }
        
        return baseUrl;
    }
    
    /**
     * 脚本执行结果
     */
    public static class ScriptExecutionResult {
        private String message;
        private int successCount;
        private int failedCount;
        private long totalTime;
        private List<StatementResult> statementResults;
        
        public ScriptExecutionResult(String message, int successCount, int failedCount, long totalTime) {
            this.message = message;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.totalTime = totalTime;
            this.statementResults = new ArrayList<>();
        }
        
        // Getters and Setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
        public long getTotalTime() { return totalTime; }
        
        public List<StatementResult> getStatementResults() { return statementResults; }
        public void setStatementResults(List<StatementResult> statementResults) { this.statementResults = statementResults; }
        
        public int getTotalCount() { return successCount + failedCount; }
        public boolean isSuccess() { return failedCount == 0; }
    }
    
    /**
     * 单个语句执行结果
     */
    public static class StatementResult {
        private SqlStatement statement;
        private boolean success;
        private String message;
        private int affectedRows;
        private long executionTime;
        private int lineNumber;
        
        public StatementResult(SqlStatement statement, boolean success, String message, 
                              int affectedRows, long executionTime, int lineNumber) {
            this.statement = statement;
            this.success = success;
            this.message = message;
            this.affectedRows = affectedRows;
            this.executionTime = executionTime;
            this.lineNumber = lineNumber;
        }
        
        // Getters
        public SqlStatement getStatement() { return statement; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getErrorMessage() { return success ? null : message; }
        public int getAffectedRows() { return affectedRows; }
        public long getExecutionTime() { return executionTime; }
        public int getLineNumber() { return lineNumber; }
        
        public String getStatementPreview() {
            String sql = statement.getSql();
            return sql.length() > 100 ? sql.substring(0, 100) + "..." : sql;
        }
    }
}