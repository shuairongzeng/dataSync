package com.dbsync.dbsync.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL查询解析器
 * 用于解析SQL语句，提取表名和字段信息，支持中文字段名显示功能
 */
@Component
public class SqlQueryAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlQueryAnalyzer.class);
    
    // 简单的SQL解析正则表达式
    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "SELECT\\s+(.+?)\\s+FROM\\s+([\\w\\s,\\.`\"\\[\\]]+?)(?:\\s+WHERE|\\s+GROUP\\s+BY|\\s+ORDER\\s+BY|\\s+HAVING|\\s+LIMIT|\\s*;|\\s*$)", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern TABLE_PATTERN = Pattern.compile(
        "([\\w\\.`\"\\[\\]]+)(?:\\s+(?:AS\\s+)?([\\w`\"\\[\\]]+))?", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FIELD_ALIAS_PATTERN = Pattern.compile(
        "(.+?)\\s+(?:AS\\s+)?([\\w`\"\\[\\]]+)\\s*$", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 解析SQL查询语句
     */
    public QueryAnalysisResult analyzeQuery(String sql) {
        QueryAnalysisResult result = new QueryAnalysisResult();
        
        if (sql == null || sql.trim().isEmpty()) {
            result.setComplexQuery(true);
            return result;
        }
        
        try {
            String cleanSql = preprocessSql(sql);
            logger.debug("预处理后的SQL: {}", cleanSql);
            
            // 1. 提取SELECT子句和FROM子句
            Matcher selectMatcher = SELECT_PATTERN.matcher(cleanSql);
            if (!selectMatcher.find()) {
                logger.debug("无法匹配SELECT和FROM子句，标记为复杂查询");
                result.setComplexQuery(true);
                return result;
            }
            
            String selectClause = selectMatcher.group(1).trim();
            String fromClause = selectMatcher.group(2).trim();
            
            logger.debug("SELECT子句: {}", selectClause);
            logger.debug("FROM子句: {}", fromClause);
            
            // 2. 解析表名
            result.setTables(parseTableNames(fromClause));
            logger.debug("解析出的表名: {}", result.getTables());
            
            // 3. 解析字段信息
            result.setFields(parseFields(selectClause, result.getTables()));
            logger.debug("解析出的字段: {}", result.getFields());
            
            // 4. 判断是否为复杂查询
            result.setComplexQuery(isComplexQuery(cleanSql, result));
            
            logger.debug("查询复杂度: {}", result.isComplexQuery() ? "复杂" : "简单");
            
        } catch (Exception e) {
            logger.warn("SQL解析失败，将作为复杂查询处理: {}", e.getMessage(), e);
            result.setComplexQuery(true);
        }
        
        return result;
    }
    
    /**
     * 预处理SQL语句
     */
    private String preprocessSql(String sql) {
        return sql.replaceAll("\\s+", " ")  // 合并多个空白字符
                  .replaceAll("/\\*.*?\\*/", " ")  // 移除注释
                  .replaceAll("--.*", " ")  // 移除单行注释
                  .trim();
    }
    
    /**
     * 解析FROM子句中的表名
     */
    private Set<String> parseTableNames(String fromClause) {
        Set<String> tables = new HashSet<>();
        
        try {
            // 处理JOIN语句，将其转换为逗号分隔
            String cleanFrom = fromClause
                .replaceAll("(?i)\\s+(INNER|LEFT|RIGHT|FULL\\s+OUTER?)\\s+JOIN\\s+", " , ")
                .replaceAll("(?i)\\s+ON\\s+[^,]+?(?=\\s*(?:,|$))", "")
                .replaceAll("(?i)\\s+WHERE.*$", "");
            
            // 按逗号分割表名
            String[] tableParts = cleanFrom.split(",");
            
            for (String part : tableParts) {
                String tableName = parseTableName(part.trim());
                if (tableName != null && !tableName.isEmpty()) {
                    tables.add(tableName);
                }
            }
            
        } catch (Exception e) {
            logger.warn("解析表名失败: {}", e.getMessage());
        }
        
        return tables;
    }
    
    /**
     * 解析单个表名（处理别名和schema前缀）
     */
    private String parseTableName(String tablePart) {
        try {
            Matcher tableMatcher = TABLE_PATTERN.matcher(tablePart);
            if (tableMatcher.find()) {
                String fullTableName = tableMatcher.group(1);
                
                // 移除引号和方括号
                String cleanTableName = removeQuotes(fullTableName);
                
                // 移除schema前缀 (如果有)
                if (cleanTableName.contains(".")) {
                    String[] parts = cleanTableName.split("\\.");
                    cleanTableName = parts[parts.length - 1]; // 取最后一部分作为表名
                }
                
                return cleanTableName;
            }
        } catch (Exception e) {
            logger.warn("解析单个表名失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 解析SELECT子句中的字段信息
     */
    private List<FieldInfo> parseFields(String selectClause, Set<String> tables) {
        List<FieldInfo> fields = new ArrayList<>();
        
        try {
            if ("*".equals(selectClause.trim())) {
                // SELECT * 情况，标记为全字段查询
                for (String table : tables) {
                    FieldInfo field = new FieldInfo();
                    field.setFieldName("*");
                    field.setTableName(table);
                    field.setAllFields(true);
                    fields.add(field);
                }
            } else {
                // 解析具体字段，处理逗号分隔（但要注意函数中的逗号）
                List<String> fieldParts = splitSelectFields(selectClause);
                
                for (String part : fieldParts) {
                    FieldInfo field = parseFieldInfo(part.trim(), tables);
                    if (field != null) {
                        fields.add(field);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("解析字段信息失败: {}", e.getMessage());
        }
        
        return fields;
    }
    
    /**
     * 智能分割SELECT字段（处理函数中的逗号）
     */
    private List<String> splitSelectFields(String selectClause) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        int parenthesesLevel = 0;
        boolean inQuotes = false;
        char quoteChar = ' ';
        
        for (int i = 0; i < selectClause.length(); i++) {
            char c = selectClause.charAt(i);
            
            if (!inQuotes && (c == '\'' || c == '"' || c == '`')) {
                inQuotes = true;
                quoteChar = c;
            } else if (inQuotes && c == quoteChar) {
                inQuotes = false;
            } else if (!inQuotes) {
                if (c == '(') {
                    parenthesesLevel++;
                } else if (c == ')') {
                    parenthesesLevel--;
                } else if (c == ',' && parenthesesLevel == 0) {
                    // 找到真正的字段分隔符
                    fields.add(currentField.toString().trim());
                    currentField = new StringBuilder();
                    continue;
                }
            }
            
            currentField.append(c);
        }
        
        // 添加最后一个字段
        if (currentField.length() > 0) {
            fields.add(currentField.toString().trim());
        }
        
        return fields;
    }
    
    /**
     * 解析单个字段信息
     */
    private FieldInfo parseFieldInfo(String fieldStr, Set<String> tables) {
        try {
            FieldInfo field = new FieldInfo();
            
            // 处理别名 (AS子句)
            Matcher aliasMatcher = FIELD_ALIAS_PATTERN.matcher(fieldStr);
            
            String actualFieldName;
            if (aliasMatcher.find() && !isReservedWord(aliasMatcher.group(2))) {
                actualFieldName = aliasMatcher.group(1).trim();
                field.setAlias(removeQuotes(aliasMatcher.group(2)));
            } else {
                actualFieldName = fieldStr;
            }
            
            field.setFieldName(actualFieldName);
            
            // 判断是否为函数或计算字段
            if (isFunction(actualFieldName)) {
                field.setFunction(true);
                field.setComputed(true);
            } else if (isComputed(actualFieldName)) {
                field.setComputed(true);
            }
            
            // 尝试确定字段所属的表
            determineFieldTable(field, tables);
            
            return field;
            
        } catch (Exception e) {
            logger.warn("解析字段信息失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 确定字段所属的表
     */
    private void determineFieldTable(FieldInfo field, Set<String> tables) {
        String fieldName = field.getFieldName();
        
        // 如果字段名包含表前缀
        if (fieldName.contains(".")) {
            String[] parts = fieldName.split("\\.");
            if (parts.length >= 2) {
                String tableAlias = removeQuotes(parts[0]);
                String actualFieldName = removeQuotes(parts[1]);
                
                // 尝试匹配表名或别名
                for (String table : tables) {
                    if (table.equalsIgnoreCase(tableAlias) || 
                        table.toLowerCase().startsWith(tableAlias.toLowerCase())) {
                        field.setTableName(table);
                        field.setFieldName(actualFieldName);
                        break;
                    }
                }
            }
        } else if (tables.size() == 1 && !field.isFunction()) {
            // 单表查询，直接关联
            field.setTableName(tables.iterator().next());
        }
    }
    
    /**
     * 判断是否为复杂查询
     */
    private boolean isComplexQuery(String sql, QueryAnalysisResult result) {
        String upperSql = sql.toUpperCase();
        
        // 复杂查询判断条件
        return upperSql.contains("UNION") || 
               upperSql.contains("EXISTS") ||
               upperSql.contains("WITH") ||
               upperSql.contains("CASE WHEN") ||
               result.getTables().size() > 3 || // 超过3个表的JOIN
               result.getFields().stream().anyMatch(f -> f.isFunction() && isComplexFunction(f.getFieldName()));
    }
    
    /**
     * 判断是否为函数字段
     */
    private boolean isFunction(String fieldName) {
        return fieldName.contains("(") && fieldName.contains(")");
    }
    
    /**
     * 判断是否为计算字段
     */
    private boolean isComputed(String fieldName) {
        return fieldName.contains("+") || fieldName.contains("-") || 
               fieldName.contains("*") || fieldName.contains("/") ||
               fieldName.contains("||") || fieldName.contains("CASE");
    }
    
    /**
     * 判断是否为复杂函数
     */
    private boolean isComplexFunction(String fieldName) {
        String upperField = fieldName.toUpperCase();
        return upperField.contains("ROW_NUMBER") ||
               upperField.contains("RANK") ||
               upperField.contains("DENSE_RANK") ||
               upperField.contains("LAG") ||
               upperField.contains("LEAD");
    }
    
    /**
     * 判断是否为SQL保留字
     */
    private boolean isReservedWord(String word) {
        Set<String> reservedWords = new HashSet<>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "ORDER", "BY", "GROUP", "HAVING", 
            "INNER", "LEFT", "RIGHT", "JOIN", "ON", "AND", "OR", "NOT",
            "IN", "EXISTS", "BETWEEN", "LIKE", "IS", "NULL", "TRUE", "FALSE"
        ));
        return reservedWords.contains(word.toUpperCase());
    }
    
    /**
     * 移除字段名中的引号
     */
    private String removeQuotes(String str) {
        if (str == null) return null;
        return str.replaceAll("[`\"\\[\\]]", "");
    }
    
    /**
     * 查询分析结果
     */
    public static class QueryAnalysisResult {
        private Set<String> tables = new HashSet<>();
        private List<FieldInfo> fields = new ArrayList<>();
        private boolean isComplexQuery = false;
        
        // Getters and Setters
        public Set<String> getTables() { return tables; }
        public void setTables(Set<String> tables) { this.tables = tables; }
        public List<FieldInfo> getFields() { return fields; }
        public void setFields(List<FieldInfo> fields) { this.fields = fields; }
        public boolean isComplexQuery() { return isComplexQuery; }
        public void setComplexQuery(boolean complexQuery) { isComplexQuery = complexQuery; }
        
        @Override
        public String toString() {
            return "QueryAnalysisResult{" +
                    "tables=" + tables +
                    ", fields=" + fields +
                    ", isComplexQuery=" + isComplexQuery +
                    '}';
        }
    }
    
    /**
     * 字段信息
     */
    public static class FieldInfo {
        private String fieldName;
        private String alias;
        private String tableName;
        private boolean isComputed = false;
        private boolean isFunction = false;
        private boolean isAllFields = false;  // 是否为 SELECT * 的情况
        
        // Getters and Setters
        public String getFieldName() { return fieldName; }
        public void setFieldName(String fieldName) { this.fieldName = fieldName; }
        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }
        public String getTableName() { return tableName; }
        public void setTableName(String tableName) { this.tableName = tableName; }
        public boolean isComputed() { return isComputed; }
        public void setComputed(boolean computed) { isComputed = computed; }
        public boolean isFunction() { return isFunction; }
        public void setFunction(boolean function) { isFunction = function; }
        public boolean isAllFields() { return isAllFields; }
        public void setAllFields(boolean allFields) { isAllFields = allFields; }
        
        @Override
        public String toString() {
            return "FieldInfo{" +
                    "fieldName='" + fieldName + '\'' +
                    ", alias='" + alias + '\'' +
                    ", tableName='" + tableName + '\'' +
                    ", isComputed=" + isComputed +
                    ", isFunction=" + isFunction +
                    ", isAllFields=" + isAllFields +
                    '}';
        }
    }
}