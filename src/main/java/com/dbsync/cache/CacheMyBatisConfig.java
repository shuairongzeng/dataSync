package com.dbsync.cache;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * MyBatis configuration for cache datasource
 */
@Configuration
@MapperScan(basePackages = "com.dbsync.cache", sqlSessionFactoryRef = "cacheSqlSessionFactory")
public class CacheMyBatisConfig {

    /**
     * Cache SqlSessionFactory
     */
    @Bean(name = "cacheSqlSessionFactory")
    public SqlSessionFactory cacheSqlSessionFactory(@Qualifier("cacheDataSource") DataSource cacheDataSource) throws Exception {
        SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
        sessionFactory.setDataSource(cacheDataSource);
        
        // Configure MyBatis settings for SQLite
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(org.apache.ibatis.logging.slf4j.Slf4jImpl.class);
        configuration.setJdbcTypeForNull(org.apache.ibatis.type.JdbcType.NULL);
        
        sessionFactory.setConfiguration(configuration);
        
        return sessionFactory.getObject();
    }

    /**
     * Cache SqlSessionTemplate
     */
    @Bean(name = "cacheSqlSessionTemplate")
    public SqlSessionTemplate cacheSqlSessionTemplate(@Qualifier("cacheSqlSessionFactory") SqlSessionFactory cacheSqlSessionFactory) {
        return new SqlSessionTemplate(cacheSqlSessionFactory);
    }
}