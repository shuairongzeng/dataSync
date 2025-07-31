package com.dbsync.dbsync.entity;

/**
 * 数据库表列信息实体类
 */
public class ColumnInfo {
    
    /**
     * 列名
     */
    private String columnName;
    
    /**
     * 数据类型
     */
    private String dataType;
    
    /**
     * 列大小
     */
    private Integer columnSize;
    
    /**
     * 小数位数
     */
    private Integer decimalDigits;
    
    /**
     * 是否可为空
     */
    private Boolean nullable;
    
    /**
     * 默认值
     */
    private String defaultValue;
    
    /**
     * 列注释
     */
    private String remarks;
    
    /**
     * 是否为主键
     */
    private Boolean isPrimaryKey;
    
    /**
     * 是否为自增列
     */
    private Boolean isAutoIncrement;
    
    /**
     * 列位置
     */
    private Integer ordinalPosition;
    
    /**
     * JDBC类型代码
     */
    private Integer jdbcType;
    
    /**
     * 类型名称
     */
    private String typeName;

    // 构造函数
    public ColumnInfo() {}

    public ColumnInfo(String columnName, String dataType) {
        this.columnName = columnName;
        this.dataType = dataType;
    }

    // Getter和Setter方法
    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public Integer getColumnSize() {
        return columnSize;
    }

    public void setColumnSize(Integer columnSize) {
        this.columnSize = columnSize;
    }

    public Integer getDecimalDigits() {
        return decimalDigits;
    }

    public void setDecimalDigits(Integer decimalDigits) {
        this.decimalDigits = decimalDigits;
    }

    public Boolean getNullable() {
        return nullable;
    }

    public void setNullable(Boolean nullable) {
        this.nullable = nullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Boolean getIsPrimaryKey() {
        return isPrimaryKey;
    }

    public void setIsPrimaryKey(Boolean isPrimaryKey) {
        this.isPrimaryKey = isPrimaryKey;
    }

    public Boolean getIsAutoIncrement() {
        return isAutoIncrement;
    }

    public void setIsAutoIncrement(Boolean isAutoIncrement) {
        this.isAutoIncrement = isAutoIncrement;
    }

    public Integer getOrdinalPosition() {
        return ordinalPosition;
    }

    public void setOrdinalPosition(Integer ordinalPosition) {
        this.ordinalPosition = ordinalPosition;
    }

    public Integer getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(Integer jdbcType) {
        this.jdbcType = jdbcType;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public String toString() {
        return "ColumnInfo{" +
                "columnName='" + columnName + '\'' +
                ", dataType='" + dataType + '\'' +
                ", columnSize=" + columnSize +
                ", decimalDigits=" + decimalDigits +
                ", nullable=" + nullable +
                ", defaultValue='" + defaultValue + '\'' +
                ", remarks='" + remarks + '\'' +
                ", isPrimaryKey=" + isPrimaryKey +
                ", isAutoIncrement=" + isAutoIncrement +
                ", ordinalPosition=" + ordinalPosition +
                ", jdbcType=" + jdbcType +
                ", typeName='" + typeName + '\'' +
                '}';
    }
}
