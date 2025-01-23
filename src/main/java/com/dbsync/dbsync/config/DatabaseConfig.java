package com.dbsync.dbsync.config;


import com.alibaba.druid.pool.DruidDataSource;
import com.dbsync.dbsync.mapper.TableMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.util.Properties;

public class DatabaseConfig {

    public static SqlSessionFactory getOracleSessionFactory(String url, String username, String password) {
        DataSource dataSource = getOracleDataSource(url, username, password);
        Environment environment = new Environment("Development", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(TableMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    public static SqlSessionFactory getPgSessionFactory(String url, String username, String password) {
        DataSource dataSource = getPgDataSource(url, username, password);
        Environment environment = new Environment("Development", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(TableMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private static DataSource getOracleDataSource(String url, String username, String password) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        // 连接池配置
        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(20);
        return dataSource;
    }

    private static DataSource getPgDataSource(String url, String username, String password) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        // 连接池配置
        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(20);
        return dataSource;
    }
}