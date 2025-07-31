package com.dbsync.dbsync.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 同步任务日志实体类
 */
@TableName("sync_task_logs")
public class SyncTaskLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long taskId;
    
    private String level;
    
    private String message;
    
    private String createdAt;
    
    // 构造函数
    public SyncTaskLog() {
        this.level = "INFO";
        this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    public SyncTaskLog(Long taskId, String level, String message) {
        this();
        this.taskId = taskId;
        this.level = level;
        this.message = message;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getTaskId() {
        return taskId;
    }
    
    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
    
    public String getLevel() {
        return level;
    }
    
    public void setLevel(String level) {
        this.level = level;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "SyncTaskLog{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", level='" + level + '\'' +
                ", message='" + message + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}