package com.dbsync.dbsync;

import com.dbsync.dbsync.config.DatabaseConfig;
import com.dbsync.dbsync.service.DatabaseSyncService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.ibatis.session.SqlSessionFactory;

//@SpringBootApplication
public class DbsyncApplication {
    public static void main(String[] args) {
        /**
         * Oracle数据库配置 portal库
         */
        SqlSessionFactory sourceFactory = DatabaseConfig.getOracleSessionFactory(
                "jdbc:oracle:thin:@192.168.107.101:1525/orcl",
                "PT1_ECI_CQDM",
                "ecidh.com2024"
        );

/**
 * oracle数据库配置 shard库
 */
//        SqlSessionFactory sourceFactory = DatabaseConfig.getOracleSessionFactory(
//                "jdbc:oracle:thin:@192.168.107.101:1523/orcl",
//                "CQ1_CQSW_SHARE_QUERY",
//                "CQSW_SHARE_QUERY_2wsx"
//        );

        /**
         * PostgreSQL数据库配置
         */
        SqlSessionFactory targetFactory = DatabaseConfig.getPgSessionFactory(
                "jdbc:postgresql://192.168.106.103:5432/pt1_eci_cqdm",
                "cqdm_basic",
                "cqdm_basic_1qaz"
        );

        // 创建同步服务实例，设置是否在同步前清空目标表
        DatabaseSyncService syncService = new DatabaseSyncService(sourceFactory, targetFactory, true);

        try {
            syncService.syncDatabase();
            System.out.println("数据同步完成");
        } catch (Exception e) {
            System.err.println("数据同步失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

//    public static void main(String[] args) {
//        SpringApplication.run(DbsyncApplication.class, args);
//    }

}
