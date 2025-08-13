package com.dbsync.dbsync.entity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强的查询结果类
 * 扩展QueryResult，支持中英文字段名对照显示
 */
public class EnhancedQueryResult extends QueryResult {
    
    private Map<String, String> columnDisplayNames;    // 列显示名映射: 英文->中文
    private List<ColumnMetadata> columnsMetadata;      // 详细的列元数据
    private boolean enableChineseColumnNames = true;   // 是否启用中文列名功能
    
    public EnhancedQueryResult() {
        super();
        this.columnDisplayNames = new HashMap<>();
        this.columnsMetadata = new ArrayList<>();
    }
    
    public EnhancedQueryResult(QueryResult original) {
        super(original.getColumns(), original.getRows(), original.getTotalRows(), original.getExecutionTime());
        this.setMessage(original.getMessage());
        this.setCurrentPage(original.getCurrentPage());
        this.setPageSize(original.getPageSize());
        this.setTotalPages(original.getTotalPages());
        this.setHasMore(original.getHasMore());
        this.setFromCache(original.getFromCache());
        
        this.columnDisplayNames = new HashMap<>();
        this.columnsMetadata = new ArrayList<>();
        
        // 初始化列元数据
        if (original.getColumns() != null) {
            for (String column : original.getColumns()) {
                ColumnMetadata metadata = new ColumnMetadata();
                metadata.setOriginalName(column);
                metadata.setDisplayName(column);
                metadata.setHasChineseName(false);
                this.columnsMetadata.add(metadata);
            }
        }
    }
    
    /**
     * 设置字段的中文显示名称
     */
    public void setFieldDisplayName(String originalName, String displayName, String chineseName) {
        if (originalName == null || displayName == null) {
            return;
        }
        
        this.columnDisplayNames.put(originalName, displayName);
        
        // 更新列元数据
        for (ColumnMetadata metadata : this.columnsMetadata) {
            if (originalName.equals(metadata.getOriginalName())) {
                metadata.setDisplayName(displayName);
                metadata.setChineseName(chineseName);
                metadata.setHasChineseName(chineseName != null && !chineseName.trim().isEmpty());
                break;
            }
        }
    }
    
    /**
     * 批量设置字段显示名称
     */
    public void setFieldDisplayNames(Map<String, String> displayNameMappings) {
        if (displayNameMappings == null || displayNameMappings.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, String> entry : displayNameMappings.entrySet()) {
            setFieldDisplayName(entry.getKey(), entry.getValue(), entry.getValue());
        }
    }
    
    /**
     * 获取用于前端显示的列信息
     */
    public List<String> getDisplayColumns() {
        if (getColumns() == null) {
            return new ArrayList<>();
        }
        
        if (!enableChineseColumnNames) {
            return new ArrayList<>(getColumns());
        }
        
        return getColumns().stream()
            .map(column -> columnDisplayNames.getOrDefault(column, column))
            .collect(Collectors.toList());
    }
    
    /**
     * 获取列显示映射（用于前端数据转换）
     */
    public Map<String, String> getColumnDisplayMapping() {
        if (!enableChineseColumnNames) {
            return new HashMap<>();
        }
        return new HashMap<>(this.columnDisplayNames);
    }
    
    /**
     * 获取原始列名到显示名的反向映射
     */
    public Map<String, String> getReverseColumnMapping() {
        Map<String, String> reverseMapping = new HashMap<>();
        for (Map.Entry<String, String> entry : columnDisplayNames.entrySet()) {
            reverseMapping.put(entry.getValue(), entry.getKey());
        }
        return reverseMapping;
    }
    
    /**
     * 检查指定列是否有中文显示名
     */
    public boolean hasChineseColumnName(String originalColumnName) {
        if (!enableChineseColumnNames) {
            return false;
        }
        
        String displayName = columnDisplayNames.get(originalColumnName);
        return displayName != null && !displayName.equals(originalColumnName);
    }
    
    /**
     * 获取指定列的显示名称
     */
    public String getColumnDisplayName(String originalColumnName) {
        if (!enableChineseColumnNames) {
            return originalColumnName;
        }
        return columnDisplayNames.getOrDefault(originalColumnName, originalColumnName);
    }
    
    /**
     * 获取有中文名的列数量
     */
    public int getChineseColumnCount() {
        if (!enableChineseColumnNames) {
            return 0;
        }
        
        return (int) columnsMetadata.stream()
            .filter(ColumnMetadata::isHasChineseName)
            .count();
    }
    
    /**
     * 获取中文列名覆盖率
     */
    public double getChineseColumnCoverage() {
        if (columnsMetadata.isEmpty() || !enableChineseColumnNames) {
            return 0.0;
        }
        
        return (double) getChineseColumnCount() / columnsMetadata.size();
    }
    
    /**
     * 转换为前端友好的格式
     */
    public Map<String, Object> toFrontendFormat() {
        Map<String, Object> result = new HashMap<>();
        
        // 基础信息
        result.put("columns", getColumns());
        result.put("displayColumns", getDisplayColumns());
        result.put("columnDisplayMapping", getColumnDisplayMapping());
        result.put("rows", getRows());
        result.put("totalRows", getTotalRows());
        result.put("executionTime", getExecutionTime());
        result.put("message", getMessage());
        
        // 分页信息
        result.put("currentPage", getCurrentPage());
        result.put("pageSize", getPageSize());
        result.put("totalPages", getTotalPages());
        result.put("hasMore", getHasMore());
        result.put("fromCache", getFromCache());
        
        // 中文列名相关信息
        result.put("enableChineseColumnNames", enableChineseColumnNames);
        result.put("chineseColumnCount", getChineseColumnCount());
        result.put("chineseColumnCoverage", getChineseColumnCoverage());
        result.put("columnsMetadata", columnsMetadata);
        
        // 如果有数据，转换为对象格式（兼容前端表格组件）
        if (getRows() != null && getColumns() != null) {
            List<Map<String, Object>> dataObjects = new ArrayList<>();
            
            for (List<Object> row : getRows()) {
                Map<String, Object> rowObj = new HashMap<>();
                for (int i = 0; i < getColumns().size() && i < row.size(); i++) {
                    rowObj.put(getColumns().get(i), row.get(i));
                }
                dataObjects.add(rowObj);
            }
            
            result.put("data", dataObjects);
        }
        
        return result;
    }
    
    /**
     * 列元数据
     */
    public static class ColumnMetadata {
        private String originalName;      // 原始英文名
        private String displayName;       // 显示名称(中文优先)
        private String chineseName;       // 中文备注
        private String tableName;         // 所属表名
        private String dataType;          // 数据类型
        private boolean hasChineseName;   // 是否有中文名
        private int columnIndex;          // 列索引
        
        public ColumnMetadata() {
        }
        
        public ColumnMetadata(String originalName, String displayName) {
            this.originalName = originalName;
            this.displayName = displayName;
            this.hasChineseName = !originalName.equals(displayName);
        }
        
        // Getters and Setters
        public String getOriginalName() { 
            return originalName; 
        }
        
        public void setOriginalName(String originalName) { 
            this.originalName = originalName; 
        }
        
        public String getDisplayName() { 
            return displayName; 
        }
        
        public void setDisplayName(String displayName) { 
            this.displayName = displayName; 
        }
        
        public String getChineseName() { 
            return chineseName; 
        }
        
        public void setChineseName(String chineseName) { 
            this.chineseName = chineseName; 
        }
        
        public String getTableName() { 
            return tableName; 
        }
        
        public void setTableName(String tableName) { 
            this.tableName = tableName; 
        }
        
        public String getDataType() { 
            return dataType; 
        }
        
        public void setDataType(String dataType) { 
            this.dataType = dataType; 
        }
        
        public boolean isHasChineseName() { 
            return hasChineseName; 
        }
        
        public void setHasChineseName(boolean hasChineseName) { 
            this.hasChineseName = hasChineseName; 
        }
        
        public int getColumnIndex() { 
            return columnIndex; 
        }
        
        public void setColumnIndex(int columnIndex) { 
            this.columnIndex = columnIndex; 
        }
        
        @Override
        public String toString() {
            return "ColumnMetadata{" +
                    "originalName='" + originalName + '\'' +
                    ", displayName='" + displayName + '\'' +
                    ", chineseName='" + chineseName + '\'' +
                    ", tableName='" + tableName + '\'' +
                    ", dataType='" + dataType + '\'' +
                    ", hasChineseName=" + hasChineseName +
                    ", columnIndex=" + columnIndex +
                    '}';
        }
    }
    
    // Getters and Setters for new fields
    public Map<String, String> getColumnDisplayNames() { 
        return columnDisplayNames; 
    }
    
    public void setColumnDisplayNames(Map<String, String> columnDisplayNames) { 
        this.columnDisplayNames = columnDisplayNames; 
    }
    
    public List<ColumnMetadata> getColumnsMetadata() { 
        return columnsMetadata; 
    }
    
    public void setColumnsMetadata(List<ColumnMetadata> columnsMetadata) { 
        this.columnsMetadata = columnsMetadata; 
    }
    
    public boolean isEnableChineseColumnNames() { 
        return enableChineseColumnNames; 
    }
    
    public void setEnableChineseColumnNames(boolean enableChineseColumnNames) { 
        this.enableChineseColumnNames = enableChineseColumnNames; 
    }
    
    @Override
    public String toString() {
        return "EnhancedQueryResult{" +
                "columns=" + getColumns() +
                ", displayColumns=" + getDisplayColumns() +
                ", totalRows=" + getTotalRows() +
                ", executionTime=" + getExecutionTime() +
                ", chineseColumnCount=" + getChineseColumnCount() +
                ", chineseColumnCoverage=" + getChineseColumnCoverage() +
                ", enableChineseColumnNames=" + enableChineseColumnNames +
                '}';
    }
}