package com.dbsync.dbsync.entity;

/**
 * 数据库表信息实体类
 */
public class TableInfo {
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 表类型（TABLE, VIEW等）
     */
    private String tableType;
    
    /**
     * 表注释/备注
     */
    private String remarks;
    
    /**
     * 数据库schema名称
     */
    private String schemaName;
    
    /**
     * 数据库目录名称
     */
    private String catalogName;
    
    /**
     * 表的行数（估算值，可能为null）
     */
    private Long rowCount;
    
    /**
     * 表的列数
     */
    private Integer columnCount;
    
    /**
     * 是否有主键
     */
    private Boolean hasPrimaryKey;
    
    /**
     * 主键列名（如果只有一个主键）
     */
    private String primaryKeyColumn;
    
    // 构造函数
    public TableInfo() {}
    
    public TableInfo(String tableName) {
        this.tableName = tableName;
        this.tableType = "TABLE";
    }
    
    public TableInfo(String tableName, String tableType, String remarks) {
        this.tableName = tableName;
        this.tableType = tableType;
        this.remarks = remarks;
    }
    
    // Getters and Setters
    public String getTableName() {
        return tableName;
    }
    
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }
    
    public String getTableType() {
        return tableType;
    }
    
    public void setTableType(String tableType) {
        this.tableType = tableType;
    }
    
    public String getRemarks() {
        return remarks;
    }
    
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
    
    public String getSchemaName() {
        return schemaName;
    }
    
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }
    
    public String getCatalogName() {
        return catalogName;
    }
    
    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }
    
    public Long getRowCount() {
        return rowCount;
    }
    
    public void setRowCount(Long rowCount) {
        this.rowCount = rowCount;
    }
    
    public Integer getColumnCount() {
        return columnCount;
    }
    
    public void setColumnCount(Integer columnCount) {
        this.columnCount = columnCount;
    }
    
    public Boolean getHasPrimaryKey() {
        return hasPrimaryKey;
    }
    
    public void setHasPrimaryKey(Boolean hasPrimaryKey) {
        this.hasPrimaryKey = hasPrimaryKey;
    }
    
    public String getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }
    
    public void setPrimaryKeyColumn(String primaryKeyColumn) {
        this.primaryKeyColumn = primaryKeyColumn;
    }
    
    @Override
    public String toString() {
        return "TableInfo{" +
                "tableName='" + tableName + '\'' +
                ", tableType='" + tableType + '\'' +
                ", remarks='" + remarks + '\'' +
                ", schemaName='" + schemaName + '\'' +
                ", catalogName='" + catalogName + '\'' +
                ", rowCount=" + rowCount +
                ", columnCount=" + columnCount +
                ", hasPrimaryKey=" + hasPrimaryKey +
                ", primaryKeyColumn='" + primaryKeyColumn + '\'' +
                '}';
    }
}
