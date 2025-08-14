package com.dbsync.dbsync.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL脚本解析器
 * 支持多语句脚本的解析，特别是Oracle数据库的复杂语法
 */
public class SqlScriptParser {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlScriptParser.class);
    
    /**
     * 解析SQL脚本为独立的语句列表
     * 
     * @param script 完整的SQL脚本
     * @param dbType 数据库类型
     * @return 解析后的SQL语句列表
     */
    public static List<SqlStatement> parseScript(String script, String dbType) {
        List<SqlStatement> statements = new ArrayList<>();
        
        if (script == null || script.trim().isEmpty()) {
            return statements;
        }
        
        // 移除注释并保留重要的空行信息
        String cleanedScript = removeComments(script);
        
        switch (dbType.toLowerCase()) {
            case "oracle":
                return parseOracleScript(cleanedScript);
            case "mysql":
                return parseMySqlScript(cleanedScript);
            case "postgresql":
            case "vastbase":
                return parsePostgreSqlScript(cleanedScript);
            case "sqlserver":
                return parseSqlServerScript(cleanedScript);
            default:
                return parseGenericScript(cleanedScript);
        }
    }
    
    /**
     * 解析Oracle脚本
     * 特殊处理：触发器、存储过程、包等以END;或/结尾的语句
     */
    private static List<SqlStatement> parseOracleScript(String script) {
        List<SqlStatement> statements = new ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        String[] lines = script.split("\\n");
        
        boolean inBlock = false;
        String blockType = "";
        int blockLevel = 0;
        int lineNumber = 1;
        int statementStartLine = 1;
        
        for (String line : lines) {
            String trimmedLine = line.trim().toUpperCase();
            
            // 检查是否开始块语句（触发器、存储过程等）
            if (!inBlock) {
                if (trimmedLine.startsWith("CREATE OR REPLACE TRIGGER") ||
                    trimmedLine.startsWith("CREATE TRIGGER") ||
                    trimmedLine.startsWith("CREATE OR REPLACE PROCEDURE") ||
                    trimmedLine.startsWith("CREATE PROCEDURE") ||
                    trimmedLine.startsWith("CREATE OR REPLACE FUNCTION") ||
                    trimmedLine.startsWith("CREATE FUNCTION") ||
                    trimmedLine.startsWith("CREATE OR REPLACE PACKAGE") ||
                    trimmedLine.startsWith("CREATE PACKAGE")) {
                    inBlock = true;
                    blockType = extractBlockType(trimmedLine);
                    blockLevel = 0;
                    statementStartLine = lineNumber;
                }
            }
            
            currentStatement.append(line).append("\n");
            
            if (inBlock) {
                // 计算嵌套级别
                if (trimmedLine.contains(" BEGIN") || trimmedLine.equals("BEGIN")) {
                    blockLevel++;
                }
                if (trimmedLine.contains(" END") || trimmedLine.startsWith("END")) {
                    blockLevel--;
                }
                
                // 检查块结束条件
                if ((trimmedLine.equals("/") || 
                    (trimmedLine.endsWith(";") && blockLevel <= 0 && 
                     (trimmedLine.equals("END;") || trimmedLine.matches("END\\s+\\w+;")))) &&
                    blockLevel <= 0) {
                    
                    // 块语句结束
                    inBlock = false;
                    String stmt = currentStatement.toString().trim();
                    if (!stmt.isEmpty()) {
                        statements.add(new SqlStatement(stmt, StatementType.DDL, blockType, statementStartLine, lineNumber));
                    }
                    currentStatement.setLength(0);
                    statementStartLine = lineNumber + 1;
                }
            } else {
                // 普通语句处理
                if (trimmedLine.equals("/") || trimmedLine.endsWith(";")) {
                    String stmt = currentStatement.toString().trim();
                    
                    // 移除末尾的/或;
                    if (stmt.endsWith("/")) {
                        stmt = stmt.substring(0, stmt.length() - 1).trim();
                    }
                    if (stmt.endsWith(";")) {
                        stmt = stmt.substring(0, stmt.length() - 1).trim();
                    }
                    
                    if (!stmt.isEmpty()) {
                        StatementType type = determineStatementType(stmt);
                        String category = determineStatementCategory(stmt);
                        statements.add(new SqlStatement(stmt, type, category, statementStartLine, lineNumber));
                    }
                    currentStatement.setLength(0);
                    statementStartLine = lineNumber + 1;
                }
            }
            
            lineNumber++;
        }
        
        // 处理最后一个语句（如果没有以;或/结尾）
        if (currentStatement.length() > 0) {
            String stmt = currentStatement.toString().trim();
            if (!stmt.isEmpty()) {
                StatementType type = determineStatementType(stmt);
                String category = determineStatementCategory(stmt);
                statements.add(new SqlStatement(stmt, type, category, statementStartLine, lineNumber - 1));
            }
        }
        
        return statements;
    }
    
    /**
     * 解析通用SQL脚本（以分号分隔）
     */
    private static List<SqlStatement> parseGenericScript(String script) {
        List<SqlStatement> statements = new ArrayList<>();
        String[] parts = script.split(";");
        
        int lineNumber = 1;
        for (String part : parts) {
            String stmt = part.trim();
            if (!stmt.isEmpty()) {
                StatementType type = determineStatementType(stmt);
                String category = determineStatementCategory(stmt);
                statements.add(new SqlStatement(stmt, type, category, lineNumber, lineNumber));
            }
            // 简单地假设每个语句占一行（实际情况可能更复杂）
            lineNumber++;
        }
        
        return statements;
    }
    
    /**
     * MySQL脚本解析
     */
    private static List<SqlStatement> parseMySqlScript(String script) {
        // MySQL语法相对简单，主要以分号分隔
        return parseGenericScript(script);
    }
    
    /**
     * PostgreSQL脚本解析
     */
    private static List<SqlStatement> parsePostgreSqlScript(String script) {
        // PostgreSQL支持$$分隔符，但基本解析逻辑类似
        return parseGenericScript(script);
    }
    
    /**
     * SQL Server脚本解析
     */
    private static List<SqlStatement> parseSqlServerScript(String script) {
        // SQL Server使用GO分隔批处理
        List<SqlStatement> statements = new ArrayList<>();
        String[] batches = script.split("(?i)\\bGO\\b");
        
        int lineNumber = 1;
        for (String batch : batches) {
            String[] stmts = batch.split(";");
            for (String stmt : stmts) {
                String cleanStmt = stmt.trim();
                if (!cleanStmt.isEmpty()) {
                    StatementType type = determineStatementType(cleanStmt);
                    String category = determineStatementCategory(cleanStmt);
                    statements.add(new SqlStatement(cleanStmt, type, category, lineNumber, lineNumber));
                }
                lineNumber++;
            }
        }
        
        return statements;
    }
    
    /**
     * 移除SQL注释
     */
    private static String removeComments(String script) {
        // 移除-- 单行注释
        script = script.replaceAll("--[^\n\r]*", "");
        
        // 移除/* */ 多行注释
        script = script.replaceAll("/\\*[\\s\\S]*?\\*/", "");
        
        return script;
    }
    
    /**
     * 提取块类型
     */
    private static String extractBlockType(String line) {
        if (line.contains("TRIGGER")) return "TRIGGER";
        if (line.contains("PROCEDURE")) return "PROCEDURE";
        if (line.contains("FUNCTION")) return "FUNCTION";
        if (line.contains("PACKAGE")) return "PACKAGE";
        return "BLOCK";
    }
    
    /**
     * 确定语句类型
     */
    private static StatementType determineStatementType(String sql) {
        String upperSql = sql.trim().toUpperCase();
        
        if (upperSql.startsWith("SELECT") || upperSql.startsWith("WITH")) {
            return StatementType.QUERY;
        } else if (upperSql.startsWith("INSERT") || upperSql.startsWith("UPDATE") || upperSql.startsWith("DELETE")) {
            return StatementType.DML;
        } else if (upperSql.startsWith("CREATE") || upperSql.startsWith("ALTER") || 
                   upperSql.startsWith("DROP") || upperSql.startsWith("COMMENT")) {
            return StatementType.DDL;
        } else if (upperSql.startsWith("COMMIT") || upperSql.startsWith("ROLLBACK") || 
                   upperSql.startsWith("SAVEPOINT")) {
            return StatementType.TRANSACTION;
        } else {
            return StatementType.OTHER;
        }
    }
    
    /**
     * 确定语句分类
     */
    private static String determineStatementCategory(String sql) {
        String upperSql = sql.trim().toUpperCase();
        
        if (upperSql.contains("SEQUENCE")) return "SEQUENCE";
        if (upperSql.contains("TABLE")) return "TABLE";
        if (upperSql.contains("INDEX")) return "INDEX";
        if (upperSql.contains("TRIGGER")) return "TRIGGER";
        if (upperSql.contains("PROCEDURE")) return "PROCEDURE";
        if (upperSql.contains("FUNCTION")) return "FUNCTION";
        if (upperSql.contains("COMMENT")) return "COMMENT";
        
        return "GENERAL";
    }
    
    /**
     * SQL语句信息类
     */
    public static class SqlStatement {
        private final String sql;
        private final StatementType type;
        private final String category;
        private final int startLine;
        private final int endLine;
        
        public SqlStatement(String sql, StatementType type, String category, int startLine, int endLine) {
            this.sql = sql;
            this.type = type;
            this.category = category;
            this.startLine = startLine;
            this.endLine = endLine;
        }
        
        // Getters
        public String getSql() { return sql; }
        public StatementType getType() { return type; }
        public String getCategory() { return category; }
        public int getStartLine() { return startLine; }
        public int getEndLine() { return endLine; }
        
        @Override
        public String toString() {
            return String.format("[%s/%s] Lines %d-%d: %s", 
                type, category, startLine, endLine, 
                sql.length() > 50 ? sql.substring(0, 50) + "..." : sql);
        }
    }
    
    /**
     * SQL语句类型枚举
     */
    public enum StatementType {
        QUERY,      // SELECT查询
        DML,        // INSERT/UPDATE/DELETE
        DDL,        // CREATE/ALTER/DROP
        TRANSACTION, // COMMIT/ROLLBACK
        OTHER       // 其他
    }
}