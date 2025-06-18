package com.dbsync.dbsync.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class BeanChecker {

    @Autowired
    private ApplicationContext context;

    @PostConstruct
    public void checkBeans() {
        System.out.println("authDataSource exists? " + context.containsBean("authDataSource"));
        System.out.println("authSqlSessionFactory exists? " + context.containsBean("authSqlSessionFactory"));
    }
}