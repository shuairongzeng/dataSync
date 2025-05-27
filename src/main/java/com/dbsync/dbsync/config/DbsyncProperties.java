package com.dbsync.dbsync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "sync")
public class DbsyncProperties {

    private List<SyncTaskProperties> tasks = new ArrayList<>();

    public List<SyncTaskProperties> getTasks() {
        return tasks;
    }

    public void setTasks(List<SyncTaskProperties> tasks) {
        this.tasks = tasks;
    }
}
