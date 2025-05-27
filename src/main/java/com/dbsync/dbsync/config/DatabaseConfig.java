package com.dbsync.dbsync.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbsync.dbsync.mapper.TableMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "db")
public class DatabaseConfig {

    private final Map<String, DataSourceProperties> connections = new HashMap<>();

    public Map<String, DataSourceProperties> getConnections() {
        return connections;
    }

    public SqlSessionFactory getSessionFactory(String connectionId) {
        DataSource dataSource = getDataSource(connectionId);
        if (dataSource == null) {
            throw new IllegalArgumentException("No configuration found for connection ID: " + connectionId);
        }
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration(
                new Environment("Development", new JdbcTransactionFactory(), dataSource)
        );
        configuration.addMapper(TableMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private DataSource getDataSource(String connectionId) {
        DataSourceProperties props = connections.get(connectionId);
        if (props == null) {
            return null; // Or throw an exception
        }

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName(props.getDriverClassName());
        dataSource.setUrl(props.getUrl());
        dataSource.setUsername(props.getUsername());
        dataSource.setPassword(props.getPassword());
        // 连接池配置
        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(20);
        return dataSource;
    }
}