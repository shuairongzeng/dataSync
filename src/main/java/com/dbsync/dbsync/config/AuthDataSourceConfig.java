package com.dbsync.dbsync.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * 认证数据源配置（SQLite）
 */
@Configuration
public class AuthDataSourceConfig {

    @Bean(name = "authDataSource")
    public DataSource authDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:sqlite:auth.db")
                .driverClassName("org.sqlite.JDBC")
                .build();
    }

    @Bean(name = "authSqlSessionFactory")
    public SqlSessionFactory authSqlSessionFactory() throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(authDataSource());

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);

        // 手动添加Mapper
        configuration.addMapper(com.dbsync.dbsync.mapper.auth.UserMapper.class);

        bean.setConfiguration(configuration);
        return bean.getObject();
    }

    @Bean(name = "authSqlSessionTemplate")
    public SqlSessionTemplate authSqlSessionTemplate() throws Exception {
        return new SqlSessionTemplate(authSqlSessionFactory());
    }

    @Bean(name = "userMapper")
    public com.dbsync.dbsync.mapper.auth.UserMapper userMapper() throws Exception {
        return authSqlSessionTemplate().getMapper(com.dbsync.dbsync.mapper.auth.UserMapper.class);
    }

    @Bean
    public DataSourceInitializer authDataSourceInitializer() {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(authDataSource());

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema.sql"));
        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}