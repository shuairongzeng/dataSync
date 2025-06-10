package com.dbsync.dbsync;

import com.dbsync.dbsync.config.DatabaseConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@MapperScan("com.dbsync.dbsync.mapper")
@Import(DatabaseConfig.class)
public class DbsyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbsyncApplication.class, args);
    }

}
