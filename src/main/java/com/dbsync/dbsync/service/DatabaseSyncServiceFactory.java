package com.dbsync.dbsync.service;

import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.progress.ProgressManager;
import com.dbsync.dbsync.typemapping.TypeMappingRegistry;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据库同步服务工厂类
 * 用于动态创建DatabaseSyncService实例
 */
@Component
public class DatabaseSyncServiceFactory {

    @Autowired
    private TypeMappingRegistry typeMappingRegistry;

    @Autowired
    private ProgressManager progressManager;

    /**
     * 创建DatabaseSyncService实例
     */
    public DatabaseSyncService createSyncService(SqlSessionFactory sourceFactory, 
                                                 SqlSessionFactory targetFactory,
                                                 DbConnection sourceConnection,
                                                 DbConnection targetConnection) {
        
        // 构建同步参数
        boolean truncateBeforeSync = true; // 可以从任务配置中获取
        String sourceDbType = sourceConnection.getDbType();
        String targetDbType = targetConnection.getDbType();
        String targetSchemaName = targetConnection.getSchema() != null ? 
            targetConnection.getSchema() : getDefaultSchema(targetDbType);

        return new DatabaseSyncService(
            sourceFactory,
            targetFactory,
            truncateBeforeSync,
            typeMappingRegistry,
            sourceDbType,
            targetDbType,
            targetSchemaName,
            progressManager
        );
    }

    /**
     * 获取默认的schema名称
     */
    private String getDefaultSchema(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
                return "";
            case "postgresql":
            case "vastbase":
                return "public";
            case "oracle":
            case "dameng":
                return "";
            case "sqlserver":
                return "dbo";
            default:
                return "";
        }
    }
}