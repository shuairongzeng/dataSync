package com.dbsync.dbsync.entity;

/**
 * 基础表信息实体 - 用于快速加载表列表
 * 只包含最基本的信息，避免耗时的详细查询
 */
public class BasicTableInfo {
    
    private String tableName;
    private String tableType;
    private String remarks;
    private String schemaName;
    private String catalogName;

    public BasicTableInfo() {}

    public BasicTableInfo(String tableName, String tableType, String remarks, String schemaName, String catalogName) {
        this.tableName = tableName;
        this.tableType = tableType;
        this.remarks = remarks;
        this.schemaName = schemaName;
        this.catalogName = catalogName;
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

    @Override
    public String toString() {
        return "BasicTableInfo{" +
                "tableName='" + tableName + '\'' +
                ", tableType='" + tableType + '\'' +
                ", remarks='" + remarks + '\'' +
                ", schemaName='" + schemaName + '\'' +
                ", catalogName='" + catalogName + '\'' +
                '}';
    }
}