package com.dbsync.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

/**
 * Cache database initializer
 */
@Component
public class CacheInitializer implements CommandLineRunner {

    @Autowired
    @Qualifier("cacheDataSource")
    private DataSource cacheDataSource;

    @Override
    public void run(String... args) throws Exception {
        initializeCacheDatabase();
    }


    private void initializeCacheDatabase() {
        try {
            ClassPathResource resource = new ClassPathResource("cache-schema.sql");

            // For debugging: Read and print the SQL content
            String sqlContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            System.out.println("Executing SQL script:");
            System.out.println(sqlContent);

            // Use ResourceDatabasePopulator for more control over script execution
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(resource);

            // This is the key change: Set the separator for statements that end with END;
            // This tells ScriptUtils not to split the trigger on the inner semicolon.
            populator.setSeparator("END;");

            try (Connection connection = cacheDataSource.getConnection()) {
                populator.execute(cacheDataSource);
            }

            System.out.println("Cache database initialized successfully");

        } catch (Exception e) {
            System.err.println("Failed to initialize cache database: " + e.getMessage());
            e.printStackTrace();
        }
    }

}